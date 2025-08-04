package com.muditsahni.controller.v1

import com.muditsahni.error.AlreadyExistsException
import com.muditsahni.error.ErrorResponse
import com.muditsahni.error.InvalidRequestException
import com.muditsahni.error.RecordNotFoundException
import com.muditsahni.error.TenantAlreadyExistsException
import com.muditsahni.error.UserAlreadyExistsException
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.FileNotFoundException
import java.time.Instant

@Component
@Order(-2) // To ensure it takes precedence over the default handler
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer

): AbstractErrorWebExceptionHandler(
    errorAttributes,
    webProperties.resources,
    applicationContext,

) {

    init {
        this.setMessageReaders(serverCodecConfigurer.readers)
        this.setMessageWriters(serverCodecConfigurer.writers)
    }


    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
    }

    private fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val error = getError(request)
        val status = when (error) {
            is RecordNotFoundException -> HttpStatus.NOT_FOUND
            is FileNotFoundException -> HttpStatus.BAD_REQUEST
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            is InvalidRequestException -> HttpStatus.BAD_REQUEST
            is AlreadyExistsException -> HttpStatus.CONFLICT
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = error.message ?: "An error occurred",
            path = request.path()
        )

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorResponse))
    }

}
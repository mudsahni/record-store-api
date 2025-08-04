package com.muditsahni

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableAsync

@OpenAPIDefinition(
    security = [ io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth") ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

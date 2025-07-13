package com.muditsahni.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.UuidRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
open class MongoConfig {
    @Bean
    open fun mongoClient(
        @Value("\${spring.data.mongodb.uri}") connectionString: String
    ): MongoClient {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(30, TimeUnit.SECONDS)
            }
            .applyToSocketSettings { builder ->
                builder.connectTimeout(30, TimeUnit.SECONDS)
            }
            .build()

        return MongoClients.create(settings)
    }
}


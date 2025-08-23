package com.muditsahni.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.muditsahni.service.TenantAwareMongoService
import com.muditsahni.repository.TenantAwareUserRepository
import org.bson.UuidRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.util.concurrent.TimeUnit

@Configuration
@EnableReactiveMongoRepositories(
    basePackages = ["com.muditsahni.repository"]
)class MongoConfig {

    @Bean
    @Primary
    fun mongoClient(
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

    @Bean("reactiveMongoTemplate")  // This is the required bean name
    @Primary
    fun reactiveMongoTemplate(mongoClient: MongoClient): ReactiveMongoTemplate {
        return ReactiveMongoTemplate(mongoClient, "global-db")
    }

    @Bean("globalMongoTemplate")  // Keep this as alias if needed elsewhere
    fun globalMongoTemplate(reactiveMongoTemplate: ReactiveMongoTemplate): ReactiveMongoTemplate {
        return reactiveMongoTemplate
    }

    @Bean
    fun tenantAwareMongoService(mongoClient: MongoClient): TenantAwareMongoService {
        return TenantAwareMongoService(mongoClient)
    }

    @Bean
    fun tenantAwareUserRepository(tenantAwareMongoService: TenantAwareMongoService): TenantAwareUserRepository {
        return TenantAwareUserRepository(tenantAwareMongoService)
    }
}


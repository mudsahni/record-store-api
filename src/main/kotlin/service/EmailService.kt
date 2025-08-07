package com.muditsahni.service

import com.azure.communication.email.EmailClient
import com.azure.communication.email.EmailClientBuilder
import com.azure.communication.email.models.EmailAddress
import com.azure.communication.email.models.EmailMessage
import com.muditsahni.model.entity.User
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EmailService(
    @Value("\${azure.communication.email.connection-string}")
    private val connectionString: String,

    @Value("\${azure.communication.email.sender-address}")
    private val senderAddress: String,

    @Value("\${app.base-url}")
    private val baseUrl: String
) {

    private val logger = KotlinLogging.logger {}

    private val emailClient: EmailClient by lazy {
        EmailClientBuilder()
            .connectionString(connectionString)
            .buildClient()
    }

    suspend fun sendVerificationEmail(user: User, verificationToken: String): Boolean {
        return try {
            val verificationUrl = "$baseUrl/api/v1/auth/verify?token=$verificationToken"

            val emailMessage = EmailMessage()
            emailMessage.senderAddress = senderAddress
            emailMessage.toRecipients = listOf(EmailAddress(user.email))
            emailMessage.subject = "Verify your email address"
            emailMessage.bodyHtml = buildVerificationEmailHtml(user.firstName, verificationUrl)
            emailMessage.bodyPlainText = buildVerificationEmailText(user.firstName, verificationUrl)

            val response = emailClient.beginSend(
                emailMessage
            )
            response.getFinalResult() // Wait for completion

            true
        } catch (e: Exception) {
            logger.error("Failed to send verification email: ${e.message}")
            false
        }
    }

    private fun buildVerificationEmailHtml(firstName: String, verificationUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Email</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px;">
                    <div style="text-align: center; padding: 20px 0; border-bottom: 1px solid #eee;">
                        <h1 style="color: #333; margin: 0;">Welcome to Our Platform!</h1>
                    </div>
                    
                    <div style="padding: 30px 0;">
                        <h2 style="color: #333;">Hi $firstName,</h2>
                        <p style="color: #666; line-height: 1.6; font-size: 16px;">
                            Thank you for registering! Please verify your email address to complete your account setup.
                        </p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="$verificationUrl" 
                               style="background-color: #007bff; color: #ffffff; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">
                                Verify Email Address
                            </a>
                        </div>
                        
                        <p style="color: #666; line-height: 1.6; font-size: 14px;">
                            If the button doesn't work, copy and paste this link into your browser:<br>
                            <a href="$verificationUrl" style="color: #007bff; word-break: break-all;">$verificationUrl</a>
                        </p>
                        
                        <p style="color: #666; line-height: 1.6; font-size: 14px;">
                            <strong>This link expires in 24 hours.</strong>
                        </p>
                        
                        <p style="color: #666; line-height: 1.6; font-size: 14px;">
                            If you didn't create an account, please ignore this email.
                        </p>
                    </div>
                    
                    <div style="border-top: 1px solid #eee; padding-top: 20px; text-align: center;">
                        <p style="color: #999; font-size: 12px;">
                            © ${java.time.Year.now()} Your Company Name. All rights reserved.
                        </p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildVerificationEmailText(firstName: String, verificationUrl: String): String {
        return """
            Hi $firstName,
            
            Thank you for registering! Please verify your email address to complete your account setup.
            
            Click this link to verify your email:
            $verificationUrl
            
            This link expires in 24 hours.
            
            If you didn't create an account, please ignore this email.
            
            Thanks,
            Your Company Team
        """.trimIndent()
    }
}
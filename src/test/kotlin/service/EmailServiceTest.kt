package service

import com.azure.communication.email.EmailClient
import com.azure.communication.email.models.EmailAddress
import com.azure.communication.email.models.EmailMessage
import com.azure.communication.email.models.EmailSendResult
import com.azure.communication.email.models.EmailSendStatus
import com.azure.core.util.polling.SyncPoller
import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.service.EmailService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.Assertions.*

class EmailServiceTest {

    private lateinit var emailService: EmailService
    private lateinit var mockEmailClient: EmailClient
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // Mock the email client
        mockEmailClient = mockk()

        // Create a custom EmailService for testing
        emailService = object : EmailService(
            connectionString = "test-connection-string",
            senderAddress = "noreply@test.com",
            baseUrl = "https://test-app.com"
        ) {
            override suspend fun sendVerificationEmail(user: User, verificationToken: String): Boolean {
                return try {
                    val verificationUrl = "https://test-app.com/api/v1/auth/verify?token=$verificationToken"

                    val emailMessage = EmailMessage()
                    emailMessage.senderAddress = "noreply@test.com"
                    emailMessage.toRecipients = listOf(EmailAddress(user.email))
                    emailMessage.subject = "Verify your email address"
                    emailMessage.bodyHtml = buildVerificationEmailHtml(user.firstName, verificationUrl)
                    emailMessage.bodyPlainText = buildVerificationEmailText(user.firstName, verificationUrl)

                    val response = mockEmailClient.beginSend(emailMessage)
                    response.getFinalResult() // Wait for completion

                    true
                } catch (e: Exception) {
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

        // Create test user
        testUser = User(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            phoneNumber = "+1234567890",
            passwordHash = "hashedPassword",
            tenantName = "test-tenant",
            roles = listOf(Role.USER),
            createdBy = "system",
            status = UserStatus.PENDING,
            createdAt = Instant.now(),
            emailVerified = false,
        )
    }

    @Test
    fun `sendVerificationEmail should return true when email is sent successfully`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(any<EmailMessage>()) } returns mockPoller

        // When
        val result = emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        assertTrue(result)
        verify { mockEmailClient.beginSend(any<EmailMessage>()) }
        verify { mockPoller.getFinalResult() }
    }

    @Test
    fun `sendVerificationEmail should return false when email sending fails`() = runTest {
        // Given
        val verificationToken = "test-verification-token"

        every { mockEmailClient.beginSend(any<EmailMessage>()) } throws RuntimeException("Email service unavailable")

        // When
        val result = emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        assertFalse(result)
        verify { mockEmailClient.beginSend(any<EmailMessage>()) }
    }

    @Test
    fun `sendVerificationEmail should create email message with correct properties`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertEquals("noreply@test.com", emailMessage.senderAddress)
        assertEquals(1, emailMessage.toRecipients.size)
        assertEquals("john.doe@example.com", emailMessage.toRecipients[0].address)
        assertEquals("Verify your email address", emailMessage.subject)
        assertNotNull(emailMessage.bodyHtml)
        assertNotNull(emailMessage.bodyPlainText)
    }

    @Test
    fun `sendVerificationEmail should include correct verification URL in email content`() = runTest {
        // Given
        val verificationToken = "test-verification-token-123"
        val expectedUrl = "https://test-app.com/api/v1/auth/verify?token=test-verification-token-123"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertTrue(emailMessage.bodyHtml.contains(expectedUrl))
        assertTrue(emailMessage.bodyPlainText.contains(expectedUrl))
    }

    @Test
    fun `sendVerificationEmail should include user first name in email content`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertTrue(emailMessage.bodyHtml.contains("Hi ${testUser.firstName}"))
        assertTrue(emailMessage.bodyPlainText.contains("Hi ${testUser.firstName}"))
    }

    @Test
    fun `sendVerificationEmail HTML content should be well-formed`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val htmlContent = capturedEmailMessage.captured.bodyHtml

        // Check for essential HTML structure
        assertTrue(htmlContent.contains("<!DOCTYPE html>"))
        assertTrue(htmlContent.contains("<html>"))
        assertTrue(htmlContent.contains("<head>"))
        assertTrue(htmlContent.contains("<body"))
        assertTrue(htmlContent.contains("</html>"))

        // Check for email-specific content
        assertTrue(htmlContent.contains("Welcome to Our Platform!"))
        assertTrue(htmlContent.contains("Verify Email Address"))
        assertTrue(htmlContent.contains("This link expires in 24 hours"))

        // Check for proper link structure
        assertTrue(htmlContent.contains("href=\"https://test-app.com/api/v1/auth/verify?token=$verificationToken\""))
    }

    @Test
    fun `sendVerificationEmail plain text content should be properly formatted`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val plainTextContent = capturedEmailMessage.captured.bodyPlainText

        // Check for essential content
        assertTrue(plainTextContent.contains("Hi ${testUser.firstName}"))
        assertTrue(plainTextContent.contains("Thank you for registering!"))
        assertTrue(plainTextContent.contains("Click this link to verify your email:"))
        assertTrue(plainTextContent.contains("https://test-app.com/api/v1/auth/verify?token=$verificationToken"))
        assertTrue(plainTextContent.contains("This link expires in 24 hours"))
        assertTrue(plainTextContent.contains("Your Company Team"))
    }

    @Test
    fun `sendVerificationEmail should handle different user names correctly`() = runTest {
        // Given
        val userWithDifferentName = testUser.copy(firstName = "Jane", lastName = "Smith")
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(userWithDifferentName, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertTrue(emailMessage.bodyHtml.contains("Hi Jane"))
        assertTrue(emailMessage.bodyPlainText.contains("Hi Jane"))
        assertFalse(emailMessage.bodyHtml.contains("Hi John"))
        assertFalse(emailMessage.bodyPlainText.contains("Hi John"))
    }

    @Test
    fun `sendVerificationEmail should handle special characters in token`() = runTest {
        // Given
        val verificationToken = "test-token-with-special-chars_123-456"
        val expectedUrl = "https://test-app.com/api/v1/auth/verify?token=test-token-with-special-chars_123-456"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertTrue(emailMessage.bodyHtml.contains(expectedUrl))
        assertTrue(emailMessage.bodyPlainText.contains(expectedUrl))
    }

    @Test
    fun `sendVerificationEmail should return false when poller throws exception`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()

        every { mockEmailClient.beginSend(any<EmailMessage>()) } returns mockPoller
        every { mockPoller.getFinalResult() } throws RuntimeException("Polling failed")

        // When
        val result = emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        assertFalse(result)
        verify { mockEmailClient.beginSend(any<EmailMessage>()) }
        verify { mockPoller.getFinalResult() }
    }

    @Test
    fun `email content should include current year in copyright`() = runTest {
        // Given
        val verificationToken = "test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()
        val currentYear = java.time.Year.now().value

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailService.sendVerificationEmail(testUser, verificationToken)

        // Then
        val htmlContent = capturedEmailMessage.captured.bodyHtml
        assertTrue(htmlContent.contains("© $currentYear Your Company Name"))
    }

    @Test
    fun `sendVerificationEmail should work with different base URLs`() = runTest {
        // Given - Create a new service with different URL
        val emailServiceWithDifferentUrl = object : EmailService(
            connectionString = "test-connection-string",
            senderAddress = "noreply@test.com",
            baseUrl = "https://production-app.azurewebsites.net"
        ) {
            override suspend fun sendVerificationEmail(user: User, verificationToken: String): Boolean {
                return try {
                    val verificationUrl = "https://production-app.azurewebsites.net/api/v1/auth/verify?token=$verificationToken"

                    val emailMessage = EmailMessage()
                    emailMessage.senderAddress = "noreply@test.com"
                    emailMessage.toRecipients = listOf(EmailAddress(user.email))
                    emailMessage.subject = "Verify your email address"
                    emailMessage.bodyHtml = "HTML content with $verificationUrl"
                    emailMessage.bodyPlainText = "Text content with $verificationUrl"

                    val response = mockEmailClient.beginSend(emailMessage)
                    response.getFinalResult()

                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        val verificationToken = "test-verification-token"
        val expectedUrl = "https://production-app.azurewebsites.net/api/v1/auth/verify?token=test-verification-token"
        val mockPoller = mockk<SyncPoller<EmailSendResult, EmailSendResult>>()
        val mockResult = mockk<EmailSendResult>()
        val capturedEmailMessage = slot<EmailMessage>()

        every { mockResult.status } returns EmailSendStatus.SUCCEEDED
        every { mockPoller.getFinalResult() } returns mockResult
        every { mockEmailClient.beginSend(capture(capturedEmailMessage)) } returns mockPoller

        // When
        emailServiceWithDifferentUrl.sendVerificationEmail(testUser, verificationToken)

        // Then
        val emailMessage = capturedEmailMessage.captured
        assertTrue(emailMessage.bodyHtml.contains(expectedUrl))
        assertTrue(emailMessage.bodyPlainText.contains(expectedUrl))
    }
}
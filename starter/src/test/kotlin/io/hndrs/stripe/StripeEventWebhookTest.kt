package io.hndrs.stripe

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.EventDataObjectDeserializer
import com.stripe.model.StripeObject
import com.stripe.model.Subscription
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity

@DisplayName("Stripe Event Webhook")
internal class StripeEventWebhookTest {

    private val eventBuilder = mockk<StripeEventBuilder>(relaxed = true)

    companion object {
        private const val TEST_BODY = ""
    }

    private fun testWebHook(stripeEventHandler: StripeEventHandler<*>? = null): StripeEventWebhook {
        return stripeEventHandler?.let {
            StripeEventWebhook(listOf(stripeEventHandler as StripeEventHandler<StripeObject>), "", eventBuilder)
        } ?: StripeEventWebhook(listOf(), "", eventBuilder)
    }

    @BeforeEach
    fun setup() {
        clearMocks(eventBuilder)
    }

    @DisplayName("Missing or Invalid Signature")
    @Test
    fun signatureVerificationFailed() {
        every { eventBuilder.constructEvent(any(), any(), any()) } throws SignatureVerificationException(
            "message",
            "sigheader"
        )

        Assertions.assertEquals(
            ResponseEntity.badRequest().body("Signature Verification failed"),
            testWebHook().stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Any exception during event construction")
    @Test
    fun anyOtherException() {
        every { eventBuilder.constructEvent(any(), any(), any()) } throws IllegalStateException()

        Assertions.assertEquals(
            ResponseEntity.badRequest().body("Event handling failed"),
            testWebHook().stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Any exception during supports check")
    @Test
    fun exceptionDuringSupportsCheck() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        Assertions.assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(ThrowsOnSupport()).stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Any exception during onReceive call")
    @Test
    fun exceptionDuringOnReceiveCall() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        Assertions.assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(ThrowsOnReceive()).stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    private fun mockkEvent(stripeObject: StripeObject): Event {

        val event = mockk<Event>()
        val deserializer = mockk<EventDataObjectDeserializer>()
        every { event.dataObjectDeserializer } returns deserializer
        every<StripeObject?> { deserializer.deserializeUnsafe() } returns stripeObject

        return event
    }

    class ThrowsOnSupport : StripeEventHandler<Subscription>(Subscription::class.java) {

        override fun supports(eventType: String): Boolean {
            throw IllegalStateException()
        }

        override fun onReceive(stripeObject: Subscription) {
            // do nothing
        }
    }

    class ThrowsOnReceive : StripeEventHandler<Subscription>(Subscription::class.java) {

        override fun onReceive(stripeObject: Subscription) {
            throw IllegalStateException()
        }
    }
}

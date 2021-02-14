package io.hndrs.stripe

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.EventData
import com.stripe.model.EventDataObjectDeserializer
import com.stripe.model.Invoice
import com.stripe.model.StripeObject
import com.stripe.model.Subscription
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

        assertEquals(
            ResponseEntity.badRequest().body("Signature Verification failed"),
            testWebHook().stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Any exception during event construction")
    @Test
    fun anyOtherException() {
        every { eventBuilder.constructEvent(any(), any(), any()) } throws IllegalStateException()

        assertEquals(
            ResponseEntity.badRequest().body("Event handling failed"),
            testWebHook().stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Any exception during supports check")
    @Test
    fun exceptionDuringSupportsCheck() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val throwsOnSupport = ThrowsOnSupport()

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(throwsOnSupport).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(throwsOnSupport.exectuedOnReceive, "onReceive was executed")
    }

    @DisplayName("Any exception during onReceive call")
    @Test
    fun exceptionDuringOnReceiveCall() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(ThrowsOnReceive()).stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("Event Class Not Supported")
    @Test
    fun doesNotSupportsEventClass() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())
        val invoiceEventHandler = InvoiceEventHandler()

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(invoiceEventHandler).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(invoiceEventHandler.exectuedOnReceive)
    }

    @DisplayName("Event Type Not Supported")
    @Test
    fun doesNotSupportsEventType() {
        val eventType = "someEventType"
        val otherEventType = "someOtherEventType"
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>(), eventType)

        val eventTypeHandler = EventTypeHandler(otherEventType)

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(eventTypeHandler).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(eventTypeHandler.exectuedOnReceive)
    }

    @DisplayName("Previous Attributes Not Supported")
    @Test
    fun doesNotSupportsPreviousAttribues() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val previousAttribuesCheck = PreviousAttribuesCheck()

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(previousAttribuesCheck).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(previousAttribuesCheck.exectuedOnReceive)
    }

    @DisplayName("On Receive Method Called")
    @Test
    fun onReceiveExectuted() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val baseHandler = BaseHandler()

        assertEquals(
            ResponseEntity.ok(TEST_BODY),
            testWebHook(baseHandler).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertTrue(baseHandler.exectuedOnReceive)
    }

    private fun mockkEvent(
        stripeObject: StripeObject,
        type: String = "anyType",
        previousAttributes: Map<String, Any> = mapOf()
    ): Event {

        val event = mockk<Event>()
        val deserializer = mockk<EventDataObjectDeserializer>()
        val data = mockk<EventData>() {}
        every { event.dataObjectDeserializer } returns deserializer
        every { deserializer.deserializeUnsafe() } returns stripeObject
        every { event.type } returns type
        every { event.data } returns data
        every { data.previousAttributes } returns previousAttributes
        return event
    }

    class ThrowsOnSupport : StripeEventHandler<Subscription>(Subscription::class.java) {

        var exectuedOnReceive: Boolean = false

        override fun supports(eventType: String): Boolean {
            throw IllegalStateException()
        }

        override fun onReceive(stripeObject: Subscription) {
            exectuedOnReceive = true
        }
    }

    class ThrowsOnReceive : StripeEventHandler<Subscription>(Subscription::class.java) {
        override fun onReceive(stripeObject: Subscription) {
            throw IllegalStateException()
        }
    }

    class InvoiceEventHandler : StripeEventHandler<Invoice>(Invoice::class.java) {
        var exectuedOnReceive: Boolean = false
        override fun onReceive(stripeObject: Invoice) {
            exectuedOnReceive = true
        }
    }

    class EventTypeHandler(private val eventType: String) : StripeEventHandler<Subscription>(Subscription::class.java) {

        var exectuedOnReceive: Boolean = false

        override fun supports(eventType: String): Boolean {
            return eventType == this.eventType
        }

        override fun onReceive(stripeObject: Subscription) {
            exectuedOnReceive = true
        }
    }

    class PreviousAttribuesCheck : StripeEventHandler<Subscription>(Subscription::class.java) {

        var exectuedOnReceive: Boolean = false

        override fun supports(previousAttributes: Map<String, Any>): Boolean {
            return false
        }

        override fun onReceive(stripeObject: Subscription) {
            exectuedOnReceive = true
        }
    }

    class BaseHandler : StripeEventHandler<Subscription>(Subscription::class.java) {
        var exectuedOnReceive: Boolean = false

        override fun onReceive(stripeObject: Subscription) {
            exectuedOnReceive = true
        }

    }

}

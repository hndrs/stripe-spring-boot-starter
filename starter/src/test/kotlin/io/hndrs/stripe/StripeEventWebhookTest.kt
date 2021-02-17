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

    private fun testWebHook(stripeEventReceiver: StripeEventReceiver<*>? = null): StripeEventWebhook {
        return stripeEventReceiver?.let {
            StripeEventWebhook(listOf(stripeEventReceiver as StripeEventReceiver<StripeObject>), "", eventBuilder)
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
    fun previousAttributesNull() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>(), previousAttributes = null)

        val ex = IllegalStateException()
        val throwsOnSupport = ThrowsOnSupport(ex)

        assertEquals(
            ResponseEntity.ok(listOf(ReceiverExecution(ThrowsOnSupport::class.simpleName!!, null, ex.message))),
            testWebHook(throwsOnSupport).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(throwsOnSupport.exectuedOnReceive, "onReceive was executed")
    }

    @DisplayName("Any exception during supports check")
    @Test
    fun exceptionDuringOnCondition() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val ex = IllegalStateException()
        val throwsOnSupport = ThrowsOnSupport(ex)

        assertEquals(
            ResponseEntity.ok(listOf(ReceiverExecution(ThrowsOnSupport::class.simpleName!!, null, ex.message))),
            testWebHook(throwsOnSupport).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(throwsOnSupport.exectuedOnReceive, "onReceive was executed")
    }

    @DisplayName("Any exception during onReceive call")
    @Test
    fun exceptionDuringOnReceiveCall() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val ex = IllegalStateException()
        assertEquals(
            ResponseEntity.ok(
                listOf(
                    ReceiverExecution(
                        ThrowsOnReceive::class.simpleName!!,
                        null,
                        ex.message
                    )
                )
            ),
            testWebHook(ThrowsOnReceive(ex)).stripeEvents(HttpHeaders(), TEST_BODY)
        )
    }

    @DisplayName("onCondtion(Event Class)")
    @Test
    fun onConditionEventClass() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())
        val invoiceEventHandler = InvoiceEventReceiver()

        assertEquals(
            ResponseEntity.ok(listOf<ReceiverExecution>()),
            testWebHook(invoiceEventHandler).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(invoiceEventHandler.exectuedOnReceive)
    }

    @DisplayName("onCondtion(Event)")
    @Test
    fun onConditionEvent() {
        val eventType = "someEventType"
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>(), eventType)

        val testReceiver = TestReceiver(onConditionEventType = false)

        assertEquals(
            ResponseEntity.ok(listOf<ReceiverExecution>()),
            testWebHook(testReceiver).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(testReceiver.exectuedOnReceive)
    }

    @DisplayName("onCondition(previousAttributes Map)")
    @Test
    fun onConditionPreviousAttributes() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val testReceiver = TestReceiver(onConditionPreviousAttributes = false)

        assertEquals(
            ResponseEntity.ok(listOf<ReceiverExecution>()),
            testWebHook(testReceiver).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(testReceiver.exectuedOnReceive)
    }

    @DisplayName("onCondition(stripeObject)")
    @Test
    fun onConditionStripeObject() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val testReceiver = TestReceiver(onConditionStripeObject = false)

        assertEquals(
            ResponseEntity.ok(listOf<ReceiverExecution>()),
            testWebHook(testReceiver).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(testReceiver.exectuedOnReceive)
    }

    @DisplayName("onCondition(previousAttributes Map ,stripeObject)")
    @Test
    fun onConditionPreviousAttributesAndStripeObject() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val testReceiver = TestReceiver(onConditionPreviousAttributesAndStripeObject = false)

        assertEquals(
            ResponseEntity.ok(listOf<ReceiverExecution>()),
            testWebHook(testReceiver).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertFalse(testReceiver.exectuedOnReceive)
    }

    @DisplayName("On Receive Method Called")
    @Test
    fun onReceiveExectuted() {
        every { eventBuilder.constructEvent(any(), any(), any()) } returns mockkEvent(mockk<Subscription>())

        val baseHandler = TestReceiver()

        assertEquals(
            ResponseEntity.ok(listOf(ReceiverExecution(TestReceiver::class.simpleName!!, Unit, null))),
            testWebHook(baseHandler).stripeEvents(HttpHeaders(), TEST_BODY)
        )
        assertTrue(baseHandler.exectuedOnReceive)
    }

    private fun mockkEvent(
        stripeObject: StripeObject,
        type: String = "anyType",
        previousAttributes: Map<String, Any>? = mapOf()
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

    class ThrowsOnSupport(private val ex: Exception) : StripeEventReceiver<Subscription>(Subscription::class.java) {

        var exectuedOnReceive: Boolean = false

        override fun onCondition(event: Event): Boolean {
            throw ex
        }

        override fun onReceive(stripeObject: Subscription, event: Event) {
            exectuedOnReceive = true
        }
    }

    class ThrowsOnReceive(private val ex: Exception) : StripeEventReceiver<Subscription>(Subscription::class.java) {
        override fun onReceive(stripeObject: Subscription, event: Event) {
            throw ex
        }
    }

    class InvoiceEventReceiver : StripeEventReceiver<Invoice>(Invoice::class.java) {
        var exectuedOnReceive: Boolean = false
        override fun onReceive(stripeObject: Invoice, event: Event) {
            exectuedOnReceive = true
        }
    }

    class TestReceiver(
        private val onConditionEventType: Boolean = true,
        private val onConditionStripeObject: Boolean = true,
        private val onConditionPreviousAttributes: Boolean = true,
        private val onConditionPreviousAttributesAndStripeObject: Boolean = true,
    ) : StripeEventReceiver<Subscription>(Subscription::class.java) {

        override fun onCondition(event: Event): Boolean {
            return onConditionEventType
        }

        override fun onCondition(stripeObject: Subscription): Boolean {
            return onConditionStripeObject
        }

        override fun onCondition(previousAttributes: Map<String, Any?>?): Boolean {
            return onConditionPreviousAttributes
        }

        override fun onCondition(previousAttributes: Map<String, Any?>?, stripeObject: Subscription): Boolean {
            return onConditionPreviousAttributesAndStripeObject
        }

        var exectuedOnReceive: Boolean = false

        override fun onReceive(stripeObject: Subscription, event: Event) {
            exectuedOnReceive = true
        }

    }

}

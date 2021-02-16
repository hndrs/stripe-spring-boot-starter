package io.hndrs.stripe

import com.fasterxml.jackson.annotation.JsonProperty
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.StripeObject
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import kotlin.reflect.KClass

@RestController
class StripeEventWebhook(
    private val stripeEventReceivers: List<StripeEventReceiver<StripeObject>>,
    private val signingSecret: String,
    private val eventBuilder: StripeEventBuilder = StripeEventBuilder(),
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(StripeEventWebhook::class.java)
    }

    @PostMapping("\${hndrs.stripe.webhook-path}")
    fun stripeEvents(
        @RequestHeader httpHeaders: HttpHeaders,
        @RequestBody body: String
    ): ResponseEntity<*> {

        val sigHeader = httpHeaders["stripe-signature"]?.firstOrNull().orEmpty()

        // verify signing secret and construct event
        val event = try {
            eventBuilder.constructEvent(
                body, sigHeader, signingSecret
            )
        } catch (e: SignatureVerificationException) {
            // Invalid signature
            LOG.error("Failed to verify stripe signature", e)
            return ResponseEntity.badRequest().body("Signature Verification failed")
        } catch (e: Exception) {
            LOG.error("Failed to handle event callback", e)
            return ResponseEntity.badRequest().body("Event handling failed")
        }

        val exceptions = mutableMapOf<KClass<*>, Exception>()
        val results = mutableMapOf<KClass<*>, Any?>()

        val stripeObject = event.dataObjectDeserializer.deserializeUnsafe()
        stripeEventReceivers.stream()
            .forEach { eventHandler ->
                try {
                    if (eventHandler.supports(stripeObject.javaClass, event.type, event.data.previousAttributes)) {
                        val result = eventHandler.onReceive(stripeObject)
                        results[eventHandler::class] = result
                    }
                } catch (e: Exception) {
                    exceptions[eventHandler::class] = e
                    LOG.error("Error while executing {}", eventHandler::class.java.canonicalName)
                }
            }

        return ResponseEntity.ok(
            ReceiverExecution.of(results, exceptions)
        )
    }
}

data class ReceiverExecution(
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("result")
    val result: Any?,
    @field:JsonProperty("exceptionMessage")
    val exceptionMessage: String?) {

    companion object {
        fun of(
            results: MutableMap<KClass<*>, Any?>,
            exceptions: MutableMap<KClass<*>, Exception>
        ): List<ReceiverExecution> {
            return (results.keys + exceptions.keys)
                .map {
                    val name = it.simpleName ?: "Anonymous"
                    ReceiverExecution(name, results[it], exceptions[it]?.message)
                }
        }
    }
}

/**
 * Delegate class introduced to give the possiblibity to test [StripeEventWebhook]
 */
class StripeEventBuilder() {

    fun constructEvent(payload: String, signature: String, signingSecret: String): Event {
        return Webhook.constructEvent(
            payload, signature, signingSecret
        )
    }
}

abstract class StripeEventReceiver<in T : StripeObject>(private val clazz: Class<T>) {

    /**
     * Set if handler should only be executed for a specifc event type
     */
    open fun supports(eventType: String): Boolean {
        return true
    }

    /**
     * Additional checks on previous attributes if handler should be executed
     */
    open fun supports(previousAttributes: Map<String, Any>): Boolean {
        return true
    }

    /**
     * internal support checks
     */
    internal fun supports(type: Class<Any>, eventType: String, previousAttributes: Map<String, Any>): Boolean {
        return type == clazz
                && supports(eventType)
                && supports(previousAttributes)
    }

    abstract fun onReceive(stripeObject: T): Any?

}

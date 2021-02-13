package io.hndrs.stripe

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.StripeObject
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@RestController
class StripeEventWebhook(
    private val stripeEventHandlers: List<StripeEventHandler<StripeObject>>,
    private val signingSecret: String,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(StripeEventWebhook::class.java)
    }

    @PostMapping("/stripe-events")
    fun stripeEvents(
        @RequestHeader httpHeaders: HttpHeaders,
        @RequestBody body: String
    ): ResponseEntity<String> {

        LOG.info("Events Callback {}\n{}", httpHeaders, body)

        val sigHeader = httpHeaders["stripe-signature"]?.firstOrNull().orEmpty()

        // verify signing secret and construct event
        val event = try {
            Webhook.constructEvent(
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

        // handle the event
        event?.let {

            val stripeObject = event.dataObjectDeserializer.deserializeUnsafe()

            Event.CHARSET
            stripeEventHandlers.stream()
                .filter { stripeEvent ->
                    stripeEvent.supports(
                        stripeObject.javaClass,
                        event.type,
                        event.data.previousAttributes
                    )
                }
                .forEach { stripeEvent ->
                    stripeEvent.onReceive(stripeObject)
                }
        }

        return ResponseEntity.ok(body)
    }
}

abstract class StripeEventHandler<in T : StripeObject>(private val clazz: Class<T>) {

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

    abstract fun onReceive(stripeObject: T)

}

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
            .forEach { eventReceiver ->
                try {
                    val evaluationReport = eventReceiver.onCondition(
                        stripeObject.javaClass,
                        event,
                        event.data.previousAttributes,
                        stripeObject
                    )
                    LOG.debug("{} evaluation report: {}", eventReceiver::class.simpleName, evaluationReport)
                    if (evaluationReport.evaluate()) {
                        val result = eventReceiver.onReceive(stripeObject, event)
                        results[eventReceiver::class] = result
                    }
                } catch (e: Exception) {
                    exceptions[eventReceiver::class] = e
                    LOG.error("Error while executing {}", eventReceiver::class.java.canonicalName)
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
    val exceptionMessage: String?
) {

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
class StripeEventBuilder {

    fun constructEvent(payload: String, signature: String, signingSecret: String): Event {
        return Webhook.constructEvent(
            payload, signature, signingSecret
        )
    }
}

abstract class StripeEventReceiver<in T : StripeObject>(private val clazz: Class<T>) {

    /**
     * Conditional to execute [StripeEventReceiver][onReceive]
     */
    open fun onCondition(event: Event): Boolean {
        return true
    }

    /**
     * Conditional to execute [StripeEventReceiver][onReceive]
     */
    open fun onCondition(stripeObject: T): Boolean {
        return true
    }

    /**
     * Conditional to execute [StripeEventReceiver][onReceive]
     */
    open fun onCondition(previousAttributes: Map<String, Any?>?): Boolean {
        return true
    }

    /**
     * Conditional to execute [StripeEventReceiver][onReceive]
     */
    open fun onCondition(previousAttributes: Map<String, Any?>?, stripeObject: T): Boolean {
        return true
    }

    /**
     * internal support checks
     */
    internal fun onCondition(type: Class<Any>, event: Event, previousAttributes: Map<String, Any?>?, stripeObject: T): ConditionEvaluationReport {
        return ConditionEvaluationReport(
            type == clazz, onCondition(event), onCondition(previousAttributes), onCondition(stripeObject), onCondition(previousAttributes, stripeObject)
        )
    }

    abstract fun onReceive(stripeObject: T, event: Event): Any?

    data class ConditionEvaluationReport(
        val onClass: Boolean,
        val onEvent: Boolean,
        val onPreviousAttributes: Boolean,
        val onStripeObject: Boolean,
        val onPreviousAttributesAndStripeObject: Boolean,
    ) {
        fun evaluate(): Boolean {
            return onClass && onEvent && onPreviousAttributes && onStripeObject && onStripeObject && onPreviousAttributesAndStripeObject
        }
    }
}

package io.hndrs.stripe.sample

import com.stripe.model.Subscription
import io.hndrs.stripe.StripeEventReceiver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class ExampleReceiver : StripeEventReceiver<Subscription>(Subscription::class.java) {

    override fun onReceive(stripeObject: Subscription) {
        LOG.info("Received event {}", stripeObject)
        stripeObject.toJson()
    }

    override fun onCondition(eventType: String): Boolean {
        // check the event type
        return eventType == "customer.subscription.updated"
    }

    override fun onCondition(previousAttributes: Map<String, Any>, stripeObject: Subscription): Boolean {
        return true
    }

    override fun onCondition(previousAttributes: Map<String, Any>): Boolean {
        // possibility to check previous attributes to check if event should be handled
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ExampleReceiver::class.java)
    }
}

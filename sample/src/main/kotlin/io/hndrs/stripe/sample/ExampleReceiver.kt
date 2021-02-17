package io.hndrs.stripe.sample

import com.stripe.model.Event
import com.stripe.model.Subscription
import io.hndrs.stripe.StripeEventReceiver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class ExampleReceiver : StripeEventReceiver<Subscription>(Subscription::class.java) {

    override fun onCondition(event: Event): Boolean {
        // conditional based stripe event
        return event.type == "customer.subscription.updated"
    }

    override fun onCondition(stripeObject: Subscription): Boolean {
        // conditional based stripe object
        return true
    }

    override fun onCondition(previousAttributes: Map<String, Any?>?): Boolean {
        // conditional based previousAttributes
        return true
    }

    override fun onCondition(previousAttributes: Map<String, Any?>?, stripeObject: Subscription): Boolean {
        // conditional based previousAttributes and stripe object
        return true
    }

    override fun onReceive(stripeObject: Subscription, event: Event) {
        LOG.info("Received event {}", event)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ExampleReceiver::class.java)
    }
}

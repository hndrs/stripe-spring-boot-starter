package io.hndrs.stripe.sample

import com.stripe.model.Subscription
import io.hndrs.stripe.StripeEventHandler
import org.springframework.stereotype.Component

@Component
open class SampleHandler : StripeEventHandler<Subscription>(Subscription::class.java) {
    override fun onReceive(stripeObject: Subscription) {
        //do something with received object
        println(stripeObject)
    }

    override fun supports(eventType: String): Boolean {
        return eventType == "customer.subscription.updated"
    }


    override fun supports(previousAttributes: Map<String, Any>): Boolean {
        // check previous attributes to see what changed
        return true
    }


}

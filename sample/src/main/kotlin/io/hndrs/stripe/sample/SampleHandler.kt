package io.hndrs.stripe.sample

import com.stripe.model.Subscription
import io.hndrs.stripe.StripeEventHandler
import org.springframework.stereotype.Component

@Component("stripehandler")
open class SampleHandler : StripeEventHandler<Subscription>(Subscription::class.java) {
    override fun onReceive(stripeObject: Subscription) {
        //do something with received object
        println(stripeObject)
    }

    override fun supports(eventType: String): Boolean {
        return eventType == "customer.subscription.updated"
    }


}

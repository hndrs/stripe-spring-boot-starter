package io.hndrs.stripe.sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class StripeWebhookApplication {
}

fun main(args: Array<String>) {
    runApplication<StripeWebhookApplication>(*args)
}

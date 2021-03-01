package io.hdnrs.autoconfiguration

import com.stripe.Stripe
import com.stripe.model.StripeObject
import io.hdnrs.autoconfiguration.StripeConfigurationProperties.Companion.PROPERTY_PREFIX
import io.hndrs.stripe.StripeEventReceiver
import io.hndrs.stripe.StripeEventWebhook
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConditionalOnWebApplication
@EnableConfigurationProperties(StripeConfigurationProperties::class)
@Configuration
@ConditionalOnClass(Stripe::class)
open class StripeWebhookAutoConfiguration(private val properties: StripeConfigurationProperties) {


    @Bean
    open fun stripeEventWebhook(stripeEventReceivers: List<StripeEventReceiver<*>>): StripeEventWebhook {
        return StripeEventWebhook(
            stripeEventReceivers as List<StripeEventReceiver<StripeObject>>,
            properties.signingSecret
        )
    }

}

@ConfigurationProperties(PROPERTY_PREFIX)
class StripeConfigurationProperties {

    lateinit var signingSecret: String

    lateinit var webhookPath: String

    companion object {
        const val PROPERTY_PREFIX = "hndrs.stripe"
    }
}

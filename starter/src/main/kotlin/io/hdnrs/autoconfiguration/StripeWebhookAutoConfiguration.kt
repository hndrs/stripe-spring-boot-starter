package io.hdnrs.autoconfiguration

import com.stripe.model.StripeObject
import io.hdnrs.autoconfiguration.StripeConfigurationProperties.Companion.PROPERTY_PREFIX
import io.hndrs.stripe.StripeEventHandler
import io.hndrs.stripe.StripeEventWebhook
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConditionalOnWebApplication
@EnableConfigurationProperties(StripeConfigurationProperties::class)
@Configuration
open class StripeWebhookAutoConfiguration(private val properties: StripeConfigurationProperties) {


    @Bean
    open fun stripeEventWebhook(stripeEventHandlers: List<StripeEventHandler<*>>): StripeEventWebhook {
        return StripeEventWebhook(
            stripeEventHandlers as List<StripeEventHandler<StripeObject>>,
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

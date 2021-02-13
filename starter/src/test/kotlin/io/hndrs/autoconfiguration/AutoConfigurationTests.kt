package io.hndrs.autoconfiguration

import io.hdnrs.autoconfiguration.StripeWebhookAutoConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

@DisplayName("Stripe Webhook Autoconfiguration")
class StripeWebhookAutoConfigurationTests {

    @Test
    fun autoconfiguredBeans() {
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(StripeWebhookAutoConfiguration::class.java)
            )
            .withPropertyValues("hndrs.stripe.signing-secret:testSecret", "hndrs.stripe.webhook-path:/events")
            .run {
                Assertions.assertNotNull(it.getBean(StripeWebhookAutoConfiguration::class.java))
            }
    }
}

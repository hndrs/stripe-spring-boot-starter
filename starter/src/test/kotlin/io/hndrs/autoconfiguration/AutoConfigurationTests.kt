package io.hndrs.autoconfiguration

import com.stripe.Stripe
import io.hdnrs.autoconfiguration.StripeWebhookAutoConfiguration
import io.hndrs.stripe.StripeEventWebhook
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
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
                assertNotNull(it.getBean(StripeEventWebhook::class.java))
            }
    }

    @Test
    fun autoconfiguredMissingStripeClass() {
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(StripeWebhookAutoConfiguration::class.java)
            )
            .withClassLoader(FilteredClassLoader(Stripe::class.java))
            .withPropertyValues("hndrs.stripe.signing-secret:testSecret", "hndrs.stripe.webhook-path:/events")
            .run {
                assertThrows(NoSuchBeanDefinitionException::class.java) { it.getBean(StripeEventWebhook::class.java) }
            }
    }
}

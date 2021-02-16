### What You Will Build

You will build an application that receives a subscription update stripe event by using ```StripeEventHandler```.

### What You Need

- About 15 minutes
- A favorite text editor or IDE
- JDK 11 or later
- Gradle

### Starting with Spring Initializr

For all Spring applications, you should start with the [Spring Initializr](https://start.spring.io/). The Initializr
offers a fast way to pull in all the dependencies you need for an application and does a lot of the set up for you. This
example needs only the Spring Web dependency, Java 11 and Gradle

Add the following to your ```build.gradle.kts``` or ```build.gradle```

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.4.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.spring") version "1.4.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // add the stripe spring boot starter to your gradle build file
    implementation("io.hndrs:stripe-spring-boot-starter:1.0.0")

    // add the stripe java library 
    implementation("com.stripe:stripe-java:20.37.0")


    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


```

### Configure the stripe webhook

To configure the stripe webhook we need to set its ```signingSecret``` and the ```webhook-path```. Add the following
properties to ```src/main/resources/application```

```properties
hndrs.stripe.signingSecret=whsc_**********
hndrs.stripe.webhook-path=/stripe-events
```

> The signing secret can be obtained on your [Stripe Dashboard](https://dashboard.stripe.com/test/webhooks) or with the [Stripe CLI](https://stripe.com/docs/stripe-cli/webhooks)

### Create a Stripe Event Handler

With any webhook-event-based application, you need to create a receiver that responds to published webhook events. The
following implementation shows how to do so:

```kotlin
@Component
open class ExampleReceiver : StripeEventReceiver<Subscription>(Subscription::class.java) {

    override fun onReceive(stripeObject: Subscription) {
        LOG.info("Received event {}", stripeObject)
    }

    override fun supports(eventType: String): Boolean {
        // check the event type
        return eventType == "customer.subscription.updated"
    }


    override fun supports(previousAttributes: Map<String, Any>): Boolean {
        // possibility to check previous attributes to check if event should be handled
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ExampleReceiver::class.java)
    }
}
```

### Send a Test Message

Use the [Stripe Cli](https://stripe.com/docs/stripe-cli/webhooks) to send a test event.

#### Listen for events

```shell
stripe listen
```

#### Trigger an event

```shell
stripe trigger customer.subscription.updated
```

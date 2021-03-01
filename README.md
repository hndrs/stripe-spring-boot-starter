[![Current Version](https://img.shields.io/maven-central/v/io.hndrs/hndrs_stripe-spring-boot-starter?style=for-the-badge&logo=sonar)](https://search.maven.org/search?q=io.hndrs)
[![Coverage](https://img.shields.io/sonar/coverage/hndrs_stripe-spring-boot-starter?server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/dashboard?id=hndrs_stripe-spring-boot-starter)
[![Supported Java Version](https://img.shields.io/badge/Supported%20Java%20Version-11%2B-informational?style=for-the-badge)]()
[![License](https://img.shields.io/github/license/hndrs/stripe-spring-boot-starter?style=for-the-badge)]()
[![Sponsor](https://img.shields.io/static/v1?logo=GitHub&label=Sponsor&message=%E2%9D%A4&color=ff69b4&style=for-the-badge)](https://github.com/sponsors/marvinschramm)

# stripe-spring-boot-starter

Follow the [Getting Started Guide](GETTING_STARTED_GUIDE.md) or look at the [Sample](/sample) to help setting up
stripe-spring-boot-starter.

#### Dependency

```kotlin
implementation("io.hndrs:stripe-spring-boot-starter:1.0.0")

//skip this if you already have the stripe dependency in your project
implementation("com.stripe:stripe-java:<version>")
```

> Gradle

#### Configuration

```properties
hndrs.stripe.signing-secret=whsec_*******************
hndrs.stripe.webhook-path=/stripe-events
```

> application.properties

#### StripeEventReceiver

There are 3 conditional methods that can be used to narrow the execution condition (Note: there is an
internal ```class``` conditional that makes sure that the receiver only receives the defined generic type. You can
override any of the following methods (by default they return true)

- ```onCondition(event: Event)```
    - *It is recommended to use this conditional to check at least the event type*
- ```onReceive(stripeObject: Subscription)```
    - *It is recommended to use this when your condition **only** needs values from the ```stripeObject``` for your
      business logic
- ```onCondition(previousAttributes: Map<String, Any?>?)```
    - *It is recommended to use this conditional when your condition needs **only** values from
      the ```previousAttributes```*
- ```onCondition(previousAttributes: Map<String, Any?>?, stripeObject: Subscription)```
    - *It is recommended to use this conditional when your condition needs a combination of the ```previousAttributes```
      and the received ```stripeObject```*

Implementing a ```StripeEventReceiver``` looks like the following:

```kotlin
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

    override fun onCondition(previousAttributes: Map<String, Any>): Boolean {
        // conditional based on previousAttributes
        return true
    }

    override fun onCondition(previousAttributes: Map<String, Any>, stripeObject: Subscription): Boolean {
        // conditional based previousAttributes and stripe object
        return true
    }

    override fun onReceive(stripeObject: Subscription) {
        // do something with the received object
    }
}
```

> The ```StripeEventReceiver``` generic needs to be subclass of a [StripeObject](https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/model/StripeObject.java)

#### Accessing Snapshots

To access [snapshot builds](https://github.com/hndrs/stripe-spring-boot-starter/packages) add the following to your
gradle script

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/hndrs/stripe-spring-boot-starter")
    credentials {
        username = "<GITHUB_USERNAME>"
        password = "<PERSONAL_ACCESS_TOKEN"
    }
}

dependencies {
    implementation(group = "io.hndrs", name = "stripe-spring-boot-starter", version = "1.0.0-SNAPSHOT")
}

```

> Your personal access token needs the read:packages scope (Download packages from GitHub Package Registry)

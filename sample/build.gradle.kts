plugins {
    id("org.springframework.boot") version "2.4.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.stripe:stripe-java:20.37.0")
}

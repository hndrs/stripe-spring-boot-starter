repositories {
    mavenCentral()
}

dependencies {
    optional("org.springframework.boot:spring-boot-starter-web")
    api(group = "org.springframework.boot", name = "spring-boot-autoconfigure")
    optional("com.stripe:stripe-java:20.37.0")

    annotationProcessor(group = "org.springframework.boot", name = "spring-boot-configuration-processor")
    kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
}

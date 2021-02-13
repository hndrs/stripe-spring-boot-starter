plugins {
    id("org.springframework.boot") version "2.4.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    implementation("org.springframework.boot:spring-boot-starter")
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

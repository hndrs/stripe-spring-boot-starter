rootProject.name = "stripe-spring-boot-starter"

include("starter")
project(":starter").projectDir = File("starter")

include("sample")
project(":sample").projectDir = File("sample")

val springBootDependencies: String by settings

pluginManagement {
    val kotlinVersion: String by settings
    val springDependencyManagement: String by settings

    plugins {
        id("io.spring.dependency-management").version(springDependencyManagement)
        kotlin("jvm").version(kotlinVersion)
        kotlin("plugin.spring").version(kotlinVersion)
        kotlin("kapt").version(kotlinVersion)
        id("maven-publish")
        id("idea")
    }
    repositories {
    }
}

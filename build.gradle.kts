import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://repo.spring.io/plugins-release")
    }
    dependencies {
        classpath("io.spring.gradle:propdeps-plugin:0.0.9.RELEASE")
    }
}
apply(plugin = "propdeps")
apply(plugin = "propdeps-idea")

plugins {
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
    kotlin("kapt").version("1.4.30")
    id("maven-publish")
    id("idea")
}

group = "io.hndrs"
version = "1.0.1"
java.sourceCompatibility = JavaVersion.VERSION_15


repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "propdeps")
    apply(plugin = "propdeps-idea")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "15"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    dependencyManagement {
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.4.2") {
                bomProperty("kotlin.version", "1.4.30")
            }
        }
    }
}

val sourcesJarSubProject by tasks.creating(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}


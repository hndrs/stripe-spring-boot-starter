import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
    kotlin("kapt").version("1.4.30")
    id("maven-publish")
}

group = "io.hndrs"
version = "1.0.1"
java.sourceCompatibility = JavaVersion.VERSION_15

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


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    //TODO make sure version is not "fixed" and can be overridden
    api("com.stripe:stripe-java:20.37.0")


    testImplementation("org.springframework.boot:spring-boot-starter-test")

    annotationProcessor(group = "org.springframework.boot", name = "spring-boot-configuration-processor")
    kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")
    api(group = "org.springframework.boot", name = "spring-boot-autoconfigure")
}

allprojects {

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "15"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "kotlin-kapt")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "15"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
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


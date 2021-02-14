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

val springBootDependencies: String by extra
val kotlinVersion: String by extra

plugins {
    id("org.sonarqube").version("3.1.1")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
    id("maven-publish")
    id("idea")
    id("signing")
}

group = "io.hndrs"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11


repositories {
    mavenCentral()
}

sonarqube {
    properties {
        property("sonar.projectKey", "hndrs_stripe-spring-boot-starter")
        property("sonar.organization", "hndrs")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.exclusions", "**/sample/**")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "propdeps")
    apply(plugin = "propdeps-idea")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.6"
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.apply {
                isEnabled = true
            }

        }
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

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    dependencyManagement {
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootDependencies") {
                bomProperty("kotlin.version", kotlinVersion)
            }
        }
    }

    val sourcesJarSubProject by tasks.creating(Jar::class) {
        dependsOn("classes")
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    if (project.name != "sample") {

        publishing {
            repositories {
                maven {
                    name = "release"
                    url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials {
                        username = System.getenv("SONATYPE_USER")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }
            publications {
                create<MavenPublication>(project.name) {
                    from(components["java"])
                    artifact(sourcesJarSubProject)

                    groupId = rootProject.group as? String
                    artifactId = rootProject.name
                    version = "${rootProject.version}${project.findProperty("version.appendix") ?: ""}"
                }
            }
            val signingKey: String? = System.getenv("SIGNING_KEY")
            val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
            if (signingKey != null && signingPassword != null) {
                signing {
                    useInMemoryPgpKeys(groovy.json.StringEscapeUtils.unescapeJava(signingKey), signingPassword)
                    sign(publications[project.name])
                }
            }
        }
    }
}


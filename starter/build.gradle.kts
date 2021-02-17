import io.hndrs.gradle.plugin.publishingInfo

repositories {
    mavenCentral()
}

dependencies {
    api(group = "org.springframework.boot", name = "spring-boot-autoconfigure")
    optional("org.springframework.boot:spring-boot-starter-web")
    optional("com.stripe:stripe-java:20.37.0")

    annotationProcessor(group = "org.springframework.boot", name = "spring-boot-configuration-processor")
    kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("org.junit-pioneer:junit-pioneer:1.3.0")
}

publishingInfo {
    name = rootProject.name
    description = "Stripe webhook for spring boot"
    url = "https://github.com/hndrs/stripe-spring-boot-starter"
    license = io.hndrs.gradle.plugin.License(
        "https://github.com/hndrs/stripe-spring-boot-starter/blob/main/LICENSE",
        "MIT License"
    )
    developers = listOf(
        io.hndrs.gradle.plugin.Developer("marvinschramm", "Marvin Schramm", "marvin.schramm@gmail.com")
    )
    contributers = listOf(
        io.hndrs.gradle.plugin.Contributor("Kevin Joffe", "")
    )
    organization = io.hndrs.gradle.plugin.Organization("hndrs", "https://oss.hndrs.io")
    scm = io.hndrs.gradle.plugin.Scm(
        "scm:git:git://github.com/hndrs/stripe-spring-boot-starter",
        "https://github.com/hndrs/stripe-spring-boot-starter"
    )
}

val sourcesJarSubProject by tasks.creating(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

java {
    withJavadocJar()
}

publishing {
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

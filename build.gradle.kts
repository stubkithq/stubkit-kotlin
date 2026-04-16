plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `maven-publish`
}

group = "com.stubkit"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.stubkit"
            artifactId = "stubkit"
            version = project.version.toString()
            from(components["java"])

            pom {
                name.set("Stubkit Kotlin SDK")
                description.set("Kotlin SDK for Stubkit subscription validation API")
                url.set("https://github.com/stubkithq/stubkit-kotlin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}

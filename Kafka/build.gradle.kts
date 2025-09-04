plugins {
    id("java")
    id("org.gradle.war")
}


group = "it.unisannio.gateway"
version = "unspecified"

// Aggiungi questa configurazione Java
java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")


    implementation("org.apache.kafka:kafka-clients:3.7.0")
}

tasks.test {
    useJUnitPlatform()
}
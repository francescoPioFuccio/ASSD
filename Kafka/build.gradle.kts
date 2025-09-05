plugins {
    id("java")
    id("org.gradle.war")
}


group = "it.unisannio.gateway"
version = "unspecified"

// Aggiungi questa configurazione Java
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Aggiungi questo per assicurarti che tutti i task usino la versione corretta
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")


    implementation("org.apache.kafka:kafka-clients:3.7.0")
}

tasks.test {
    useJUnitPlatform()
}
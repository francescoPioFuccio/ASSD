plugins {
    id("org.gradle.war")
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"



dependencies {
    implementation(project(":UserDataModule2"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Usa le API Jakarta fornite dal container (RESTEasy su WildFly)
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    // JSON handling esistenti
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.10.1")

    // ===== NUOVE DIPENDENZE KAFKA =====
    // Kafka Client
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // Jackson per JSON serialization/deserialization (alternativa a Gson per Kafka)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")

    // Jakarta EE APIs per WildFly (se non già presenti)
    compileOnly("jakarta.ejb:jakarta.ejb-api:4.0.1")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")

    // Per ManagedExecutorService (raccomandato per WildFly)
    compileOnly("jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.0.3")
}

tasks.named<War>("war") {
    // Imposta il nome del file WAR
    archiveFileName.set("gestionemuseo.war")


   // Gestisci i duplicati
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // o DuplicatesStrategy.REPLACE, se preferisci sovrascrivere
}


tasks.test {
    useJUnitPlatform()
}
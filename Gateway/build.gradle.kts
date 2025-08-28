plugins {
    id("java")
    id("org.gradle.war")
}

group = "it.unisannio.gateway"
version = "unspecified"



dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.jboss.resteasy:resteasy-core:6.2.8.Final")
    implementation("org.jboss.resteasy:resteasy-servlet-initializer:6.2.8.Final")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:6.2.8.Final")

        // Jakarta EE 10 (per WildFly 27+)
        implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
        implementation("jakarta.inject:jakarta.inject-api:2.0.1")
        implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
        implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")

        // Altre dipendenze
        implementation("org.mindrot:jbcrypt:0.4")
        implementation("com.google.code.gson:gson:2.10.1")
        implementation("org.json:json:20231013")

        // Per multipart (questa versione potrebbe causare problemi)
        implementation("org.jboss.resteasy:resteasy-multipart-provider:6.2.4.Final")

        // Le tue dipendenze di progetto
        implementation(project(":UserDataModule2"))
        implementation(project(":GestioneMusei"))
        implementation(project(":GestioneQuest"))
        implementation(project(":GestioneOpere"))



}
tasks.named<War>("war") {
    // Imposta il nome del file WAR
    archiveFileName.set("gateway.war")


    // Gestisci i duplicati
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // o DuplicatesStrategy.REPLACE, se preferisci sovrascrivere
}

tasks.test {
    useJUnitPlatform()
}
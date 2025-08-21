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
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.10.1")
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
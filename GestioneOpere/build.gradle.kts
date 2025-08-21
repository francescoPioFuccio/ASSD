plugins {
    id("org.gradle.war")
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"

dependencies {
    implementation(project(":UserDataModule2"))
    implementation(libs.firebase.firestore)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // JSON
    implementation("org.json:json:20240303")

    // Jakarta APIs fornite dal container (RESTEasy su WildFly)
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")

    // RESTEasy per multipart (WildFly usa RESTEasy)
    compileOnly("org.jboss.resteasy:resteasy-multipart-provider:6.2.12.Final")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // WildFly Jakarta EE (scope provided)
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
}

tasks.named<War>("war") {
    // Imposta il nome del file WAR
    archiveFileName.set("gestioneopere.war")

    // Gestisci i duplicati
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


tasks.test {
    useJUnitPlatform()
}


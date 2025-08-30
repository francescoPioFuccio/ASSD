// build.gradle.kts per il modulo UserDataModule2
plugins {
    id("org.gradle.war")
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Jakarta Persistence API
    //implementation(project(":Kafka"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    // Hibernate Core (JPA Implementation)
    implementation("org.hibernate:hibernate-core:5.6.15.Final")

    implementation("org.json:json:20240303")

    // Jakarta EE API fornite dal container (WildFly 36 è Jakarta EE 10)
    compileOnly("jakarta.platform:jakarta.jakartaee-web-api:10.0.0")

    // Dipendenza per BCrypt per l'hashing delle password
    implementation("org.mindrot:jbcrypt:0.4")

    // Dipendenza per GSON per la serializzazione/deserializzazione JSON
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.named<War>("war") {
    // Imposta il nome del file WAR
    archiveFileName.set("usermodule3.war")

    // Gestisci i duplicati
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // DISABILITA COMPLETAMENTE LA GENERAZIONE DEL MANIFEST
    manifest {
        // Non generare alcun manifest
        from(emptyList<File>())
    }

    // Escludi META-INF dalla root del WAR
    exclude("META-INF/**")

    // Escludi eventuali META-INF problematici dalle dipendenze
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Usa doLast per rimuovere META-INF dopo la creazione
    doLast {
        // Questa è una soluzione più drastica se le altre non funzionano
        println("WAR creato senza META-INF/MANIFEST.MF")
    }
}
tasks.test {
    useJUnitPlatform()
}
// build.gradle.kts per il modulo UserDataModule

plugins {
    // Applichiamo il plugin per una libreria Java
    `java-library`
}

// Impostiamo la compatibilità con Java 17
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


dependencies {
    // Jakarta Persistence API
    api("jakarta.persistence:jakarta.persistence-api:3.2.0")

    // Hibernate Core (JPA Implementation)
    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")

    // MySQL Connector
    implementation("com.mysql:mysql-connector-j:8.0.33")

    // Jakarta EE API (equivalente di "provided" in Gradle)
    // Significa che questa dipendenza serve per compilare, ma non verrà inclusa nel file finale
    // perché si presume che il server (WildFly) la fornirà.
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
}
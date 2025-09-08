// build.gradle.kts per il modulo QuestService (con supporto gRPC)
plugins {
    id("org.gradle.war")
    id("java")
    id("com.google.protobuf") version "0.9.4" // Plugin protobuf per generare codice gRPC
}

group = "it.unisannio.gateway"
version = "unspecified"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Aggiungi questo per assicurarti che tutti i task usino la versione corretta
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

// Determina il classificatore per il sistema operativo corrente
val osClassifier = when {
    System.getProperty("os.name").toLowerCase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").toLowerCase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
}

dependencies {
    // Jakarta Persistence API
    implementation(project(":Grpc-api"))
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.apache.kafka:kafka-clients:3.7.0")

    // Hibernate Core (JPA Implementation) - se necessario
    implementation("org.hibernate:hibernate-core:5.6.15.Final")
    implementation("org.json:json:20240303")

    // Jakarta EE API fornite dal container (WildFly 36 è Jakarta EE 10)
    compileOnly("jakarta.platform:jakarta.jakartaee-web-api:10.0.0")

    // Dipendenza per GSON per la serializzazione/deserializzazione JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Modulo Kafka condiviso (se hai un modulo separato per Kafka)
    implementation(project(":Kafka"))

    // Dipendenze gRPC
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    implementation("com.google.protobuf:protobuf-java:4.27.1")

    // Dipendenza per annotations (compatibilità)
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

// Configurazioni per protobuf
configurations {
    // Risolve l'ambiguità per il codice sorgente principale
    getByName("compileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
    // Risolve l'ambiguità anche per il codice di test
    getByName("testCompileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
}

// Configurazione protobuf
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.27.1:${osClassifier}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.1"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
            task.builtins {
                maybeCreate("java")
            }
        }
    }
}

tasks.named<War>("war") {
    // Imposta il nome del file WAR
    archiveFileName.set("questservice.war")

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
        println("WAR creato senza META-INF/MANIFEST.MF")
    }
}

tasks.test {
    useJUnitPlatform()
}
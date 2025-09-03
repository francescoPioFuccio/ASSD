plugins {
    id("org.gradle.war")
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

group = "it.unisannio.gateway"
version = "unspecified"

// Determina il classificatore per il sistema operativo corrente
val osClassifier = when {
    System.getProperty("os.name").toLowerCase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").toLowerCase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
}

dependencies {
    implementation(project(":UserDataModule2"))
    implementation(libs.firebase.firestore)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // JSON, Jakarta, RESTEasy, ecc...
    implementation("org.json:json:20240303")
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("org.jboss.resteasy:resteasy-multipart-provider:6.2.12.Final")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":Kafka"))
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("com.google.protobuf:protobuf-java:4.27.0")
    // Dipendenze gRPC - Aggiorna le versioni per compatibilità Jakarta
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
    implementation("com.google.protobuf:protobuf-java:4.27.1")
    // Aggiungi javax.annotation per compatibilità
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

configurations {
    // Risolve l'ambiguità per il codice sorgente principale (GIÀ PRESENTE)
    getByName("compileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
    // AGGIUNGI QUESTO: Risolve l'ambiguità anche per il codice di test (MANCANTE)
    getByName("testCompileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
}

protobuf {
    protoc {
        // Usa una versione più recente di protoc
        artifact = "com.google.protobuf:protoc:4.27.1:${osClassifier}"
    }
    plugins {
        create("grpc") {
            // Usa una versione più recente del plugin gRPC
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.1"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
            // Rimuovi l'opzione jakarta che causa l'errore
            task.builtins {
                maybeCreate("java")
            }
        }
    }
}

tasks.named<War>("war") {
    archiveFileName.set("gestioneopere.war")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
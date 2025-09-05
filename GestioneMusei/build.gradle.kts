plugins {
    id("org.gradle.war")  // Cambiato da "org.gradle.war"
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

group = "it.unisannio.gestoremusei"
version = "unspecified"
// Determina il classificatore per il sistema operativo corrente
val osClassifier = when {
    System.getProperty("os.name").toLowerCase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").toLowerCase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Aggiungi questo per assicurarti che tutti i task usino la versione corretta
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}
// Rimuovo il classificatore OS-specifico che può causare problemi

dependencies {
    implementation(project(":UserDataModule2"))
    implementation(project(":Kafka"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // JSON e utility
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.10.1")

    // Jakarta EE APIs (compileOnly per WildFly)
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("jakarta.ejb:jakarta.ejb-api:4.0.1")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    compileOnly("jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.0.3")

    // RESTEasy
    compileOnly("org.jboss.resteasy:resteasy-multipart-provider:6.2.12.Final")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // Jackson per Kafka
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")

    // gRPC e Protobuf (versioni aggiornate e consistenti)
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    implementation("com.google.protobuf:protobuf-java:4.27.1")

    // Annotations per generated code
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

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
            task.builtins {
                maybeCreate("java")
            }
        }
    }
}

// Configurazione source sets per proto
sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java", "build/generated/source/proto/main/grpc")
        }
        proto {
            srcDir("src/main/proto")
        }
        resources {
            // Escludi i file .proto dalle risorse per evitare duplicati
            exclude("**/*.proto")
        }
    }
}

tasks.named<War>("war") {
    archiveFileName.set("gestoremusei.war")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}

// Gestione duplicati per processResources
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Assicura che protobuf sia compilato prima di Java
tasks.compileJava {
    dependsOn(tasks.generateProto)
}
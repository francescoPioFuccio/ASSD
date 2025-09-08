plugins {
    id("java")
    id("org.gradle.war")
    id("com.google.protobuf") version "0.9.4"
}

val osClassifier = when {
    System.getProperty("os.name").toLowerCase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").toLowerCase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
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

dependencies {

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":Kafka"))
    implementation("org.jboss.resteasy:resteasy-core:6.2.8.Final")
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("org.jboss.resteasy:resteasy-servlet-initializer:6.2.8.Final")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:6.2.8.Final")

        // Jakarta EE 10 (per WildFly 27+)
        compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
        compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
        compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
        compileOnly("jakarta.transaction:jakarta.transaction-api:2.0.1")
        compileOnly("jakarta.ejb:jakarta.ejb-api:4.0.0")

        // Altre dipendenze
        implementation("org.mindrot:jbcrypt:0.4")
        implementation("com.google.code.gson:gson:2.10.1")
        implementation("org.json:json:20231013")
        // Per multipart (questa versione potrebbe causare problemi)
        implementation("org.jboss.resteasy:resteasy-multipart-provider:6.2.4.Final")
        implementation("org.apache.kafka:kafka-clients:3.6.1")
        // Le tue dipendenze di progetto
        implementation(project(":Grpc-api"))

        //implementation(project(":UserDataModule2"))
        //implementation(project(":GestioneMusei"))
        //implementation(project(":GestioneQuest"))
        //implementation(project(":GestioneOpere"))
        implementation(project(":Kafka"))

    // Dipendenze Protobuf e gRPC
    implementation("com.google.protobuf:protobuf-java:4.27.0")
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-java:4.27.1")


}
configurations {
    // Risolve l'ambiguità per il codice sorgente principale
    getByName("compileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
    // AGGIUNGI QUESTO: Risolve l'ambiguità anche per il codice di test
    getByName("testCompileProtoPath") {
        attributes {
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "jvm")
        }
    }
}

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
    // IMPORTANTE: Dice a Gradle dove trovare il file .proto
    sourceSets {
        main {
            proto {
                srcDir("../GestioneOpere/src/main/proto")
            }
        }
    }
}

tasks.named<War>("war") {
    archiveFileName.set("gateway.war")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Escludi i persistence.xml dai JAR delle dipendenze
    exclude { fileTreeElement ->
        fileTreeElement.path.contains("META-INF/persistence.xml") &&
                !fileTreeElement.file.absolutePath.contains("Gateway/src")
    }
}
tasks.test {
    useJUnitPlatform()
}
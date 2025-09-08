plugins {
    id("org.gradle.war")
    id("java")
    id("com.google.protobuf") version "0.9.4" // AGGIUNTO: Plugin protobuf
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

// AGGIUNTO: Determina il classificatore per il sistema operativo corrente
val osClassifier = when {
    System.getProperty("os.name").toLowerCase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").toLowerCase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-java:4.27.1")
    // Per Kotlin, se usi anche coroutines:
    // implementation("io.grpc:grpc-kotlin-stub:1.4.1")
}

// Blocco di configurazione per il plugin Protobuf
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

// Questo aggiunge le directory generate dal plugin Protobuf alle sorgenti Java
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
            exclude("*/.proto")
        }
    }
}
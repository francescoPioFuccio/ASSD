plugins {
    id("org.gradle.war")
    id("java")
    id("com.google.protobuf") version "0.9.4" // Plugin protobuf
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Forza la compatibilità anche sui task di compilazione
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

// Determina il classificatore per protoc in base al sistema operativo
val osClassifier = when {
    System.getProperty("os.name").lowercase().contains("windows") -> "windows-x86_64"
    System.getProperty("os.name").lowercase().contains("mac") -> "osx-x86_64"
    else -> "linux-x86_64"
}

dependencies {
    implementation("io.grpc:grpc-netty-shaded:1.65.1")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-java:4.27.1")
    // Se usi anche Kotlin coroutines:
    // implementation("io.grpc:grpc-kotlin-stub:1.4.1")
}

// Configurazione del plugin Protobuf
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.27.1:$osClassifier"
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

// Aggiunge le sorgenti generate dal plugin Protobuf
sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/grpc"
            )
        }
        proto {
            srcDir("src/main/proto")
        }
        resources {
            // Escludi i .proto dalle risorse per evitare duplicati
            exclude("**/*.proto")
        }
    }
}

// 👉 Gestione duplicati in processResources
tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

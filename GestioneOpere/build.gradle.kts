plugins {
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"


dependencies {
    implementation(project(":UserDataModule2"))


    implementation("org.json:json:20240303")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("org.glassfish.jersey.core:jersey-server:3.1.3")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.3")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:3.1.3")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}
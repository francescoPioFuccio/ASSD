plugins {
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"


dependencies {
    implementation(project(":UserDataModule2"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}
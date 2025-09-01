plugins {
    id("java")
}

group = "org.example"
version = "unspecified"


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")



    // ✅ JSON - org.json
    implementation("org.json:json:20240303")

    // ✅ Gson - per serializzare oggetti in JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ (Opzionale, se usi dependency injection)
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

}

tasks.test {
    useJUnitPlatform()
}
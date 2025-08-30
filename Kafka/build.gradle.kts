plugins {
    id("java")
}

group = "it.unisannio.gateway"
version = "unspecified"



dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")


    implementation("org.apache.kafka:kafka-clients:3.7.0")
}

tasks.test {
    useJUnitPlatform()
}
plugins {
    id("java")
}

group = "net.bzethmayr.prismo"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.1")
    implementation("io.github.bzethmayr.fungu:fungu:1.5.7")

    testImplementation("io.github.bzethmayr.fungu:fungu-test:1.2.12")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
}

tasks.test {
    useJUnitPlatform()
}
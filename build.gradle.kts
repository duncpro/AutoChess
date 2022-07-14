plugins {
    kotlin("jvm") version "1.6.10"
}

group = "com.duncpro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    implementation("org.fusesource.jansi:jansi:2.4.0")

}

tasks.test {
    useJUnitPlatform()
}
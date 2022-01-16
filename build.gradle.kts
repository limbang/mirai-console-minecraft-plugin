plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.8.3"
}

group = "top.limbang"
version = "1.1.6"

repositories {
    maven("https://maven.fanua.top:8015/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("top.fanua.doctor:doctor-client:1.3.5")
    testImplementation("org.slf4j:slf4j-simple:1.7.32")
}

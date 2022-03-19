plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.10.0"
}

group = "top.limbang"
version = "1.1.8"

repositories {
    maven("https://maven.fanua.top:8015/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation("top.fanua.doctor:doctor-client:1.3.7")
    implementation("top.fanua.doctor:doctor-plugin-forge-fix:1.3.7")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
}

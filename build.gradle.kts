plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.14.0"
}

group = "top.limbang"
version = "1.1.13"


repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("top.fanua.doctor:doctor-client:1.3.13")
    implementation("top.fanua.doctor:doctor-plugin-forge-fix:1.3.13")
    compileOnly("top.limbang:mirai-plugin-general-interface:1.0.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.5")
    "shadowLink"("top.fanua.doctor:doctor-client")
    "shadowLink"("top.fanua.doctor:doctor-plugin-forge-fix")
}

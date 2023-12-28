plugins {
    val kotlinVersion = "1.9.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "top.limbang"
version = "1.2.1"


repositories {
    mavenCentral()
}

dependencies {
    compileOnly("top.limbang:mirai-plugin-general-interface:1.0.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.5")
}

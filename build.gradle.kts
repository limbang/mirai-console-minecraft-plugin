plugins {
    val kotlinVersion = "1.7.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.0-RC"
}

group = "top.limbang"
version = "1.1.10"

mirai {
    coreVersion = "2.11.1"
    consoleVersion = "2.11.1"
}

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation("top.fanua.doctor:doctor-client:1.3.10")
    implementation("top.fanua.doctor:doctor-plugin-forge-fix:1.3.10")
    compileOnly("top.limbang:mirai-plugin-general-interface:1.0.1")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
    "shadowLink"("top.fanua.doctor:doctor-client")
    "shadowLink"("top.fanua.doctor:doctor-plugin-forge-fix")
}

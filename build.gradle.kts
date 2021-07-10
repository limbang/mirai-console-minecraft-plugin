plugins {
    val kotlinVersion = "1.4.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.6.4"
}

group = "top.limbang"
version = "1.1.0"

repositories {

    maven("http://web.blackyin.top:8015/repository/maven-public/")
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    implementation("top.limbang.doctor:doctor-client:1.2.6")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

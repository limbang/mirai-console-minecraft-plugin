plugins {
    val kotlinVersion = "1.4.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.6.4"
}

group = "top.limbang"
version = "1.0.0"

repositories {

    maven("http://web.blackyin.top:8015/repository/maven-public/")
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.12")
    implementation("top.limbang:doctor-all:+")
}

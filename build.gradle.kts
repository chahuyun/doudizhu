plugins {
    val kotlinVersion = "1.9.23"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"

    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "cn.chahuyun"
version = "1.0.0-dev"

repositories {
    maven("https://nexus.jsdu.cn/repository/maven-public/")
//    mavenCentral()
}

dependencies {

    //依赖
    compileOnly("cn.chahuyun:HuYanEconomy:1.7.7")
    compileOnly("cn.chahuyun:HuYanAuthorize:1.2.0")

    //使用库
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("cn.hutool:hutool-all:5.8.30")

    implementation("cn.chahuyun:hibernate-plus:1.0.16")

}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

buildConfig {
    className("BuildConstants")
    packageName("cn.chahuyun.teafox.game")
    useKotlinOutput()
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField(
        "java.time.Instant",
        "BUILD_TIME",
        "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)"
    )
}
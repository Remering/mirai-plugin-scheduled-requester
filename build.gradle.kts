plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "cn.remering"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

mirai {
    jvmTarget = JavaVersion.VERSION_1_8
}

dependencies {
    val quartzVersion = "2.3.2"
    val okHttpVersion = "4.11.0"
    implementation("org.quartz-scheduler:quartz:$quartzVersion")
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
}

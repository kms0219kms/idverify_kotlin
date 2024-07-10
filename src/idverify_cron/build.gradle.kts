plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    implementation("io.viascom.nanoid:nanoid:1.0.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    implementation("org.mongodb:mongodb-driver-sync:5.1.1")
    implementation("redis.clients:jedis:5.1.3")

    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

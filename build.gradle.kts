plugins {
    id("java")
    application
    kotlin("jvm") version "1.6.10"
}

group = "pw.chew"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("https://m2.chew.pro/snapshots/")
        content {
            includeGroup("pw.chew")
        }
    }

    mavenCentral()
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.0.0-beta.15")
    implementation("pw.chew", "jda-chewtils", "2.0-SNAPSHOT")
    implementation("org.json", "json", "20230227")
    implementation("ch.qos.logback", "logback-classic", "1.4.5")
    implementation("mysql", "mysql-connector-java", "8.0.28")
    implementation("org.mapdb", "mapdb", "3.0.9")
    implementation("org.hibernate", "hibernate-core", "5.6.5.Final")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.apiVersion = "1.6"
}

application {
    mainClass.set("pw.chew.mlb.MLBBot")
}

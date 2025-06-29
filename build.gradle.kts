plugins {
    id("java")
    application
    kotlin("jvm") version "1.9.25"
}

group = "pw.chew"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("https://m2.chew.pro/releases/")
        content {
            includeGroup("pw.chew")
        }
    }

    mavenCentral()
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.6.1")
    implementation("pw.chew", "jda-chewtils", "2.1")
    implementation("org.json", "json", "20250107")
    implementation("ch.qos.logback", "logback-classic", "1.5.18")
    implementation("mysql", "mysql-connector-java", "8.0.33")
    implementation("org.mapdb", "mapdb", "3.1.0")
    implementation("org.hibernate", "hibernate-core", "5.6.15.Final")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.2")
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

plugins {
    id("java")
    application
    kotlin("jvm") version "1.9.24"
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
    implementation("net.dv8tion", "JDA", "5.0.1")
    implementation("pw.chew", "jda-chewtils", "2.0")
    implementation("org.json", "json", "20240303")
    implementation("ch.qos.logback", "logback-classic", "1.5.6")
    implementation("mysql", "mysql-connector-java", "8.0.33")
    implementation("org.mapdb", "mapdb", "3.1.0")
    implementation("org.hibernate", "hibernate-core", "5.6.15.Final")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
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

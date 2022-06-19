plugins {
    id("java")
    application
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
    implementation("net.dv8tion", "JDA", "5.0.0-alpha.12")
    implementation("pw.chew", "jda-chewtils", "2.0-SNAPSHOT")
    implementation("org.json", "json", "20211205")
    implementation("ch.qos.logback", "logback-classic", "1.2.11")
    implementation("org.mapdb", "mapdb", "3.0.8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

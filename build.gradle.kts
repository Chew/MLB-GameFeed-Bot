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
    implementation("net.dv8tion", "JDA", "5.0.0-beta.10")
    implementation("pw.chew", "jda-chewtils", "2.0-SNAPSHOT")
    implementation("org.json", "json", "20230227")
    implementation("ch.qos.logback", "logback-classic", "1.4.5")
    implementation("org.mapdb", "mapdb", "3.0.9")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

application {
    mainClass.set("pw.chew.mlb.MLBBot")
}

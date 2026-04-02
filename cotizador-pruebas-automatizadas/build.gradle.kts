plugins {
    id("java")
    id("net.serenity-bdd.serenity-gradle-plugin") version "4.2.34"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val serenityVersion = "4.2.34"
    val lombokVersion = "1.18.38"

    implementation("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("net.serenity-bdd:serenity-core:$serenityVersion")
    implementation("net.serenity-bdd:serenity-junit:$serenityVersion")
    implementation("net.serenity-bdd:serenity-cucumber:$serenityVersion")
    implementation("net.serenity-bdd:serenity-screenplay:$serenityVersion")
    implementation("net.serenity-bdd:serenity-screenplay-webdriver:$serenityVersion")
    implementation("io.cucumber:cucumber-java:7.18.1")
    implementation("org.hamcrest:hamcrest:2.2")

    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testImplementation("net.serenity-bdd:serenity-core:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-junit:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-cucumber:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-screenplay:$serenityVersion")
    testImplementation("net.serenity-bdd:serenity-screenplay-webdriver:$serenityVersion")
    testImplementation("io.cucumber:cucumber-java:7.18.1")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

tasks.test {
    useJUnit()
    systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags", ""))
    finalizedBy("aggregate")
}

tasks.register<Test>("quoteE2E") {
    description = "Runs QuoteE2ERunner and generates updated Serenity reports"
    group = "verification"
    useJUnit()
    include("**/QuoteE2ERunner.class")
    systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags", ""))
    finalizedBy("aggregate")
}
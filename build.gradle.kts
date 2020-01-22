// build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

plugins {
    java
    kotlin("jvm") version "1.3.50"
}

group = ""
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testCompile("junit", "junit", "4.12")
    compile("org.seleniumhq.selenium", "selenium-java", "3.+")
    compile("mysql", "mysql-connector-java", "5.1.13")
    compile("com.sun.mail", "javax.mail", "1.6.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val fatJarScrape = task("fatJarScrape", type = Jar::class) {
    baseName = "${project.name}Scrape-fat"
    manifest {
        attributes["Implementation-Title"] = "ScrapeKt"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "ScrapeKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

val fatJarEmails = task("fatJarEmails", type = Jar::class) {
    baseName = "${project.name}Emails-fat"
    manifest {
        attributes["Implementation-Title"] = "EmailsKt"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "EmailsKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJarScrape)
        dependsOn(fatJarEmails)
    }
}
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.2.70"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

group = "juuxel.imagefilterer"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.26.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:0.26.1")
    implementation("com.github.Juuxel:basiks:97e513d4bf")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "juuxel.imagefilterer.ImageFilterer"
    }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

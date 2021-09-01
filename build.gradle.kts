// More about the setup here: https://github.com/DevSrSouza/KotlinBukkitAPI/wiki/Getting-Started

import org.apache.tools.ant.filters.ReplaceTokens

//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//
//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions.useIR = true

plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
//    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "yhw.panda"
version = "1.0"

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

//plugins { id("java") }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    compileOnly("org.spigotmc:spigot:1.16.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.50")
//    compileOnly("org.spigotmc:spigot:1.16.4-R0.1-SNAPSHOT")

//    val transitive = Action<ExternalModuleDependency> { isTransitive = false }
}

//bukkit {
//    main = "yhw.panda.pandamoniumcrates.PandamoniumCrates"
////    depend = listOf("KotlinBukkitAPI")
//    description = ""
//    author = ""
//    website = ""
//}

//processResources {
//    from(sourceSets.main.resources.srcDirs) {
//        filter ReplaceTokens, tokens: [version: version]
//    }
//}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi"
    }
    shadowJar {
        classifier = null
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(120, "seconds")
}
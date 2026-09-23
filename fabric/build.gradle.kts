plugins {
    id("fabric-loom") version "1.17.12"
}

base {
    archivesName.set("HeadDrop")
}

version = "1.0.0"
group = "com.example"

loom {
    enableModProvidedJavadoc.set(false)
    decompilers { clear() }
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    targetNamespace.set("named")
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:26.2")
    mappings("net.fabricmc:yarn:1.21.11+build.6:v2")
    modImplementation("net.fabricmc:fabric-loader:0.19.5")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.159.0+26.2")
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

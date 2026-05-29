plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

architectury {
    fabric()
}

configurations {
    create("common")
    create("shadowCommon")

    named("compileClasspath") {
        extendsFrom(named("common").get())
    }
    named("runtimeClasspath") {
        extendsFrom(named("common").get())
    }
    named("developmentFabric") {
        extendsFrom(named("common").get())
    }
}

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.layered {
        it.mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    })

    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.102.0+1.21.1")
    modImplementation("dev.architectury:architectury-fabric:13.0.6")

    "common"(project(":common") {
        configuration = "namedElements"
        isTransitive = false
    })
    "shadowCommon"(project(":common") {
        configuration = "transformProductionFabric"
        isTransitive = false
    })
}

tasks.shadowJar {
    configurations = listOf(project.configurations.named("shadowCommon").get())
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.build {
    dependsOn(tasks.remapJar)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

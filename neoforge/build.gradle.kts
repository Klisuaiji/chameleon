plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

architectury {
    neoforge()
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
    named("developmentNeoForge") {
        extendsFrom(named("common").get())
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.layered {
        it.officialMojangMappings()
    })

    neoForge("net.neoforged:neoforge:21.1.85")
    modImplementation("dev.architectury:architectury-neoforge:13.0.6")

    "common"(project(":common") {
        configuration = "namedElements"
        isTransitive = false
    })
    "shadowCommon"(project(":common") {
        configuration = "transformProductionNeoForge"
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
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("version" to project.version))
    }
}

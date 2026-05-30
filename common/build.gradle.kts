plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

architectury {
    common("fabric", "neoforge")
}

configurations {
    create("shade")
    named("implementation") {
        extendsFrom(named("shade").get())
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    loom {
        mappings {
            officialMojangMappings()
        }
    }
    
    modImplementation("dev.architectury:architectury:13.0.6")
    "shade"("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.named("shade").get())
    archiveClassifier.set("")
    relocate("com.moandjiezana.toml", "com.kulisaiji.chameleon.shaded.com.moandjiezana.toml")
}

tasks.jar {
    dependsOn(tasks.shadowJar)
    enabled = false
}

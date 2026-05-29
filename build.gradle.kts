plugins {
    id("dev.architectury.loom") version "1.10-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "8.3.6" apply false
}

allprojects {
    group = "com.kulisaiji"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    base {
        archivesName.set("chameleon-${project.name}")
    }

    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
    }

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = base.archivesName.get()
                from(components["java"])
            }
        }
    }
}

project(":common") {
    apply(plugin = "com.gradleup.shadow")
    
    configurations {
        create("shade")
        named("implementation") {
            extendsFrom(named("shade").get())
        }
    }

    dependencies {
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
}

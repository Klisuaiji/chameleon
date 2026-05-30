import org.gradle.jvm.tasks.Jar

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
    apply(plugin = "base")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    extensions.configure<org.gradle.api.plugins.BasePluginExtension> {
        archivesName.set("chameleon-${project.name}")
    }

    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = extensions.getByType<org.gradle.api.plugins.BasePluginExtension>().archivesName.get()
                from(components["java"])
            }
        }
    }
}

project(":common") {
    apply(plugin = "com.gradleup.shadow")
    
    val shade = configurations.create("shade")
    configurations.named("implementation") {
        extendsFrom(shade)
    }

    dependencies {
        add("shade", "com.moandjiezana.toml:toml4j:0.7.2")
        add("implementation", "com.google.code.gson:gson:2.10.1")
    }

    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        configurations = listOf(shade)
        archiveClassifier.set("")
        relocate("com.moandjiezana.toml", "com.kulisaiji.chameleon.shaded.com.moandjiezana.toml")
    }

    tasks.named<Jar>("jar") {
        dependsOn(tasks.named("shadowJar"))
        enabled = false
    }
}

import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

repositories {
    mavenCentral()
    maven { url = URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val pluginVersion = "0.7.0"

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
    }
    publishing {
        // export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='YOUR_TOKEN' to publish the plugin
        // token can be retrieved from https://plugins.jetbrains.com/author/me/tokens
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("243.*")
        version = pluginVersion
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.1.7")
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
        instrumentationTools()
    }
}
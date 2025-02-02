import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
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

val pluginVersion = "0.8.1"

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
    }
    publishing {
        // export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='YOUR_TOKEN' to publish the plugin
        // token can be retrieved from https://plugins.jetbrains.com/author/me/tokens
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }

    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "232"
                untilBuild = "243.*"
            }
        }
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
        pluginVerifier()
    }
}
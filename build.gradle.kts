import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform") version "2.0.1"
    id("dev.bmac.intellij.plugin-uploader") version "1.3.3"
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
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        version = pluginVersion
    }
    publishPlugin {
        token.set(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.1.7")
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")
        instrumentationTools()
    }
}
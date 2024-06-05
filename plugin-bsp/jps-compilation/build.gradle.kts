
plugins {
    id("intellijbsp.kotlin-conventions")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(Platform.version)

        plugins(Platform.plugins)
        bundledPlugins(Platform.bundledPlugins)
        instrumentationTools()
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}
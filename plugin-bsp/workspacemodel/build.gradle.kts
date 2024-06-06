plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.jetbrains.intellij.platform.base")
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
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

kotlin {
  sourceSets.main {
    kotlin.srcDirs("src/main/kotlin", "src/main/gen")
  }
}
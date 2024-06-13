plugins {
    id("intellijbsp.kotlin-conventions")
}

dependencies {
    testImplementation(libs.junitJupiter)
    testImplementation(libs.ideStarterSquashed)
    testImplementation(libs.ideMetricsCollector)
    testImplementation(libs.ideMetricsCollectorStarter)
    testImplementation(libs.ideStarterJunit5)
    testImplementation(libs.jackson)
    testImplementation(libs.jacksonKotlin)
    testImplementation(libs.kodeIn)
    testImplementation(libs.apacheHttpClient)
}

tasks {
    processResources {
        dependsOn(":buildPlugin")
        from("$rootDir/build/distributions")
    }
    test {
        useJUnitPlatform()
        systemProperty("bsp.benchmark.plugin.zip", "intellij-bsp-${Plugin.version}.zip")
        systemProperty("bsp.benchmark.platform.version", Platform.version)
        systemProperties(System.getProperties() as Map<String, *>)
    }
}

repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

package org.jetbrains.plugins.bsp.utils

import com.intellij.execution.RunConfigurationProducerService
import com.intellij.openapi.project.Project

internal object RunConfigurationProducersDisabler {
  operator fun invoke(project: Project): Boolean =
    RunConfigurationProducerService.getInstance(project).state.ignoredProducers.addAll(producersNames)

  private val producersNames = listOf(
    "com.android.tools.idea.run.AndroidConfigurationProducer",
    "com.android.tools.idea.run.configuration.AndroidComplicationRunConfigurationProducer",
    "com.android.tools.idea.run.configuration.AndroidTileRunConfigurationProducer",
    "com.android.tools.idea.run.configuration.AndroidWatchFaceRunConfigurationProducer",
    "com.android.tools.idea.testartifacts.instrumented.AndroidTestConfigurationProducer",
    "com.android.tools.idea.testartifacts.junit.TestClassAndroidConfigurationProducer",
    "com.android.tools.idea.testartifacts.junit.TestDirectoryAndroidConfigurationProducer",
    "com.android.tools.idea.testartifacts.junit.TestMethodAndroidConfigurationProducer",
    "com.android.tools.idea.testartifacts.junit.TestPackageAndroidConfigurationProducer",
    "com.android.tools.idea.testartifacts.junit.TestPatternConfigurationProducer",
    "com.intellij.execution.application.ApplicationConfigurationProducer",
    "com.intellij.execution.junit.AbstractAllInDirectoryConfigurationProducer",
    "com.intellij.execution.junit.AllInDirectoryConfigurationProducer",
    "com.intellij.execution.junit.AllInPackageConfigurationProducer",
    "com.intellij.execution.junit.PatternConfigurationProducer",
    "com.intellij.execution.junit.TestClassConfigurationProducer",
    "com.intellij.execution.junit.TestInClassConfigurationProducer",
    "com.intellij.execution.junit.UniqueIdConfigurationProducer",
    "com.intellij.execution.junit.testDiscovery.JUnitTestDiscoveryConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradle.native.KotlinNativeRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.run.KotlinJvmTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.run.KotlinJvmTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.run.KotlinMultiplatformJvmTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.run.KotlinMultiplatformJvmTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.KotlinMultiplatformAllInDirectoryConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.KotlinMultiplatformAllInPackageConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.common.KotlinMultiplatformCommonTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.common." +
      "KotlinMultiplatformCommonTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.js.KotlinMultiplatformJsTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.js.KotlinMultiplatformJsTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.native.KotlinMultiplatformNativeTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.gradleJava.testing.native." +
      "KotlinMultiplatformNativeTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.junit.KotlinJUnitRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.junit.KotlinPatternConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinJvmTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinJvmTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinMultiplatformJvmTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinMultiplatformJvmTestMethodGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinPatternConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinTestClassGradleConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinTestMethodGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.GradleGroovyScriptRunConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.test.runner.AllInDirectoryGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.test.runner.AllInPackageGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.test.runner.PatternGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer",
    "org.jetbrains.plugins.gradle.service.execution.GradleRuntimeConfigurationProducer",
  )
}

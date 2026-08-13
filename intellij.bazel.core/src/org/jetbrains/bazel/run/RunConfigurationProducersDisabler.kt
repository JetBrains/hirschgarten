package org.jetbrains.bazel.run

import com.intellij.execution.RunConfigurationProducerSuppressor
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject

/**
 * Disables [RunConfigurationProducer]s that are already supported by our plugin via [org.jetbrains.bazel.run.config.BazelRunConfiguration].
 * Those producers are attempting, e.g., to launch a test via calling `python` or `go` directly (skipping Bazel entirely).
 * While this may work for very simple projects without external deps/codegen, Bazel should be the only available option to avoid confusion.
 * We could instead disable all producers except several "known" ones, but that would disable valid cases, e.g., running `.sh`/`.bat` files.
 */
internal class BazelRunConfigurationProducerSuppressor : RunConfigurationProducerSuppressor {
  override fun shouldSuppress(producer: RunConfigurationProducer<*>, project: Project): Boolean =
    project.isBazelProject &&
    generateSequence<Class<*>>(producer::class.java) { clazz -> clazz.superclass }
      .any { clazz -> clazz.name in producerNames }

  private val producerNames = setOf(
    "com.android.tools.idea.run.configuration.AndroidWearRunConfigurationProducer",
    "com.intellij.execution.junit.JavaRunConfigurationProducerBase",
    "org.jetbrains.kotlin.idea.gradleCodeInsightCommon.native.KotlinNativeRunConfigurationProducer",
    "org.jetbrains.plugins.gradle.execution.GradleRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.junit.KotlinJUnitRunConfigurationProducer",
    "org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer",
    "com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer",
    "com.jetbrains.python.run.PythonRunConfigurationProducer",
    "com.jetbrains.python.testing.AbstractPythonTestConfigurationProducer",
    "com.jetbrains.python.testing.tox.PyToxConfigurationProducer",
    "com.goide.execution.GoRunConfigurationProducerBase",
  )
}

package org.jetbrains.bazel.clion

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcRunConfigurationProducerTest {

  private val project by clionBazelProjectFixture("clion/simple") {
    addBuildFlags("--extra_toolchains=//toolchain:toolchain")
  }

  @Test
  fun testEntryPointRunsItsTarget(): Unit = timeoutRunBlocking {
    val configuration = project.configurationAt("main/main.cc", anchor = "main(").assertNotNull()

    assertThat(configuration.targets).containsExactly(Label.parse("//main:main"))
  }

  @Test
  fun testElementOutsideEntryPointRunsNothing(): Unit = timeoutRunBlocking {
    assertThat(project.configurationAt("main/main.cc", anchor = "stdio.h")).isNull()
  }

  @Test
  fun testNonExecutableTargetRunsNothing(): Unit = timeoutRunBlocking {
    // `//lib:lib` is a cc_library, so no gutter runs it, whatever the element is.
    assertThat(project.configurationAt("lib/lib.cc", anchor = "message(")).isNull()
  }

  private suspend fun Project.configurationAt(relativePath: String, anchor: String): BazelRunConfiguration? {
    val file = rootDir.findFileByRelativePath(relativePath).assertNotNull()

    return smartReadAction(this) {
      val psiFile = PsiManager.getInstance(this).findFile(file).assertNotNull()
      val offset = psiFile.text.indexOf(anchor)
      check(offset >= 0) { "$relativePath holds no anchor '$anchor'" }
      val element = psiFile.findElementAt(offset).assertNotNull()

      ConfigurationContext.createEmptyContextForLocation(PsiLocation.fromPsiElement(element))
        .configurationsFromContext
        .orEmpty()
        .map { it.configuration }
        .filterIsInstance<BazelRunConfiguration>()
        .singleOrNull()
    }
  }
}

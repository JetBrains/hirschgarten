package org.jetbrains.bazel.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.TestActionEvent
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.pythonTarget
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.testLocation
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.StringsCollection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

@BazelTestApplication
internal class BazelRunConfigurationProducerSuppressorTest {
  private val myFixture by bazelCodeInsightFixture()
  private val testBazel get() = myFixture.testBazel

  private val project get() = myFixture.project

  @Test
  fun `should show standard Run context action with Bazel configuration for Python main file`() {
    val psiFile = configurePhysicalPythonFile(
      "main.py",
      """
      <caret>if __name__ == '__main__':
          print('Hello')
      """.trimIndent(),
    )
    addPyBinaryTarget(label = LABEL, mainFile = psiFile)

    val element = runReadActionBlocking { PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset) }

    val context = runReadActionBlocking { ConfigurationContext(element) }
    val configuration = assertSingleBazelConfiguration(context)
    assertEquals(listOf(LABEL), configuration.targets())
    assertFalse(configuration.isTestHandler())
    assertRunContextActionAvailable(element)
  }

  @Test
  fun `should show standard Run context action with Bazel configuration for Python test function`() {
    val psiFile = configurePhysicalPythonFile(
      "test_sample.py",
      """
      def <caret>test_passes():
          assert True
      """.trimIndent(),
    )
    addPyTestTarget(label = LABEL, source = psiFile)

    val element = runReadActionBlocking { PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset) }

    val context = runReadActionBlocking { ConfigurationContext(element) }
    val configuration = assertSingleBazelConfiguration(context)
    assertEquals(listOf(LABEL), configuration.targets())
    assertTrue(configuration.isTestHandler())
    assertEquals("\"test_sample.py::test_passes\"", configuration.programArguments())
    assertRunContextActionAvailable(element)
  }

  @Test
  fun `should hide standard Run context action for Python files without Bazel runnable target`() {
    val psiFile = configurePhysicalPythonFile(
      "main.py",
      """
      <caret>if __name__ == '__main__':
          print('Hello')
      """.trimIndent(),
    )
    val element = runReadActionBlocking { PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset) }

    val context = runReadActionBlocking { ConfigurationContext(element) }
    assertNull(runReadActionBlocking { context.configuration })
    assertTrue(runReadActionBlocking { context.configurationsFromContext.orEmpty() }.isEmpty())
    assertRunContextActionHidden(element)
  }

  @Test
  fun `should hide standard Run context action for Python files with ambiguous Bazel targets`() {
    val psiFile = configurePhysicalPythonFile(
      "main.py",
      """
      <caret>if __name__ == '__main__':
          print('Hello')
      """.trimIndent(),
    )
    addPyBinaryTargets(
      pyBinaryTarget(label = LABEL, mainFile = psiFile),
      pyBinaryTarget(label = SECOND_LABEL, mainFile = psiFile),
    )

    val element = runReadActionBlocking { PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset) }

    val context = runReadActionBlocking { ConfigurationContext(element) }
    runReadActionBlocking { context.configurationsFromContext }?.forEach {
      it.configuration.shouldBeInstanceOf<BazelRunConfiguration>()
    }
  }

  private fun assertSingleBazelConfiguration(context: ConfigurationContext): RunConfiguration {
    val configurations = runReadActionBlocking { context.configurationsFromContext.orEmpty() }
    assertEquals(1, configurations.size)
    val configuration = configurations.single().configurationSettings.configuration
    assertEquals("org.jetbrains.bazel.run.config.BazelRunConfiguration", configuration.javaClass.name)
    return configuration
  }

  private fun configurePhysicalPythonFile(fileName: String, text: String): PsiFile {
    myFixture.configureFromExistingVirtualFile(testBazel.writeFile(fileName, text))
    return myFixture.file
  }

  private fun assertRunContextActionAvailable(element: PsiElement) {
    val action = RunContextAction(DefaultRunExecutor.getRunExecutorInstance())
    val event = runReadActionBlocking { createRunContextActionEvent(action, element).also { action.update(it) } }
    assertTrue(event.presentation.isVisible)
    assertTrue(event.presentation.isEnabled)
  }

  private fun assertRunContextActionHidden(element: PsiElement) {
    val action = RunContextAction(DefaultRunExecutor.getRunExecutorInstance())
    val event = runReadActionBlocking { createRunContextActionEvent(action, element).also { action.update(it) } }
    assertFalse(event.presentation.isVisible)
    assertFalse(event.presentation.isEnabled)
  }

  private fun createRunContextActionEvent(action: RunContextAction, element: PsiElement): AnActionEvent =
    TestActionEvent.createTestEvent(
      action,
      SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(PlatformCoreDataKeys.PSI_ELEMENT_ARRAY, arrayOf(element))
        .build(),
    )

  private fun addPyBinaryTarget(label: Label, mainFile: PsiFile) {
    addPyBinaryTargets(pyBinaryTarget(label, mainFile))
  }

  private fun addPyBinaryTargets(vararg targets: BuildTarget) {
    testBazel.sync { targets(*targets) }
  }

  private fun addPyTestTarget(label: Label, source: PsiFile) {
    testBazel.sync { targets(pyTestTarget(label, source)) }
  }

  private fun pyBinaryTarget(label: Label, mainFile: PsiFile): TestBuildTarget =
    pythonSourceTarget(label, "py_binary", RuleType.BINARY, source = mainFile, mainFile = mainFile)

  private fun pyTestTarget(label: Label, source: PsiFile): TestBuildTarget =
    pythonSourceTarget(label, "py_test", RuleType.TEST, source = source, mainFile = null)

  private fun pythonSourceTarget(
    label: Label,
    ruleKind: String,
    ruleType: RuleType,
    source: PsiFile,
    mainFile: PsiFile?,
  ): TestBuildTarget =
    pythonTarget(label, ruleKind, ruleType) {
      srcs(source.name)
      data(
        PythonBuildTarget(
          version = "3.8",
          interpreter = testLocation(Path.of(myFixture.tempDirPath, "python3")),
          StringsCollection.EMPTY,
          OutputLocationCollection.EMPTY,
          OutputLocationCollection.EMPTY,
          mainFile = mainFile?.virtualFile?.toNioPath()?.let(::testLocation),
          mainModule = null,
        ),
      )
    }

  @Suppress("UNCHECKED_CAST")
  private fun RunConfiguration.targets(): List<Label> =
    javaClass.getMethod("getTargets").invoke(this) as List<Label>

  private fun RunConfiguration.isTestHandler(): Boolean {
    val handler = handler() ?: return false
    return handler.javaClass.getMethod("isTestHandler").invoke(handler) as Boolean
  }

  private fun RunConfiguration.programArguments(): String? {
    val handler = handler() ?: return null
    val state = handler.javaClass.getMethod("getState").invoke(handler)
    return state.javaClass.getMethod("getProgramArguments").invoke(state) as? String
  }

  private fun RunConfiguration.handler(): Any? =
    javaClass.getMethod("getHandler").invoke(this)

  companion object {
    private val LABEL = Label.parse("//foo:bar")
    private val SECOND_LABEL = Label.parse("//foo:baz")
  }
}

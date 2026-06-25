package org.jetbrains.bazel.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunContextAction
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.rules.TempDirectory
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
internal class RunConfigurationProducersDisablerTest : BazelBasePlatformTestCase() {
  @Rule
  @JvmField
  val tempDirectory = TempDirectory()

  @Test
  fun `should show standard Run context action with Bazel configuration for Python main file`() {
    val psiFile = configurePhysicalPythonFile(
      "main.py",
      """
      <caret>if __name__ == '__main__':
          print('Hello')
      """.trimIndent(),
    )
    project.addPyBinaryTarget(label = LABEL, mainFile = psiFile)
    project.addFileToTargets(psiFile, LABEL)

    val element = PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset)

    val context = ConfigurationContext(element)
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
    project.addPyTestTarget(label = LABEL)
    project.addFileToTargets(psiFile, LABEL)

    val element = PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset)

    val context = ConfigurationContext(element)
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
    val element = PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset)

    val context = ConfigurationContext(element)
    assertNull(context.configuration)
    assertEmpty(context.configurationsFromContext.orEmpty())
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
    project.addPyBinaryTargets(
      pyBinaryTarget(label = LABEL, mainFile = psiFile),
      pyBinaryTarget(label = SECOND_LABEL, mainFile = psiFile),
    )
    project.addFileToTargets(psiFile, LABEL, SECOND_LABEL)

    val element = PsiUtilCore.getElementAtOffset(psiFile, myFixture.caretOffset)

    val context = ConfigurationContext(element)
    assertNull(context.configuration)
    assertEmpty(context.configurationsFromContext.orEmpty())
    assertRunContextActionHidden(element)
  }

  private fun assertSingleBazelConfiguration(context: ConfigurationContext): RunConfiguration {
    val configurations = context.configurationsFromContext.orEmpty()
    assertEquals(1, configurations.size)
    val configuration = configurations.single().configurationSettings.configuration
    assertEquals("org.jetbrains.bazel.run.config.BazelRunConfiguration", configuration.javaClass.name)
    return configuration
  }

  private fun configurePhysicalPythonFile(fileName: String, text: String): PsiFile {
    val directory = tempDirectory.newDirectoryPath()
    VfsRootAccess.allowRootAccess(testRootDisposable, directory.toRealPath().toString())

    val filePath = directory.resolve(fileName)
    Files.writeString(filePath, text)
    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath)!!
    myFixture.configureFromExistingVirtualFile(virtualFile)
    return myFixture.file
  }

  private fun assertRunContextActionAvailable(element: PsiElement) {
    val action = RunContextAction(DefaultRunExecutor.getRunExecutorInstance())
    val event = createRunContextActionEvent(action, element)
    action.update(event)
    assertTrue(event.presentation.isVisible)
    assertTrue(event.presentation.isEnabled)
  }

  private fun assertRunContextActionHidden(element: PsiElement) {
    val action = RunContextAction(DefaultRunExecutor.getRunExecutorInstance())
    val event = createRunContextActionEvent(action, element)
    action.update(event)
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

  private fun Project.addPyBinaryTarget(label: Label, mainFile: PsiFile) {
    addPyBinaryTargets(pyBinaryTarget(label, mainFile))
  }

  private fun Project.addPyBinaryTargets(vararg targets: RawBuildTarget) {
    targetUtils.setTargets(targets.toList())
  }

  private fun Project.addPyTestTarget(label: Label) {
    targetUtils.setTargets(listOf(pyTestTarget(label)))
  }

  private fun pyBinaryTarget(label: Label, mainFile: PsiFile): RawBuildTarget =
    pythonTarget(
      label = label,
      ruleKind = "py_binary",
      ruleType = RuleType.BINARY,
      mainFile = mainFile,
    )

  private fun pyTestTarget(label: Label): RawBuildTarget =
    pythonTarget(
      label = label,
      ruleKind = "py_test",
      ruleType = RuleType.TEST,
      mainFile = null,
    )

  private fun pythonTarget(
    label: Label,
    ruleKind: String,
    ruleType: RuleType,
    mainFile: PsiFile?,
  ): RawBuildTarget =
    RawBuildTarget(
      key = WorkspaceTargetKey(label = label),
      dependencies = emptyList(),
      kind =
        TargetKind(
          kind = ruleKind,
          languageClasses = setOf(PythonLanguageClass.PYTHON),
          ruleType = ruleType,
        ),
      sources = SourceFileCollection.EMPTY,
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = Path.of(myFixture.tempDirPath, "base_dir"),
      data = listOf(
        PythonBuildTarget(
          version = "3.8",
          interpreter = Path.of(myFixture.tempDirPath, "python3"),
          listOf(),
          SourceFileCollection.EMPTY,
          SourceFileCollection.EMPTY,
          mainFile = mainFile?.virtualFile?.toNioPath(),
          mainModule = null,
        ),
      ),
    )

  private fun Project.addFileToTargets(file: PsiFile, vararg targets: Label) {
    targetUtils.addFileToTargetIdEntry(file.virtualFile.toNioPath(), targets.toList())
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

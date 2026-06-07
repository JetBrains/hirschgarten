package org.jetbrains.bazel.python.gutter

import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.testing.isTestClass as isPythonTestClass
import com.jetbrains.python.testing.isTestFunction as isPythonTestFunction
import com.jetbrains.python.testing.isUnitTestCaseClass
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import javax.swing.Icon
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
class BazelPyRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean =
    containingFile is PyFile &&
    ((containingFile.isMainFileInAnyBuildTarget() && isIfNameMain()) || isTestFunctionNameIdentifier())

  override fun getRunLineMarkerIcon(element: PsiElement): Icon? =
    if (element.isTestFunctionNameIdentifier()) executeRunLineMarkerIcon else null

  override fun getExtraProgramArguments(element: PsiElement): List<String> =
    element.getTestRunnerArgument()?.let { listOf(it) }.orEmpty()

  // Check if this file is configured as a main application file for any bazel target.
  // Showing our gutter icon in other cases doesn't make sense, since clicking it would run another file.
  private fun PsiFile.isMainFileInAnyBuildTarget(): Boolean {
    val targetUtils = project.targetUtils
    val fileNioPath = virtualFile.toNioPathOrNull()
    return targetUtils.getTargetsForFile(virtualFile).any {
      val buildTarget = targetUtils.getBuildTargetForLabel(it) ?: return@any false
      val pythonBuildTargetData = extractPythonBuildTarget(buildTarget) ?: return@any false
      if (pythonBuildTargetData.hasMainFileDefined()) {
        pythonBuildTargetData.mainFile == fileNioPath
      } else if (pythonBuildTargetData.mainModule.isNullOrEmpty()) {
        // When both the main file and main module aren't defined, then py_binary and py_test rules
        // expect the file named exactly targetName + ".py".
        // See the "main" attribute documentation: https://bazel.build/reference/be/python#py_binary
        virtualFile.name == "${buildTarget.id.targetName}.py"
      } else {
        // Currently, I don't support finding the main file of a Python module.
        // After researching the public GitHub repositories, I decided that at this moment
        // it isn't worth the effort to support it.
        false
      }
    }
  }

  private fun PythonBuildTarget.hasMainFileDefined(): Boolean = mainFile != null && mainFile!!.isRegularFile()

  private fun PsiElement.isTestFunctionNameIdentifier(): Boolean =
    containingFile.hasTestTarget() && node?.elementType == PyTokenTypes.IDENTIFIER && getTestFunction() != null

  private fun PsiFile.hasTestTarget(): Boolean {
    val targetUtils = project.targetUtils
    return targetUtils.getTargetsForFile(virtualFile).any { label ->
      targetUtils.getBuildTargetForLabel(label)?.kind?.ruleType == RuleType.TEST
    }
  }

  private fun PsiElement.getTestFunction(): PyFunction? =
    parentOfType<PsiNameIdentifierOwner>()
      ?.takeIf { it.nameIdentifier == this }
      ?.let { it as? PyFunction }
      ?.takeIf { it.isTestFunction() }

  private fun PyFunction.isTestFunction(): Boolean {
    if (!isPythonTestFunction(this)) return false

    val containingClass = containingClass
    return containingClass?.isTestClass() ?: PyUtil.isTopLevel(this)
  }

  private fun PyClass.isTestClass(): Boolean =
    isPythonTestClass(this, getTypeEvalContext())

  private fun PyClass.isUnitTestClass(): Boolean =
    isUnitTestCaseClass(this, getTypeEvalContext()) || hasUnitTestCaseSuperClassExpression()

  private fun PyClass.hasUnitTestCaseSuperClassExpression(): Boolean =
    getSuperClassExpressions().any { it.text == "unittest.TestCase" || it.text == "TestCase" }

  private fun PsiElement.getTypeEvalContext(): TypeEvalContext =
    TypeEvalContext.codeAnalysis(project, containingFile)

  private fun PsiElement.getTestRunnerArgument(): String? {
    val testFunction = getTestFunction() ?: return null
    if (testFunction.isUnitTestFunction()) return testFunction.getUnitTestArgument()

    val relativeFilePath = containingFile.getPathRelativeToProjectRoot() ?: return null
    return testFunction.getPyTestArgument(relativeFilePath)
  }

  private fun PsiFile.getPathRelativeToProjectRoot(): String? {
    val filePath = virtualFile.toNioPathOrNull() ?: return null
    val projectRoot = project.rootDir.toNioPathOrNull() ?: return virtualFile.name
    if (!filePath.startsWith(projectRoot)) return virtualFile.name
    return projectRoot.relativize(filePath).invariantSeparatorsPathString
  }

  private fun PyFunction.isUnitTestFunction(): Boolean =
    containingClass?.isUnitTestClass() == true

  private fun PyFunction.getUnitTestArgument(): String? {
    val className = containingClass?.name ?: return null
    val functionName = name ?: return null
    return "$className.$functionName"
  }

  private fun PyFunction.getPyTestArgument(relativeFilePath: String): String? {
    val functionName = name ?: return null
    val className = containingClass?.name
    return if (className == null) {
      "$relativeFilePath::$functionName"
    } else {
      "$relativeFilePath::$className::$functionName"
    }
  }

  // It checks if a Psi element is a top level < if __name__ == "__main__" > python statement
  private fun PsiElement.isIfNameMain(): Boolean {
    // The built-in python gutter contributor places these only on the if keyword itself,
    // so we just add it to the same if keywork to overwrite it
    if (this.node.elementType != PyTokenTypes.IF_KEYWORD) {
      return false
    }
    var element: PsiElement = this
    while (true) {
      val ifStatement = PsiTreeUtil.getParentOfType(element, PyIfStatement::class.java) ?: break
      element = ifStatement
    }
    if (element is PyIfStatement) {
      return PyUtil.isIfNameEqualsMain(element)
    }
    return false
  }
}

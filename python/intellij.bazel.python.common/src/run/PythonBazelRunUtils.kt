package org.jetbrains.bazel.python.run

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
import com.jetbrains.python.testing.isTestClass
import com.jetbrains.python.testing.isTestFunction
import com.jetbrains.python.testing.isUnitTestCaseClass
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
object PythonBazelRunUtils {
  fun isRunLineMarkerElement(element: PsiElement): Boolean {
    val pyFile = element.containingFile as? PyFile ?: return false
    return (pyFile.getPythonBazelMainFileTargets().isNotEmpty() && element.isIfNameMain()) ||
           isTestFunctionNameIdentifier(element)
  }

  fun isTestFunctionNameIdentifier(element: PsiElement): Boolean =
    (element.containingFile as? PyFile)?.getPythonBazelTestTargets()?.isNotEmpty() == true &&
    element.node?.elementType == PyTokenTypes.IDENTIFIER &&
    element.getTestFunctionFromNameIdentifier() != null

  fun getTestRunnerArguments(element: PsiElement): List<String> =
    element.getTestFunctionFromNameIdentifier()
      ?.getTestRunnerArgument()
      ?.let { listOf(it) }
      .orEmpty()

  internal fun findPythonBazelRunContext(element: PsiElement): PythonBazelRunContext? {
    val testFunction = element.getTestFunctionFromContext()
    if (testFunction != null) {
      val target = (testFunction.containingFile as? PyFile)?.getPythonBazelTestTargets()?.singleOrNull() ?: return null
      val testRunnerArgument = testFunction.getTestRunnerArgument() ?: return null
      return PythonBazelRunContext.Test(
        target = target,
        sourceElement = testFunction,
        configurationName = target.id.toShortString(element.project),
        testExecutableArguments = listOf(testRunnerArgument),
      )
    }

    val pyFile = element as? PyFile ?: element.containingFile as? PyFile ?: return null
    val target = pyFile.getPythonBazelMainFileTargets().singleOrNull() ?: return null
    return PythonBazelRunContext.Binary(
      target = target,
      sourceElement = pyFile,
      configurationName = target.id.toShortString(element.project),
    )
  }
}

private fun PyFile.getPythonBazelMainFileTargets(): List<BuildTarget> =
  getPythonBazelTargets(RuleType.BINARY)
    .filter { isMainFileInTarget(it) }

private fun PyFile.getPythonBazelTestTargets(): List<BuildTarget> =
  getPythonBazelTargets(RuleType.TEST)

private fun PyFile.getPythonBazelTargets(ruleType: RuleType): List<BuildTarget> {
  val targetUtils = project.targetUtils
  val file = virtualFile ?: return emptyList()
  return targetUtils.getTargetsForFile(file)
    .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    .filter { target ->
      target.kind.ruleType == ruleType &&
      target.kind.languageClasses.contains(PythonLanguageClass.PYTHON)
    }
}

private fun PyFile.isMainFileInTarget(target: BuildTarget): Boolean {
  val pythonBuildTargetData = extractPythonBuildTarget(target) ?: return false
  val fileNioPath = virtualFile.toNioPathOrNull()
  return if (pythonBuildTargetData.hasMainFileDefined()) {
    pythonBuildTargetData.mainFile == fileNioPath
  } else if (pythonBuildTargetData.mainModule.isNullOrEmpty()) {
    // When both the main file and main module aren't defined, py_binary expects targetName + ".py".
    virtualFile.name == "${target.id.targetName}.py"
  } else {
    // Resolving Python modules to files is intentionally not supported here.
    false
  }
}

private fun PythonBuildTarget.hasMainFileDefined(): Boolean = mainFile != null && mainFile!!.isRegularFile()

private fun PsiElement.getTestFunctionFromContext(): PyFunction? =
  getTestFunctionFromNameIdentifier()
    ?: (this as? PyFunction)?.takeIf { it.isTestFunction() }
    ?: PsiTreeUtil.getParentOfType(this, PyFunction::class.java, false)?.takeIf { it.isTestFunction() }


private fun PsiElement.getTestFunctionFromNameIdentifier(): PyFunction? =
  parentOfType<PsiNameIdentifierOwner>()
    ?.takeIf { it.nameIdentifier == this }
    ?.let { it as? PyFunction }
    ?.takeIf { it.isTestFunction() }

private fun PyFunction.isTestFunction(): Boolean {
  if (!isTestFunction(this)) return false

  val containingClass = containingClass
  return containingClass?.isTestClass() ?: PyUtil.isTopLevel(this)
}

private fun PyClass.isTestClass(): Boolean =
  isTestClass(this, getTypeEvalContext())

private fun PyClass.isUnitTestClass(): Boolean =
  isUnitTestCaseClass(this, getTypeEvalContext()) || hasUnitTestCaseSuperClassExpression()

private fun PyClass.hasUnitTestCaseSuperClassExpression(): Boolean =
  getSuperClassExpressions().any { it.text == "unittest.TestCase" || it.text == "TestCase" }

private fun PsiElement.getTypeEvalContext(): TypeEvalContext =
  TypeEvalContext.codeAnalysis(project, containingFile)

private fun PyFunction.getTestRunnerArgument(): String? {
  if (isUnitTestFunction()) return getUnitTestArgument()

  val relativeFilePath = containingFile.getPathRelativeToProjectRoot() ?: return null
  return getPyTestArgument(relativeFilePath)
}

private fun PsiFile.getPathRelativeToProjectRoot(): String? {
  val filePath = virtualFile.toNioPathOrNull() ?: return null
  val projectRoot = project.rootDir.toNioPathOrNull() ?: return virtualFile.name
  return if (filePath.startsWith(projectRoot)) {
    projectRoot.relativize(filePath).invariantSeparatorsPathString
  } else {
    virtualFile.name
  }
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

// Checks if a PSI element is a top-level < if __name__ == "__main__" > Python statement.
private fun PsiElement.isIfNameMain(): Boolean {
  if (node?.elementType != PyTokenTypes.IF_KEYWORD) {
    return false
  }
  var element: PsiElement = this
  while (true) {
    val ifStatement = PsiTreeUtil.getParentOfType(element, PyIfStatement::class.java) ?: break
    element = ifStatement
  }
  return element is PyIfStatement && PyUtil.isIfNameEqualsMain(element)
}

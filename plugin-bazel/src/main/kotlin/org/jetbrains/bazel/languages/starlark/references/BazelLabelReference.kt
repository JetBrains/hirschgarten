package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.languages.bazel.BazelLabel
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir

private val BUILD_FILE_NAMES = sequenceOf("BUILD.bazel", "BUILD")

class BazelLabelReference(element: StarlarkStringLiteralExpression, soft: Boolean) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), soft) {
  override fun resolve(): PsiElement? {
    if (!element.project.isBspProject || isInNameArgument()) return null
    val label = BazelLabel.ofString(element.getStringContents() ?: "")
    val buildFilePsi = resolveBuildFile(label) ?: return null
    return resolveRuleTarget(buildFilePsi, label) ?: resolveFileTarget(element.project, buildFilePsi, label)
  }

  private fun isInNameArgument(): Boolean {
    val parent = element.parent ?: return false
    return parent is StarlarkNamedArgumentExpression && parent.isNameArgument()
  }

  private fun resolveBuildFile(label: BazelLabel): StarlarkFile? =
    if (label.hasPackageName) resolveExplicitPackage(label) else resolveImplicitPackage()

  private fun resolveExplicitPackage(label: BazelLabel): StarlarkFile? {
    val project = element.project
    val packageDir = findReferredPackage(project, label) ?: return null
    val buildFile = findBuildFile(packageDir) ?: return null
    return findBuildFilePsi(project, buildFile)
  }

  private fun resolveImplicitPackage(): StarlarkFile? = element.getBazelPackage()?.buildFile

  private fun resolveRuleTarget(buildFilePsi: StarlarkFile, label: BazelLabel): StarlarkCallExpression? =
    buildFilePsi.findRuleTarget(label.targetName)

  private fun resolveFileTarget(
    project: Project,
    buildFilePsi: StarlarkFile,
    label: BazelLabel,
  ): PsiFile? {
    val targetFile = buildFilePsi.parent?.virtualFile?.findFileByRelativePath(label.targetName) ?: return null
    return PsiManager.getInstance(project).findFile(targetFile)
  }

  private fun findReferredPackage(project: Project, label: BazelLabel): VirtualFile? =
    project.rootDir.findFileByRelativePath(label.packageName)

  private fun findBuildFile(packageDir: VirtualFile): VirtualFile? = BUILD_FILE_NAMES.mapNotNull { packageDir.findChild(it) }.firstOrNull()

  private fun findBuildFilePsi(project: Project, buildFile: VirtualFile): StarlarkFile? =
    PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile
}

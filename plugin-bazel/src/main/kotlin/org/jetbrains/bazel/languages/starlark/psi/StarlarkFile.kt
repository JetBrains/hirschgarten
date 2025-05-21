package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.utils.VfsUtils
import java.nio.file.Path

open class StarlarkFile(viewProvider: FileViewProvider) :
  PsiFileBase(viewProvider, StarlarkLanguage),
  StarlarkElement {
  override fun getFileType(): FileType = StarlarkFileType

  fun isBuildFile(): Boolean = this.getBazelFileType() == BazelFileType.BUILD

  fun getBazelFileType(): BazelFileType = BazelFileType.ofFileName(name)

  fun findRuleTarget(targetName: String): StarlarkCallExpression? =
    findChildrenByClass(StarlarkExpressionStatement::class.java)
      .mapNotNull { it.callExpressionOrNull() }
      .firstOrNull { it.getArgumentList()?.getNameArgumentValue() == targetName }

  fun searchInLoads(processor: Processor<StarlarkElement>): Boolean =
    findChildrenByClass(StarlarkLoadStatement::class.java).flatMap { it.getLoadedSymbolsPsi() }.all(processor::process)

  companion object {
    fun findBuildFile(packageDirectory: Path, project: Project): StarlarkFile? {
      val vf = LocalFileSystem.getInstance().findFileByPath(packageDirectory.toFile().path) ?: return null
      val buildFile = VfsUtils.getBuildFileForPackageDirectory(vf) ?: return null
      val psiFile = PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile ?: return null
      return if (psiFile.isBuildFile()) psiFile else null
    }
  }
}

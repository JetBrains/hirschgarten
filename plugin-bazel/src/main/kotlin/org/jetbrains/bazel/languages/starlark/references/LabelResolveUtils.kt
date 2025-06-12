package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import org.jetbrains.bazel.label.asBazelLabel
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.languages.starlark.repomapping.findContainingBazelRepo
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabel

/**
 * @param containingFile the file that should be used as context for resolving, e.g., relative labels
 * @param acceptOnlyFileTarget `true` if [TargetPattern.target] always refers to a file on disk (e.g., in `load` statements).
 * @see ExternalRepoResolveTest
 */
@RequiresReadLock
fun resolveTargetPattern(
  project: Project,
  targetPattern: TargetPattern,
  containingFile: VirtualFile? = null,
  acceptOnlyFileTarget: Boolean = false,
): PsiElement? {
  val buildOrSource = resolveBuildFileOrSourceFile(project, targetPattern, containingFile) ?: return null
  return when (buildOrSource) {
    is BuildFilePsi -> {
      val buildFile = buildOrSource.file
      if (!acceptOnlyFileTarget) {
        resolveBuildFileTarget(buildFile, targetPattern)?.let { return it }
      }
      resolveFileTarget(project, buildFile, targetPattern)?.let { return it }
      // Fall back to the BUILD file, as opposed to a specific target inside it.
      // The reference may still be valid, e.g., if a macro in the file creates a target with a custom name,
      // in which case we can't determine which macro actually corresponds to the target with that name.
      return buildFile.takeIf { !acceptOnlyFileTarget }
    }
    is SourceFile -> PsiManager.getInstance(project).findFile(buildOrSource.file)
  }
}

private sealed interface BuildFileOrSourceFile

private class BuildFilePsi(val file: StarlarkFile) : BuildFileOrSourceFile

private class SourceFile(val file: VirtualFile) : BuildFileOrSourceFile

private fun resolveBuildFileOrSourceFile(
  project: Project,
  targetPattern: TargetPattern,
  containingFile: VirtualFile?,
): BuildFileOrSourceFile? =
  when (targetPattern) {
    is Label ->
      findBuildFile(
        project,
        targetPattern,
        containingFile,
      )?.let { BuildFilePsi(it) }

    else -> resolveRelativePackageOrSourceFile(project, targetPattern, containingFile)
  }

private fun resolveBuildFileTarget(buildFile: StarlarkFile, label: TargetPattern): PsiElement? {
  val target = label.asBazelLabel()?.target ?: return null
  val ruleTarget = buildFile.findRuleTarget(target.targetName) ?: return null
  return ruleTarget
    .getArgumentList()
    ?.getNameArgument()
    ?.getValue()
    ?.let { return it }
}

fun findBuildFile(
  project: Project,
  label: Label,
  containingFile: VirtualFile? = null,
): StarlarkFile? {
  val packageDir = findReferredAbsolutePackage(project, containingFile, label) ?: return null
  return findBuildFilePsi(project, packageDir)
}

private fun resolveRelativePackageOrSourceFile(
  project: Project,
  targetPattern: TargetPattern,
  containingFile: VirtualFile?,
): BuildFileOrSourceFile? {
  val containingPackage = findContainingPackage(containingFile) ?: return null
  val referredPackage = containingPackage.findFileByRelativePath(targetPattern.packagePath.toString()) ?: return null
  if (referredPackage.isFile) {
    if (targetPattern.target is AmbiguousEmptyTarget) return SourceFile(referredPackage)
    return null
  }
  return findBuildFilePsi(project, referredPackage)?.let { BuildFilePsi(it) }
}

private fun findContainingPackage(directory: VirtualFile?): VirtualFile? =
  directory?.let {
    if (findBuildFile(directory) != null) directory else findContainingPackage(directory.parent)
  }

private fun resolveFileTarget(
  project: Project,
  buildFilePsi: StarlarkFile,
  label: TargetPattern,
): PsiFile? {
  // buildFilePsi may be excluded, in which case we can't get the parent PsiDirectory.
  // Therefore get virtualFile then parent and not vice-versa
  val targetFile = buildFilePsi.virtualFile?.parent?.findFileByRelativePath(label.targetName) ?: return null

  return PsiManager.getInstance(project).findFile(targetFile)
}

private fun findReferredAbsolutePackage(
  project: Project,
  containingFile: VirtualFile?,
  label: Label,
): VirtualFile? {
  val canonicalLabel = label.toCanonicalLabel(project) ?: return null
  val foundRepoRoot =
    if (canonicalLabel.repo.isMain && containingFile != null) {
      findContainingBazelRepo(containingFile.toNioPathOrNull() ?: return null)
    } else {
      project.canonicalRepoNameToPath[canonicalLabel.repoName]
    }

  val repoRoot =
    foundRepoRoot?.let {
      VirtualFileManager.getInstance().refreshAndFindFileByNioPath(foundRepoRoot)
    } ?: return null

  return repoRoot.findFileByRelativePath(label.packagePath.toString())
}

fun findBuildFile(packageDir: VirtualFile): VirtualFile? =
  Constants.BUILD_FILE_NAMES.firstNotNullOfOrNull { buildFileName ->
    packageDir.findChild(buildFileName)?.takeIf { it.isFile }
  }

private fun findBuildFilePsi(project: Project, packageDir: VirtualFile): StarlarkFile? {
  val buildFile = findBuildFile(packageDir) ?: return null
  return PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile
}

package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.targets
import org.jetbrains.bazel.workspace.apparentRepoNameToCanonicalName
import org.jetbrains.bazel.workspace.canonicalRepoNameToApparentName
import org.jetbrains.bazel.workspace.canonicalRepoNameToPath
import java.nio.file.Path

@ApiStatus.Internal
fun findContainingBazelRepo(path: VirtualFile): VirtualFile? =
  generateSequence(path) { it.parent }.firstOrNull { repoDir ->
    WORKSPACE_FILE_NAMES.any { workspaceFileName ->
      repoDir.findChild(workspaceFileName)?.isFile == true
    }
  }

@ApiStatus.Internal
fun calculateLabel(
  project: Project,
  buildFile: VirtualFile,
  targetName: String? = null,
): ResolvedLabel? {
  val targetBaseDirectory = buildFile.parent ?: return null
  val containingRepoDirectory = findContainingBazelRepo(targetBaseDirectory) ?: return null
  val containingRepoPath = Path.of(containingRepoDirectory.path)
  val containingCanonicalRepoName =
    project.canonicalRepoNameToPath.entries
      .firstOrNull { (_, repoPath) ->
        repoPath == containingRepoPath
      }?.key ?: return null

  val relativeTargetBaseDirectory = VfsUtil.getRelativePath(targetBaseDirectory, containingRepoDirectory, '/') ?: return null
  return ResolvedLabel(
    repo = Canonical.createCanonicalOrMain(containingCanonicalRepoName),
    packagePath = Package(relativeTargetBaseDirectory.split('/').filter { it.isNotEmpty() }),
    target = targetName?.let { SingleTarget(targetName) } ?: AmbiguousEmptyTarget,
  )
}

/**
 * We should show apparent labels in the UI, where possible, to avoid confusing the user with labels containing symbols such as `~` or `+`.
 * If conversion to an apparent label fails, fall back to the original label.
 */
@ApiStatus.Internal
fun Label.toApparentLabelOrThis(project: Project): Label = toApparentLabel(project) ?: this

@ApiStatus.Internal
fun Label.toApparentLabelOrThis(repoMapping: RepoMapping): Label = toApparentLabel(repoMapping) ?: this

/**
 * Converts this [Label]'s repository either to [Apparent] or to [Main].
 */
@ApiStatus.Internal
fun Label.toApparentLabel(project: Project): ResolvedLabel? {
  if (this !is ResolvedLabel) return null
  if (this.repo !is Canonical) return this
  val apparentRepoName = project.canonicalRepoNameToApparentName[this.repo.repoName] ?: return null
  return this.copy(repo = Apparent(apparentRepoName))
}

@ApiStatus.Internal
fun Label.toApparentLabel(repoMapping: RepoMapping): ResolvedLabel? {
  if (this !is ResolvedLabel) return null
  if (this.repo !is Canonical) return this
  val apparentRepoName = when (repoMapping) {
                           is BzlmodRepoMapping -> repoMapping.canonicalRepoNameToApparentName[this.repo.repoName]
                           RepoMappingDisabled -> null
                         } ?: return null
  return this.copy(repo = Apparent(apparentRepoName))
}

/**
 * Converts this [Label]'s repository either to [Canonical] or to [Main].
 */
@ApiStatus.Internal
fun Label.toCanonicalLabel(project: Project): ResolvedLabel? {
  if (this !is ResolvedLabel) return null
  val repo =
    if (repo !is Apparent) {
      repo
    }
    else {
      val canonicalRepoName = project.apparentRepoNameToCanonicalName[repo.repoName] ?: return null
      Canonical.createCanonicalOrMain(canonicalRepoName)
    }
  val target = this.singleTarget() ?: return null
  return this.copy(repo = repo, target = target)
}

@ApiStatus.Internal
fun Label.toCanonicalLabelOrThis(project: Project): ResolvedLabel? = toCanonicalLabel(project) ?: this as? ResolvedLabel

@ApiStatus.Internal
fun Label.singleTarget(): SingleTarget? {
  return when (val oldTarget = target) {
    is AmbiguousEmptyTarget -> packagePath.pathSegments.lastOrNull()?.let { SingleTarget(it) }
    is SingleTarget -> oldTarget
    else -> null
  }
}

@NlsSafe
@ApiStatus.Internal
fun Label.toShortString(project: Project): String {
  val label = this.toApparentLabelOrThis(project)
  if (label !is ResolvedLabel) return label.toString()

  val repoPart = if (label.repo !is Main) label.repo.toString() else ""
  val packagePart = label.packagePath.toString()
  val targetPart =
    when {
      label.target is AmbiguousEmptyTarget -> ""
      label.target is SingleTarget && label.target.targetName == (label.packagePath as? Package)?.name() -> ""
      else -> ":${label.target}"
    }
  return "$repoPart//$packagePart$targetPart"
}

/**
 * List of names of repositories that should be treated as internal because there are some targets that we want to be imported that
 * belong to them.
 */
val ProjectView.externalRepositoriesTreatedAsInternal: List<String>
  @ApiStatus.Internal
  get() =
    targets
      .filter { it.isIncluded() }
      .mapNotNull { excludableValue ->
        excludableValue.value
          .assumeResolved()
          .repo.repoName
          .takeIf { repoName -> repoName.isNotEmpty() }
      }.distinct()

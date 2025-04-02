package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.utils.allAncestorsSequence
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

fun findContainingBazelRepo(project: Project, path: Path): Path? {
  val repositoryPaths = project.repositoryPaths
  return path.allAncestorsSequence().firstOrNull { it in repositoryPaths }
}

fun calculateLabel(
  project: Project,
  buildFile: Path,
  targetName: String? = null,
): ResolvedLabel? {
  val targetBaseDirectory = buildFile.parent ?: return null
  val containingRepoPath = findContainingBazelRepo(project, targetBaseDirectory) ?: return null
  val containingCanonicalRepoName =
    project.canonicalRepoNameToPath.entries
      .firstOrNull { (_, repoPath) ->
        repoPath == containingRepoPath
      }?.key ?: return null

  val relativeTargetBaseDirectory = targetBaseDirectory.relativeToOrNull(containingRepoPath) ?: return null
  return ResolvedLabel(
    repo = Canonical.createCanonicalOrMain(containingCanonicalRepoName),
    packagePath = Package(relativeTargetBaseDirectory.segments()),
    target = targetName?.let { SingleTarget(targetName) } ?: AmbiguousEmptyTarget,
  )
}

private fun Path.segments(): List<String> {
  // Check needed for the case if we're computing a relative path between two equal paths
  if (nameCount == 1 && toString().isEmpty()) return emptyList()
  return (0 until nameCount).map { getName(it).toString() }
}

/**
 * We should show apparent labels in the UI, where possible, to avoid confusing the user with labels containing symbols such as ~ or +.
 */
fun Label.toApparentLabel(project: Project): Label {
  if (this !is ResolvedLabel) return this
  if (this.repo !is Canonical) return this
  val apparentRepoName = project.canonicalRepoNameToApparentName[this.repo.repoName] ?: return this
  return this.copy(repo = Apparent(apparentRepoName))
}

fun Label.toShortString(project: Project): String {
  val label = this.toApparentLabel(project)
  if (label !is ResolvedLabel) return label.toString()

  val repoPart = if (label.repo !is Main) label.repo.toString() else ""
  val packagePart = label.packagePath.toString()
  val targetPart =
    when {
      label.target is AmbiguousEmptyTarget -> ""
      label.target is SingleTarget && (label.target as SingleTarget).targetName == (label.packagePath as? Package)?.name() -> ""
      else -> ":${label.target}"
    }
  return "$repoPart//$packagePart$targetPart"
}

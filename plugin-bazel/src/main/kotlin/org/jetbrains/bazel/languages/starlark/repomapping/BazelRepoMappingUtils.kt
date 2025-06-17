package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.ApparentLabel
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.utils.allAncestorsSequence
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeToOrNull

fun findContainingBazelRepo(path: Path): Path? =
  path.allAncestorsSequence().firstOrNull {
    WORKSPACE_FILE_NAMES.any { workspaceFileName ->
      it.resolve(workspaceFileName).isRegularFile()
    }
  }

fun calculateLabel(
  project: Project,
  buildFile: Path,
  targetName: String? = null,
): CanonicalLabel? {
  val targetBaseDirectory = buildFile.parent ?: return null
  val containingRepoPath = findContainingBazelRepo(targetBaseDirectory) ?: return null
  val containingCanonicalRepoName =
    project.canonicalRepoNameToPath.entries
      .firstOrNull { (_, repoPath) ->
        repoPath == containingRepoPath
      }?.key ?: return null

  val packagePath = targetBaseDirectory.relativeToOrNull(containingRepoPath)?.segments() ?: return null

  val target =
    if (targetName != null) {
      targetName
    } else {
      if (packagePath.isEmpty()) {
        return null
      }
      packagePath.last()
    }
  return CanonicalLabel(
    repo = Canonical(containingCanonicalRepoName),
    packagePath = Package(packagePath),
    target = SingleTarget(target),
  )
}

private fun Path.segments(): List<String> {
  // Check needed for the case if we're computing a relative path between two equal paths
  if (nameCount == 1 && toString().isEmpty()) return emptyList()
  return (0 until nameCount).map { getName(it).toString() }
}

/**
 * We should show apparent labels in the UI, where possible, to avoid confusing the user with labels containing symbols such as `~` or `+`.
 * If conversion to an apparent label fails, fall back to the original label.
 */
fun Label.toApparentLabelOrThis(project: Project): Label = (this as? CanonicalLabel)?.toApparentLabel(project) ?: this

/**
 * Converts this [Label]'s repository either to [Apparent] or to [Main].
 */
fun CanonicalLabel.toApparentLabel(project: Project): ApparentLabel? {
  val apparentRepoName = project.canonicalRepoNameToApparentName[this.repo.repoName] ?: return null
  return ApparentLabel(
    repo = Apparent(apparentRepoName),
    packagePath = this.packagePath,
    target = this.target,
  )
}

/**
 * Converts this [Label]'s repository to [Canonical].
 */
fun Label.toCanonicalLabel(project: Project): CanonicalLabel? {
  if (this is CanonicalLabel) return this
  val canonicalRepoName = project.apparentRepoNameToCanonicalName[repo.repoName] ?: return null
  return CanonicalLabel(
    repo = Canonical(canonicalRepoName),
    packagePath = this.packagePath,
    target = this.target,
  )
}

fun Label.toCanonicalLabelOrThis(project: Project): ResolvedLabel? = toCanonicalLabel(project) ?: this as? ResolvedLabel

fun Label.toShortString(project: Project): String {
  val label = this.toApparentLabelOrThis(project)

  val repoPart = if (!label.repo.isMain) label.repo.toString() else ""
  val packagePart = label.packagePath.toString()
  val targetPart =
    if (label.target.targetName == label.packagePath.name()) {
      ""
    } else {
      ":${label.target}"
    }
  return "$repoPart//$packagePart$targetPart"
}

package org.jetbrains.bazel.commons.label

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import java.nio.file.Path
import kotlin.io.path.Path

private const val SYNTHETIC_TAG = "[synthetic]"
private const val WILDCARD = "*"
private const val ALL_TARGETS = "all-targets"
private const val ALL = "all"
private const val PATH_SEGMENT_SEPARATOR = "/"
private const val ALL_PACKAGES_BENEATH = "..."

/**
 * See https://bazel.build/run/build#specifying-build-targets
 */
sealed interface TargetType

open class SingleTarget(val targetName: String) : TargetType {
  override fun toString(): String = targetName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SingleTarget) return false
    if (targetName != other.targetName) return false
    return true
  }

  override fun hashCode(): Int = targetName.hashCode()
}

/**
 * Either :* or :all-targets (meaning all rules and files)
 */
data object AllRuleTargetsAndFiles : TargetType {
  override fun toString(): String = "*"
}

/**
 * All rule targets in the given package (excluding files)
 */
data object AllRuleTargets : TargetType {
  override fun toString(): String = "all"
}

/**
 * When we parse `//src/Hello.java`, we assume that it's a package named `Hello.java`, but there's no way to know from the label itself.
 */
class AmbiguousSingleTarget(targetName: String) : SingleTarget(targetName)

sealed interface PackageType {
  val pathSegments: List<String>
}

data class Package(override val pathSegments: List<String>) : PackageType {
  override fun toString(): String = pathSegments.joinToString(PATH_SEGMENT_SEPARATOR)

  fun parent(): Package = Package(pathSegments.dropLast(1))

  fun name(): String = pathSegments.lastOrNull() ?: ""
}

data class AllPackagesBeneath(override val pathSegments: List<String>) : PackageType {
  override fun toString(): String =
    if (pathSegments.isEmpty()) {
      ALL_PACKAGES_BENEATH
    } else {
      pathSegments.joinToString(PATH_SEGMENT_SEPARATOR, postfix = PATH_SEGMENT_SEPARATOR + ALL_PACKAGES_BENEATH)
    }
}

sealed interface RepoType {
  val repoName: String
}

/**
 * Targets in the main workspace are special-cased because they can be referred to
 * using both syntaxes and there's no need to use a repository mapping to resolve the label.
 */
data object Main : RepoType {
  override val repoName: String = ""

  override fun toString(): String = "@"
}

/**
 * See https://bazel.build/external/overview#canonical-repo-name
 */
data class Canonical(override val repoName: String) : RepoType {
  override fun toString(): String = "@@$repoName"
}

/**
 * See https://bazel.build/external/overview#apparent-repo-name
 */
data class Apparent(override val repoName: String) : RepoType {
  override fun toString(): String = "@$repoName"
}

data class ResolvedLabel(
  val repo: RepoType,
  override val packagePath: PackageType,
  override val target: TargetType,
) : Label {
  val repoName get() = repo.repoName

  /**
   * Returns a path to the corresponding folder in the `bazel-(project)` directory.
   * Warning: this works on label with apparent repo names only if bzlmod is not used.
   * If bzlmod is used, you need to use canonical form to resolve the path.
   */
  fun toBazelPath(): Path =
    if (packagePath !is Package) {
      error("Cannot convert wildcard package to path")
    } else {
      when (repo) {
        is Main -> Path(packagePath.toString())
        is Canonical -> Path("external", repo.repoName, *packagePath.pathSegments.toTypedArray())
        /** This works only without bzlmod... (with bzlmod you need a canonical form to resolve this path */
        is Apparent -> Path("external", repo.repoName, *packagePath.pathSegments.toTypedArray())
      }
    }

  override fun toString(): String = "$repo//$targetPathAndName"
}

/**
 * Synthethic label is a label of a target which is not present in the Bazel target graph.
 */
data class SyntheticLabel(override val target: TargetType) : Label {
  override val packagePath: PackageType = Package(listOf())

  override fun toString(): String = "$target$SYNTHETIC_TAG"
}

data class RelativeLabel(override val packagePath: PackageType, override val target: TargetType) : Label {
  override fun toString(): String = joinPackagePathAndTarget(packagePath, target)

  fun resolve(base: ResolvedLabel): ResolvedLabel {
    val repo = base.repo
    val mergedPath = base.packagePath.pathSegments + this.packagePath.pathSegments
    val packagePath =
      when (base.packagePath) {
        is Package ->
          when (this.packagePath) {
            is Package -> Package(mergedPath)
            is AllPackagesBeneath -> AllPackagesBeneath(mergedPath)
          }
        is AllPackagesBeneath -> error("Cannot resolve a relative label with respect to a wildcard package")
      }
    val target = this.target
    return ResolvedLabel(repo, packagePath, target)
  }
}

/**
 * Represents a Bazel label.
 * See https://bazel.build/concepts/labels
 */
sealed interface Label {
  val packagePath: PackageType
  val target: TargetType

  val targetName: String get() = target.toString()

  val isMainWorkspace: Boolean
    get() = (this as? ResolvedLabel)?.repo is Main || this is SyntheticLabel

  val isRelative: Boolean
    get() = this is RelativeLabel

  val isSynthetic: Boolean
    get() = this is SyntheticLabel

  val isWildcard: Boolean
    get() = target is AllRuleTargetsAndFiles || target is AllRuleTargets || packagePath is AllPackagesBeneath

  val isRecursive: Boolean
    get() = packagePath is AllPackagesBeneath

  val isApparent: Boolean
    get() = (this as? ResolvedLabel)?.repo is Apparent

  val targetPathAndName
    get() =
      when (packagePath) {
        is Package -> {
          val packageName = (packagePath as Package).name()
          when {
            target is SingleTarget && packageName == target.toString() -> packagePath.toString()
            else -> joinPackagePathAndTarget(packagePath, target)
          }
        }
        is AllPackagesBeneath ->
          if (target is AllRuleTargets) {
            packagePath.toString()
          } else {
            joinPackagePathAndTarget(packagePath, target)
          }
      }

  companion object {
    fun synthetic(targetName: String): Label = SyntheticLabel(SingleTarget(targetName.removeSuffix(SYNTHETIC_TAG)))

    fun parse(value: String): Label {
      if (value.endsWith(SYNTHETIC_TAG)) return synthetic(value)
      val normalized = value.trimStart('@')
      val repoName = normalized.substringBefore("//", "")
      val pathAndName = normalized.substringAfter("//")
      val packagePath = pathAndName.substringBefore(":")
      val packageSegments = packagePath.split(PATH_SEGMENT_SEPARATOR).filter { it.isNotEmpty() }
      val packageType =
        if (packageSegments.lastOrNull() == ALL_PACKAGES_BENEATH) {
          AllPackagesBeneath(packageSegments.dropLast(1))
        } else {
          Package(packageSegments)
        }
      val targetName = pathAndName.substringAfter(":", packagePath.substringAfterLast(PATH_SEGMENT_SEPARATOR))

      val target =
        when {
          targetName == WILDCARD -> AllRuleTargetsAndFiles
          targetName == ALL_TARGETS -> AllRuleTargetsAndFiles
          targetName == ALL -> AllRuleTargets
          targetName == ALL_PACKAGES_BENEATH -> AllRuleTargets // Special case for //...:...
          pathAndName.contains(":") -> SingleTarget(targetName)
          else -> AmbiguousSingleTarget(targetName)
        }

      if (!value.contains("//")) {
        return RelativeLabel(packageType, target)
      }

      val repo =
        when {
          repoName.isEmpty() -> Main
          value.startsWith("@@") -> Canonical(repoName)
          else -> Apparent(repoName)
        }

      return ResolvedLabel(repo, packageType, target)
    }

    fun parseOrNull(value: String?): Label? =
      try {
        value?.let { parse(it) }
      } catch (_: Exception) {
        null
      }
  }
}

fun Label.toBspIdentifier(): BuildTargetIdentifier = BuildTargetIdentifier(toString())

fun BuildTargetIdentifier.label(): Label = Label.parse(uri)

fun Label.asRelative(): RelativeLabel? = this as? RelativeLabel

fun Label.assumeResolved(): ResolvedLabel =
  when (this) {
    is ResolvedLabel -> this
    is SyntheticLabel -> error("Cannot resolve synthetic label $this")
    is RelativeLabel -> this.resolve(ResolvedLabel(Main, Package(listOf()), SingleTarget("")))
  }

private fun joinPackagePathAndTarget(packagePath: PackageType, target: TargetType) =
  when (target) {
    is AmbiguousSingleTarget -> packagePath.toString()
    else -> "$packagePath:$target"
  }

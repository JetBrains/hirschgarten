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

data class SingleTarget(val targetName: String) : TargetType {
  override fun toString(): String = targetName
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
 * Label `//src/Hello.java` can mean different things:
 * - target `Hello.java` in the package `src` (same as `//src:Hello.java`)
 * - target `Hello.java` in the package `src/Hello.java` (same as `//src/Hello.java:Hello.java`)
 * - file `src/Hello.java` in the root package (same as `//:src/Hello.java`)
 *
 * Unfortunately the meaning depends on the package structure (i.e. which of the folders `src` and `src/Hello.java` exist and whether
 * there are BUILD files in them).
 *
 * Bazel docs say:
 * > Bazel allows a slash to be used instead of the colon required by the label syntax;
 * > this is often convenient when using Bash filename expansion. For example, foo/bar/wiz is equivalent to
 * > //foo/bar:wiz (if there is a package foo/bar) or to //foo:bar/wiz (if there is a package foo).
 */
data object AmbiguousEmptyTarget : TargetType {
  override fun toString(): String = ""
}

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

  override fun toString(): String = "$repo//${joinPackagePathAndTarget(packagePath, target)}"
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

  val targetName: String get() =
    when (target) {
      is AmbiguousEmptyTarget ->
        when (packagePath) {
          is Package -> (packagePath as Package).name()
          is AllPackagesBeneath -> ALL_PACKAGES_BENEATH
        }
      else -> target.toString()
    }

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
          else -> AmbiguousEmptyTarget
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
    is AmbiguousEmptyTarget -> packagePath.toString()
    else -> "$packagePath:$target"
  }

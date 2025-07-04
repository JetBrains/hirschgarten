package org.jetbrains.bazel.label

import org.jetbrains.bazel.annotations.PublicApi
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

@PublicApi
sealed interface PackageType {
  val pathSegments: List<String>
}

fun PackageType.toPath(): Path = Path(pathSegments.joinToString(PATH_SEGMENT_SEPARATOR))

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

  val isMain: Boolean
    get() = repoName == ""
}

/**
 * See https://bazel.build/external/overview#canonical-repo-name
 */
data class Canonical(override val repoName: String) : RepoType {
  override fun toString(): String = "@@$repoName"

  companion object {
    val main = Canonical("")
  }
}

/**
 * See https://bazel.build/external/overview#apparent-repo-name
 */
data class Apparent(override val repoName: String) : RepoType {
  override fun toString(): String = if (repoName.isEmpty()) "" else "@$repoName"

  companion object {
    val main = Apparent("")
  }
}

// single target, repo can be resolved or not, can be synthetic
sealed interface Label : TargetPattern {
  override val target: SingleTarget
  override val packagePath: Package
  override val repo: RepoType

  val repoName: String get() = repo.repoName

  companion object {
    fun synthetic(targetName: String): CanonicalLabel {
      val name = targetName.removeSuffix(SYNTHETIC_TAG) + SYNTHETIC_TAG
      return CanonicalLabel(Canonical.main, Package(emptyList()), SingleTarget(name))
    }

    fun parseCanonical(value: String): CanonicalLabel =
      parse(value) as? CanonicalLabel
        ?: throw IllegalArgumentException("Cannot parse $value as a canonical label")

    fun parse(value: String): Label = TargetPattern.parse(value).assumeLabel()

    fun parseOrNull(value: String?): Label? =
      try {
        value?.let { parse(it) }
      } catch (e: Exception) {
        null
      }
  }
}

data class ApparentLabel(
  override val repo: Apparent,
  override val packagePath: Package,
  override val target: SingleTarget,
) : Label {
  override fun toString(): String = "$repo//${joinPackagePathAndTarget(packagePath, target)}"
}

// single target, repo must be resolved, not synthetic. Uniquely identifies a real Bazel target
data class CanonicalLabel(
  override val repo: Canonical,
  override val packagePath: Package,
  override val target: SingleTarget,
) : Label {
  override fun toString(): String = "$repo//${joinPackagePathAndTarget(packagePath, target)}"

  companion object {
    fun fromParts(
      repo: String,
      packagePath: Package,
      target: SingleTarget,
    ): CanonicalLabel = CanonicalLabel(Canonical(repo), packagePath, target)
  }
}

data class AbsoluteTargetPattern(
  override val repo: RepoType,
  override val packagePath: PackageType,
  override val target: TargetType,
) : TargetPattern {
  override fun toString(): String = "$repo//${joinPackagePathAndTarget(packagePath, target)}"
}

data class RelativeTargetPattern(override val packagePath: PackageType, override val target: TargetType) : TargetPattern {
  override val repo: RepoType = Apparent.main

  override fun toString(): String = joinPackagePathAndTarget(packagePath, target)

  fun resolve(base: AbsoluteTargetPattern): AbsoluteTargetPattern {
    val mergedPath = base.packagePath.pathSegments + this.packagePath.pathSegments
    val packagePath: Package =
      when (this.packagePath) {
        is Package -> Package(mergedPath)
        is AllPackagesBeneath -> error("Cannot resolve a wildcard package $this")
      }
    return AbsoluteTargetPattern(base.repo, packagePath, target)
  }

  fun resolve(base: Label): Label {
    val mergedPath = base.packagePath.pathSegments + this.packagePath.pathSegments
    val packagePath: Package =
      when (this.packagePath) {
        is Package -> Package(mergedPath)
        is AllPackagesBeneath -> error("Cannot resolve a wildcard package $this")
      }
    val target = this.assumeLabel().target
    return when (base) {
      is CanonicalLabel -> CanonicalLabel(base.repo, packagePath, target)
      is ApparentLabel -> ApparentLabel(base.repo, packagePath, target)
    }
  }
}

/**
 * Represents a Bazel target pattern.
 * See https://bazel.build/run/build#specifying-build-targets
 */
sealed interface TargetPattern : Comparable<TargetPattern> {
  @PublicApi
  val packagePath: PackageType
  val target: TargetType
  val repo: RepoType

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
    get() = (this as? AbsoluteTargetPattern)?.repo?.isMain == true || (this as? Label)?.repo?.isMain == true

  val isRelative: Boolean
    get() = this is RelativeTargetPattern

  val isSynthetic: Boolean
    get() = this is CanonicalLabel && target.targetName.endsWith(SYNTHETIC_TAG)

  val isWildcard: Boolean
    get() = target is AllRuleTargetsAndFiles || target is AllRuleTargets || packagePath is AllPackagesBeneath

  val isRecursive: Boolean
    get() = packagePath is AllPackagesBeneath

  val isApparent: Boolean
    get() = this is ApparentLabel || (this as? AbsoluteTargetPattern)?.repo is Apparent

  override fun compareTo(other: TargetPattern): Int = toString().compareTo(other.toString())

  companion object {
    fun parse(value: String): TargetPattern {
      if (value.endsWith(SYNTHETIC_TAG)) return Label.synthetic(value)
      val normalized = value.trim().trimStart('@')
      if (normalized.contains(" ")) throw IllegalArgumentException("Labels cannot have whitespaces")
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

      if (packageType is AllPackagesBeneath && target is SingleTarget) {
        throw IllegalArgumentException("Cannot have a single target in a wildcard package")
      }

      if (!value.contains("//")) {
        return RelativeTargetPattern(packageType, target)
      }

      val repo =
        when {
          value.startsWith("@@") -> Canonical(repoName)
          else -> Apparent(repoName)
        }

      return if (packageType is Package && target is SingleTarget) {
        when (repo) {
          is Canonical -> CanonicalLabel(repo, packageType, target)
          is Apparent -> ApparentLabel(repo, packageType, target)
        }
      } else {
        AbsoluteTargetPattern(repo, packageType, target)
      }
    }

    fun parseOrNull(value: String?): TargetPattern? =
      try {
        value?.let { parse(it) }
      } catch (_: Exception) {
        null
      }
  }
}

fun TargetPattern.asRelative(): RelativeTargetPattern? = this as? RelativeTargetPattern

fun TargetPattern.tryAssumeLabel(): Label? =
  try {
    assumeLabel()
  } catch (_: Exception) {
    null
  }

fun TargetPattern.assumeLabel(): Label =
  when (this) {
    is ApparentLabel -> this
    is CanonicalLabel -> this
    else -> {
      val singlePackagePath =
        when (packagePath) {
          is Package -> packagePath as Package
          is AllPackagesBeneath -> error("Wildcard package $this is not a valid label")
        }

      val singleTarget =
        when (target) {
          is AmbiguousEmptyTarget -> SingleTarget(singlePackagePath.name())
          is SingleTarget -> target as SingleTarget
          else -> error("Wildcard pattern $this is not a valid label")
        }

      val repo =
        when (this) {
          is RelativeTargetPattern -> Apparent.main
          is AbsoluteTargetPattern -> this.repo
          else -> error("Impossible state")
        }

      when (repo) {
        is Canonical -> CanonicalLabel(repo, singlePackagePath, singleTarget)
        is Apparent -> ApparentLabel(repo, singlePackagePath, singleTarget)
      }
    }
  }

private fun joinPackagePathAndTarget(packagePath: PackageType, target: TargetType) =
  when (target) {
    is AmbiguousEmptyTarget -> packagePath.toString()
    // If it was parsed as Label
    is SingleTarget -> if (packagePath.pathSegments.lastOrNull() == target.targetName) packagePath.toString() else "$packagePath:$target"
    else -> "$packagePath:$target"
  }

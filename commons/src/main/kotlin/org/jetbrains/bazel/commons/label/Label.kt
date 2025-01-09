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

/**
 * Represents a Bazel label.
 * See https://bazel.build/concepts/labels
 */
sealed interface Label {
  val repoName: String
  val packagePath: PackageType
  val target: TargetType

  val targetName: String get() = target.toString()

  val isMainWorkspace: Boolean
    get() = this is Main || this is Synthetic

  val isSynthetic: Boolean
    get() = this is Synthetic

  val isWildcard: Boolean
    get() = target is AllRuleTargetsAndFiles || target is AllRuleTargets || packagePath is AllPackagesBeneath

  val isRecursive: Boolean
    get() = packagePath is AllPackagesBeneath

  val isApparent: Boolean
    get() = this is Apparent

  /**
   * Returns a path to the corresponding folder in the `bazel-(project)` directory.
   * Warning: this works on label with apparent repo names only if bzlmod is not used.
   * If bzlmod is used, you need to use canonical form to resolve the path.
   */
  fun toBazelPath(): Path

  val targetPathAndName
    get() =
      when (packagePath) {
        is Package -> {
          val packageName = (packagePath as Package).name()
          when {
            target is SingleTarget && packageName == target.toString() -> packagePath.toString()
            else -> "$packagePath:$target"
          }
        }
        is AllPackagesBeneath ->
          if (target is AllRuleTargets) {
            packagePath.toString()
          } else {
            "$packagePath:$target"
          }
      }

  /**
   * Targets in the main workspace are special-cased because they can be referred to
   * using both syntaxes and there's no need to use a repository mapping to resolve the label.
   */
  private data class Main(override val packagePath: PackageType, override val target: TargetType) : Label {
    override val repoName: String = ""

    override fun toBazelPath(): Path =
      if (packagePath is Package) {
        Path(packagePath.toString())
      } else {
        error("Cannot convert wildcard package to path")
      }

    override fun toString(): String = "@//$targetPathAndName"
  }

  /**
   * Synthethic label is a label of a target which is not present in the Bazel target graph.
   */
  private data class Synthetic(override val target: TargetType) : Label {
    override val repoName: String = ""
    override val packagePath: PackageType = Package(listOf())

    override fun toBazelPath(): Path = error("Synthetic labels do not have a path")

    override fun toString(): String = "$target$SYNTHETIC_TAG"
  }

  /**
   * See https://bazel.build/external/overview#canonical-repo-name
   */
  private data class Canonical(
    override val repoName: String,
    override val packagePath: PackageType,
    override val target: TargetType,
  ) : Label {
    override fun toBazelPath(): Path =
      if (packagePath is Package) {
        Path("external", repoName, *packagePath.pathSegments.toTypedArray())
      } else {
        error("Cannot convert wildcard package to path")
      }

    override fun toString(): String = "@@$repoName//$targetPathAndName"
  }

  /**
   * See https://bazel.build/external/overview#apparent-repo-name
   */
  private data class Apparent(
    override val repoName: String,
    override val packagePath: PackageType,
    override val target: TargetType,
  ) : Label {
    /** This works only without bzlmod... (with bzlmod you need a canonical form to resolve this path */
    override fun toBazelPath(): Path =
      if (packagePath is Package) {
        Path("external", repoName, *packagePath.pathSegments.toTypedArray())
      } else {
        error("Cannot convert wildcard package to path")
      }

    override fun toString(): String = "@$repoName//$targetPathAndName"
  }

  companion object {
    fun synthetic(targetName: String): Label = Synthetic(SingleTarget(targetName.removeSuffix(SYNTHETIC_TAG)))

    fun parse(value: String): Label {
      if (value.endsWith(SYNTHETIC_TAG)) return synthetic(value)
      val normalized = value.trimStart('@')
      val repoName = normalized.substringBefore("//", "")
      val pathAndName = normalized.substringAfter("//")
      val packagePath = pathAndName.substringBefore(":")
      val packageSegments = packagePath.split(PATH_SEGMENT_SEPARATOR)
      val packageType =
        if (packageSegments.lastOrNull() == ALL_PACKAGES_BENEATH) {
          AllPackagesBeneath(packageSegments.dropLast(1))
        } else {
          Package(packageSegments)
        }
      val targetName = pathAndName.substringAfter(":", packagePath.substringAfterLast(PATH_SEGMENT_SEPARATOR))

      val target =
        when (targetName) {
          WILDCARD -> AllRuleTargetsAndFiles
          ALL_TARGETS -> AllRuleTargetsAndFiles
          ALL -> AllRuleTargets
          ALL_PACKAGES_BENEATH -> AllRuleTargets // Special case for //...:...
          else -> SingleTarget(targetName)
        }

      return when {
        repoName.isEmpty() -> Main(packageType, target)
        value.startsWith("@@") -> Canonical(repoName, packageType, target)
        else -> Apparent(repoName, packageType, target)
      }
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

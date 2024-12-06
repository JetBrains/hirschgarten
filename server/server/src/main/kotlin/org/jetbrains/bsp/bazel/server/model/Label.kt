package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import java.nio.file.Path
import kotlin.io.path.Path

private const val SYNTHETIC_TAG = "[synthetic]"
private const val WILDCARD = "*"
private const val ALL_TARGETS = "all-targets"
private const val ALL = "all"

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
  val path: List<String>
}

data class Package(override val path: List<String>) : PackageType {
  override fun toString(): String = path.joinToString("/")

  fun parent(): Package = Package(path.dropLast(1))

  fun name(): String = path.lastOrNull() ?: ""
}

data class WildcardPackage(override val path: List<String>) : PackageType {
  override fun toString(): String =
    if (path.isEmpty()) {
      "..."
    } else {
      path.joinToString("/", postfix = "/...")
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
        is WildcardPackage ->
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
        Path("external", repoName, *packagePath.path.toTypedArray())
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
        Path("external", repoName, *packagePath.path.toTypedArray())
      } else {
        error("Cannot convert wildcard package to path")
      }

    override fun toString(): String = "@$repoName//$targetPathAndName"
  }

  companion object {
    fun synthetic(targetName: String): Label = Synthetic(SingleTarget(targetName.removeSuffix(SYNTHETIC_TAG)))

    fun parse(value: String): Label {
      val normalized = value.trimStart('@')
      val repoName = normalized.substringBefore("//", "")
      val pathAndName = normalized.substringAfter("//")
      val packagePath = pathAndName.substringBefore(":")
      val packageSegments = packagePath.split("/")
      val packageType =
        if (packageSegments.lastOrNull() == "...") {
          WildcardPackage(packageSegments.dropLast(1))
        } else {
          Package(packageSegments)
        }
      val targetName = pathAndName.substringAfter(":", packagePath.substringAfterLast("/"))

      val target =
        when (targetName) {
          WILDCARD -> AllRuleTargetsAndFiles
          ALL_TARGETS -> AllRuleTargetsAndFiles
          ALL -> AllRuleTargets
          else -> SingleTarget(targetName)
        }

      return when {
        repoName.isEmpty() -> Main(packageType, target)
        value.startsWith("@@") -> Canonical(repoName, packageType, target)
        else -> Apparent(repoName, packageType, target)
      }
    }
  }
}

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.id)

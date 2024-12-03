package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import java.nio.file.Path
import kotlin.io.path.Path

const val SYNTHETIC_TAG = "[synthetic]"

/**
 * Represents a Bazel label.
 * See https://bazel.build/concepts/labels
 */
sealed interface Label {
  val repoName: String
  val packagePath: String
  val targetName: String

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
      if (packagePath.substringAfterLast("/") == targetName) {
        packagePath
      } else {
        "$packagePath:$targetName"
      }

  /**
   * Targets in the main workspace are special-cased because they can be referred to
   * using both syntaxes and there's no need to use a repository mapping to resolve the label.
   */
  private data class Main(override val packagePath: String, override val targetName: String) : Label {
    override val repoName: String = ""

    override fun toBazelPath(): Path = Path(packagePath)

    override fun toString(): String = "@//$targetPathAndName"
  }

  /**
   * Synthethic label is a label of a target which is not present in the Bazel target graph.
   */
  private data class Synthetic(override val targetName: String) : Label {
    override val repoName: String = ""
    override val packagePath: String = ""

    override fun toBazelPath(): Path = error("Synthetic labels do not have a path")

    override fun toString(): String = "$targetName$SYNTHETIC_TAG"
  }

  /**
   * See https://bazel.build/external/overview#canonical-repo-name
   */
  private data class Canonical(
    override val repoName: String,
    override val packagePath: String,
    override val targetName: String,
  ) : Label {
    override fun toBazelPath(): Path = Path("external", repoName, packagePath)

    override fun toString(): String = "@@$repoName//$targetPathAndName"
  }

  /**
   * See https://bazel.build/external/overview#apparent-repo-name
   */
  private data class Apparent(
    override val repoName: String,
    override val packagePath: String,
    override val targetName: String,
  ) : Label {
    /** This works only without bzlmod... (with bzlmod you need a canonical form to resolve this path */
    override fun toBazelPath(): Path = Path("external", repoName, packagePath)

    override fun toString(): String = "@$repoName//$targetPathAndName"
  }

  companion object {
    fun synthetic(targetName: String): Label = Synthetic(targetName.removeSuffix(SYNTHETIC_TAG))

    fun parse(value: String): Label {
      val normalized = value.removePrefix("@").removePrefix("@")
      val repoName = normalized.substringBefore("//", "")
      val pathAndName = normalized.substringAfter("//")
      val targetPath = pathAndName.substringBefore(":")
      val targetName = pathAndName.substringAfter(":", targetPath.substringAfterLast("/"))
      return when {
        repoName.isEmpty() -> Main(targetPath, targetName)
        value.startsWith("@@") -> Canonical(repoName, targetPath, targetName)
        else -> Apparent(repoName, targetPath, targetName)
      }
    }
  }
}

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

fun BspTargetInfo.Dependency.label(): Label = Label.parse(this.id)

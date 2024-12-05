package org.jetbrains.bsp.bazel.server.model

import com.fasterxml.jackson.annotation.JsonValue
import org.jetbrains.bsp.bazel.info.BspTargetInfo

private const val ALL_PACKAGES_RECURSIVE_SUFFIX = "/..."
private val ALL_TARGETS_IN_SUFFIXES = listOf("*", "all-targets")
private const val ALL_RULES_IN_SUFFIX = "all"

// Labels are normalized to always start with "@//"
@JvmInline
value class Label private constructor(
  @JsonValue val value: String,
) {
  val targetName: String
    get() = value.substringAfterLast(":", "")

  val targetPath: String
    get() =
      if (value.endsWith(ALL_PACKAGES_RECURSIVE_SUFFIX)) {
        value.substringBeforeLast(ALL_PACKAGES_RECURSIVE_SUFFIX).substringAfterLast("//")
      } else {
        value.substringBeforeLast(":", "").substringAfterLast("//")
      }

  val repoName: String
    get() = value.substringBefore("//").removePrefix("@")

  val isMainWorkspace: Boolean
    get() = repoName.isEmpty()

  val isRecursive: Boolean
    get() = value.endsWith(ALL_PACKAGES_RECURSIVE_SUFFIX) || ALL_TARGETS_IN_SUFFIXES.contains(targetName)

  val isRulesOnly: Boolean
    get() = targetName == ALL_RULES_IN_SUFFIX

  val isWildcard: Boolean
    get() = isRecursive || isRulesOnly

  fun toExternalPath(): String =
    if (isMainWorkspace) {
      error("Cannot convert main workspace label to external path")
    } else {
      "external/$repoName/$targetPath"
    }

  override fun toString(): String = value

  companion object {
    fun parse(value: String): Label {
      val stripped = value.trimStart('@')
      val normalized =
        if (!value.contains("//")) {
          stripped // special case for synthetic/fake targets like "scala-compiler-2.12.14.jar"
        } else {
          "@$stripped"
        }
      return Label(normalized.intern())
    }

    fun allFromPackageNonRecursive(packagePath: String): Label =
      parse(
        "@//" + packagePath.removeSuffix("/") + ":" + ALL_RULES_IN_SUFFIX,
      )
  }
}

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

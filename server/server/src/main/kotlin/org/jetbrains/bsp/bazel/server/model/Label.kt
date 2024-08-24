package org.jetbrains.bsp.bazel.server.model

import com.fasterxml.jackson.annotation.JsonValue
import org.jetbrains.bsp.bazel.info.BspTargetInfo

// Labels are normalized to always start with "@//"
@JvmInline
value class Label private constructor(
  @JsonValue val value: String,
) {
  val targetName: String
    get() = value.substringAfterLast(":", "")

  val targetPath: String
    get() = value.substringBeforeLast(":", "").substringAfterLast("//")

  val repoName: String
    get() = value.substringBefore("//").removePrefix("@")

  val isMainWorkspace: Boolean
    get() = repoName.isEmpty()

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
  }
}

fun BspTargetInfo.TargetInfo.label(): Label = Label.parse(this.id)

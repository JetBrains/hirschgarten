package org.jetbrains.bazel.server.sync.projectResolver

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.JavaCommonInfo
import org.jetbrains.bazel.info.BspTargetInfo.JvmOutputs
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetKey
import org.jetbrains.bazel.server.sync.TargetInfoReader
import org.junit.jupiter.api.Test

class TargetInfoReaderTest {
  @Test
  fun `resolveConflict picks jvm node with jars over jar-less jvm node`() {
    // Simulates the java_proto_library shadow-graph conflict: two TargetInfo nodes for the same
    // label where only the transitioned-config node (-ST-<hash>) has actual class jars.
    val jarlessNode = targetInfo(jvmTarget = true, jars = emptyList())
    val jarNode = targetInfo(jvmTarget = true, jars = listOf("libproto-speed.jar"))

    TargetInfoReader.resolveConflict(listOf(jarlessNode, jarNode)) shouldBe jarNode
  }

  @Test
  fun `resolveConflict picks jvm node over non-jvm node when no jars present`() {
    val nonJvmNode = targetInfo(jvmTarget = false, jars = emptyList())
    val jvmNode = targetInfo(jvmTarget = true, jars = emptyList())

    TargetInfoReader.resolveConflict(listOf(nonJvmNode, jvmNode)) shouldBe jvmNode
  }

  @Test
  fun `resolveConflict picks smallest when multiple jvm nodes have jars`() {
    val bigNode = targetInfo(jvmTarget = true, jars = listOf("libproto-speed.jar"), extraField = "a".repeat(100))
    val smallNode = targetInfo(jvmTarget = true, jars = listOf("libproto-speed.jar"))

    TargetInfoReader.resolveConflict(listOf(bigNode, smallNode)) shouldBe smallNode
  }

  @Test
  fun `resolveConflict falls back to first candidate when list has one entry`() {
    val only = targetInfo(jvmTarget = false, jars = emptyList())

    TargetInfoReader.resolveConflict(listOf(only)) shouldBe only
  }

  private fun targetInfo(
    jvmTarget: Boolean,
    jars: List<String>,
    extraField: String = "",
  ): TargetInfo {
    val jvmOutputs = JvmOutputs.newBuilder().apply {
      jars.forEach { jar ->
        addBinaryJars(ArtifactLocation.newBuilder().setRelativePath(jar).build())
      }
    }.build()

    val javaCommon = JavaCommonInfo.newBuilder()
      .setJvmTarget(jvmTarget)
      .apply { if (jars.isNotEmpty()) addJars(jvmOutputs) }
      .build()

    return TargetInfo.newBuilder()
      .setKey(TargetKey.newBuilder().setLabel("@@//proto/accounts/v1:java_proto").build())
      .setJavaCommon(javaCommon)
      .setKind(extraField)
      .build()
  }
}

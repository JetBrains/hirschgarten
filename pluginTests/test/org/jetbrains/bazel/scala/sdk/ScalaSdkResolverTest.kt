package org.jetbrains.bazel.scala.sdk

import com.google.devtools.intellij.aspect.Common
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ScalaSdkResolverTest {
  @Test
  fun `test release version`() {
    val oldVersion = Common.ArtifactLocation.newBuilder()
      .setRelativePath("scala-compiler-2.9.18.jar")
      .setRootPath("external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_9_18")
      .setIsSource(true)
      .setIsExternal(true)
    val newVersion = Common.ArtifactLocation.newBuilder()
      .setRelativePath("scala-compiler-2.13.18.jar")
      .setRootPath("external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_18")
      .setIsSource(true)
      .setIsExternal(true)
    val scalaTargetInfo = IntellijIdeInfo.ScalaTargetInfo.newBuilder()
      .addCompilerClasspath(oldVersion)
      .addCompilerClasspath(newVersion)
    val targetInfo = IntellijIdeInfo.TargetIdeInfo.newBuilder()
      .setScalaTargetInfo(scalaTargetInfo)
      .build()

    ScalaSdkResolver.resolveScalaVersion(targetInfo) shouldBe "2.13.18"
  }

  @Test
  fun `test prerelease version`() {
    val location = Common.ArtifactLocation.newBuilder()
      .setRelativePath("scala-compiler-3.10.0-RC2.jar")
      .setRootPath("external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_3_10_0_RC2")
      .setIsSource(true)
      .setIsExternal(true)
    val scalaTargetInfo = IntellijIdeInfo.ScalaTargetInfo.newBuilder()
      .addCompilerClasspath(location)
    val targetInfo = IntellijIdeInfo.TargetIdeInfo.newBuilder()
      .setScalaTargetInfo(scalaTargetInfo)
      .build()

    ScalaSdkResolver.resolveScalaVersion(targetInfo) shouldBe "3.10.0"
  }
}

package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.KotlinTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class JavaLanguagePluginAdditionalTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver = BazelPathsResolverMock.create(tmp ?: Path.of(""))

  private fun javaPlugin(resolver: BazelPathsResolver): JavaLanguagePlugin =
    JavaLanguagePlugin(resolver, JdkResolver(resolver, JdkVersionResolver()), DefaultJvmPackageResolver())

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  @Test
  fun `collectResources returns base resources for jvm targets`() {
    val pathsResolver = resolver()
    val plugin = javaPlugin(pathsResolver)

    val target = TargetInfo.newBuilder()
      .setId("//app:lib")
      .setKind("java_library")
      .setJvmTargetInfo(JvmTargetInfo.newBuilder().build())
      .addResources(fileLocation("app/src/main/resources/foo.txt"))
      .build()

    val resources = plugin.collectResources(target).toList()
    assertTrue(resources.any { it.fileName.toString() == "foo.txt" })
  }

  @Test
  fun `targetSupportsStrictDeps true only for pure Java`() {
    val pathsResolver = resolver()
    val plugin = javaPlugin(pathsResolver)

    val pureJava = TargetInfo.newBuilder()
      .setId("//core:java")
      .setKind("java_library")
      .setJvmTargetInfo(JvmTargetInfo.newBuilder().build())
      .build()

    val kotlinMixed = TargetInfo.newBuilder()
      .setId("//core:kotlin")
      .setKind("kt_jvm_library")
      .setJvmTargetInfo(JvmTargetInfo.newBuilder().build())
      .setKotlinTargetInfo(KotlinTargetInfo.getDefaultInstance())
      .build()

    assertTrue(plugin.targetSupportsStrictDeps(pureJava))
    assertFalse(plugin.targetSupportsStrictDeps(kotlinMixed))
  }
}

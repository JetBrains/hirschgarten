package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.junit.jupiter.api.Test
import java.nio.file.Path

class CppPathResolverTest {
  val fakeExecRoot = "/execRootFake"
  val fakeOutputBase = "/outputBaseFake"
  val fakeWorkspaceRoot = "/workspaceRootFake"
  private val mockBazelInfo =
    BazelInfo(
      execRoot = fakeExecRoot,
      outputBase = Path.of(fakeOutputBase),
      workspaceRoot = Path.of(fakeWorkspaceRoot),
      release = BazelRelease(7),
      isBzlModEnabled = true,
      isWorkspaceEnabled = true,
    )
  private val bazelPathResolver = BazelPathsResolver(mockBazelInfo)
  private val cppPathResolver = CppPathResolver(bazelPathResolver)

  private fun targetInfo(id: String, stripePrefix: String): TargetInfo =
    TargetInfo
      .newBuilder()
      .setId(id)
      .setCppTargetInfo(
        BspTargetInfo.CppTargetInfo
          .newBuilder()
          .setStripIncludePrefix(stripePrefix)
          .build(),
      ).build()

  private val targetNamesToPrefixes =
    mapOf(
      "simple_target" to "include",
      "simple_target_trailing_slash" to "include/",
      "advanced_target" to "src/main/cpp/include",
      "include_prefix" to "include",
    )

  private fun generateTargetName(
    externalWorkspace: String?,
    workspacePath: String,
    targetName: String,
  ): String {
    val targetNamePrefix =
      if (externalWorkspace == null) {
        "//$workspacePath:"
      } else {
        "@$externalWorkspace//$workspacePath:"
      }
    return "$targetNamePrefix$targetName"
  }

  private fun generateTargetMap(externalWorkspace: String?, workspacePath: String): Map<String, TargetInfo> =
    targetNamesToPrefixes
      .map { generateTargetName(externalWorkspace, workspacePath, it.key) to targetInfo(it.key, it.value) }
      .toMap()

  @Test
  fun shouldResolveExternalWorkspacePathRelativeToOutputBase() {
    val res = cppPathResolver.resolveToIncludeDirectories(Path.of("external/guava/src"), mapOf())
    res.size shouldBe 1
    res[0] shouldBe Path.of("$fakeOutputBase/external/guava/src").toUri()
  }

  @Test
  fun shouldResolveGeneratedFilesPathRelativeToExecRoot() {
    val res = cppPathResolver.resolveToIncludeDirectories(Path.of("bazel-out/crosstool/genfiles/res/normal"), mapOf())
    res.size shouldBe 1
    res[0] shouldBe Path.of("$fakeExecRoot/bazel-out/crosstool/genfiles/res/normal").toUri()
  }

  @Test
  fun shouldResolveMainWorkspacePathsRelativeToWorkspaceRoot() {
    val res = cppPathResolver.resolveToIncludeDirectories(Path.of("foo/bar/src"), mapOf())
    res.size shouldBe 1
    res[0] shouldBe Path.of("$fakeWorkspaceRoot/foo/bar/src").toUri()
  }

  @Test
  fun shouldNotIllegalPath() {
    val res = cppPathResolver.resolveToIncludeDirectories(Path.of("tools/fast/:include"), mapOf())
    res.size shouldBe 0
  }

  @Test
  fun shouldResolveVirtualIncludesInMainWorkspace() {
    val workspacePaths = listOf("", "foo", "foo/bar")
    for (workspacePath in workspacePaths) {
      val targetMap = generateTargetMap(null, workspacePath)
      for (target in targetNamesToPrefixes.keys) {
        val path = Path.of("bazel-out/k8-fastbuild/bin", workspacePath, "_virtual_includes", target)
        val res = cppPathResolver.resolveToIncludeDirectories(path, targetMap)
        var stripPrefix = targetNamesToPrefixes[target]!!
        if (stripPrefix.endsWith("/")) {
          stripPrefix = stripPrefix.substring(0, stripPrefix.length - 1)
        }
        res.size shouldBe 1
        res[0] shouldBe Path.of(fakeWorkspaceRoot, "$workspacePath/$stripPrefix").toUri()
      }
    }
  }

  @Test
  fun shouldResolveVirtualIncludesInExternalWorkspace() {
    val workspacePaths = listOf("", "foo", "foo/bar")
    for (workspacePath in workspacePaths) {
      val targetMap = generateTargetMap("external_workspace", workspacePath)
      for (target in targetNamesToPrefixes.keys) {
        val path = Path.of("bazel-out/k8-fastbuild/bin/external/external_workspace", workspacePath, "_virtual_includes", target)
        val res = cppPathResolver.resolveToIncludeDirectories(path, targetMap)
        var stripPrefix = targetNamesToPrefixes[target]!!
        if (stripPrefix.endsWith("/")) {
          stripPrefix = stripPrefix.substring(0, stripPrefix.length - 1)
        }
        res.size shouldBe 1
        res[0] shouldBe Path.of(fakeOutputBase, "external/external_workspace/$workspacePath/$stripPrefix").toUri()
      }
    }
  }

  @Test
  fun shouldResolveVirtualIncludesWithAbsoultePrefix() {
    val targetMap = generateTargetMap(null, "anything")
    for (target in targetNamesToPrefixes.keys) {
      val path = Path.of("bazel-out/k8-fastbuild/bin/", "anything", "_virtual_includes", target)
      val res = cppPathResolver.resolveToIncludeDirectories(path, targetMap)
      var stripPrefix = targetNamesToPrefixes[target]!!
      if (stripPrefix.endsWith("/")) {
        stripPrefix = stripPrefix.substring(0, stripPrefix.length - 1)
      }
      res.size shouldBe 1
      res[0] shouldBe Path.of(fakeWorkspaceRoot, "anything/$stripPrefix").toUri()
    }
  }

  @Test
  fun shouldResolveVirtualIncludesWithAbsoultePrefixInExternal() {
    val repo = "external_workspace"
    val targetMap = generateTargetMap(repo, "anything")
    for (target in targetNamesToPrefixes.keys) {
      val path = Path.of("bazel-out/k8-fastbuild/bin/external/$repo", "anything", "_virtual_includes", target)
      val res = cppPathResolver.resolveToIncludeDirectories(path, targetMap)
      var stripPrefix = targetNamesToPrefixes[target]!!
      if (stripPrefix.endsWith("/")) {
        stripPrefix = stripPrefix.substring(0, stripPrefix.length - 1)
      }
      res.size shouldBe 1
      res[0] shouldBe Path.of(fakeOutputBase, "external/$repo/anything/$stripPrefix").toUri()
    }
  }
}

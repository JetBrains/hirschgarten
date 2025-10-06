package org.jetbrains.bazel.sync.workspace.languages.protobuf

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.ProtobufSourceMapping
import org.jetbrains.bazel.info.BspTargetInfo.ProtobufTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class ProtobufLanguagePluginTest {
  private fun resolver(tmp: Path? = null): BazelPathsResolver = BazelPathsResolverMock.create(tmp ?: Path.of(""))

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun sourceMapping(importPath: String, protoPath: String): ProtobufSourceMapping =
    ProtobufSourceMapping.newBuilder()
      .setImportPath(importPath)
      .setProtoFile(fileLocation(protoPath))
      .build()

  private fun protoInfo(mappings: List<ProtobufSourceMapping>): ProtobufTargetInfo =
    ProtobufTargetInfo.newBuilder().addAllSourceMappings(mappings).build()

  private fun target(id: String, kind: String, pt: ProtobufTargetInfo? = null): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .apply { if (pt != null) setProtobufTargetInfo(pt) }
      .build()

  @Test
  fun `supportsTarget and isWorkspaceTarget behave as expected`() {
    val pathsResolver = resolver()
    val java = JavaLanguagePlugin(pathsResolver, JdkResolver(pathsResolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())
    val plugin = ProtobufLanguagePlugin(java)

    val tNo = target("//app:noop", "java_library", null)
    val tYes = target("//app:proto", "proto_library", protoInfo(emptyList()))

    assertFalse(plugin.supportsTarget(tNo))
    assertTrue(plugin.supportsTarget(tYes))

    // isWorkspaceTarget should be true for main workspace labels with protobuf info
    val fw = org.jetbrains.bsp.protocol.FeatureFlags()
    val repoMapping = org.jetbrains.bazel.commons.RepoMappingDisabled
    assertTrue(plugin.isWorkspaceTarget(tYes, repoMapping, fw))

    // For external repo label (starts with @), the default mapping marks it external
    val ext = target("@maven//:proto_dep", "proto_library", protoInfo(emptyList()))
    assertFalse(plugin.isWorkspaceTarget(ext, repoMapping, fw))
  }

  @Test
  fun `createBuildTargetData maps sources correctly`() {
    val pathsResolver = resolver(Path.of("/ws"))
    val java = JavaLanguagePlugin(pathsResolver, JdkResolver(pathsResolver, JdkVersionResolver()), DefaultJvmPackageResolver(), org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver())
    val plugin = ProtobufLanguagePlugin(java)

    val mapping = sourceMapping("foo/bar.proto", "app/src/main/proto/foo/bar.proto")
    val pt = protoInfo(listOf(mapping))
    val t = target("//app:proto", "proto_library", pt)

    val graph = org.jetbrains.bazel.sync.workspace.graph.DependencyGraph(setOf(Label.parse(t.id)), mapOf(Label.parse(t.id) to t))
    val ctx = LanguagePluginContext(t, graph, org.jetbrains.bazel.commons.RepoMappingDisabled, pathsResolver)

    val data = kotlinx.coroutines.runBlocking { plugin.createBuildTargetData(ctx, t) }
    assertNotNull(data)
    data!!

    // Expect mapping importPath -> absolute proto file path
    val srcs = data.sources
    assertEquals(1, srcs.size)
    val value = srcs["foo/bar.proto"]
    assertTrue(value!!.startsWith("/ws/"))
    assertTrue(value.endsWith("app/src/main/proto/foo/bar.proto"))
  }
}

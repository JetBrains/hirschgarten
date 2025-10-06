package org.jetbrains.bazel.sync.workspace.languages.go

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.GoTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class GoLanguagePluginTest {
  private fun resolver(workspace: Path = Path.of("/workspace")): BazelPathsResolver = BazelPathsResolverMock.create(workspace)

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun goTargetInfo(importPath: String = "example.com/mod", genSources: List<FileLocation> = emptyList(), genLibs: List<FileLocation> = emptyList()): GoTargetInfo =
    GoTargetInfo.newBuilder()
      .setImportPath(importPath)
      .addAllGeneratedSources(genSources)
      .addAllGeneratedLibraries(genLibs)
      .build()

  private fun target(id: String, kind: String, gt: GoTargetInfo): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .setGoTargetInfo(gt)
      .build()

  @Test
  fun `go plugin requires transitive selection and returns generated sources`() {
    val pathsResolver = resolver()
    val plugin = GoLanguagePlugin(pathsResolver)

    val gen = listOf(fileLocation("bazel-bin/gen/foo.go"))
    val goInfo = goTargetInfo(genSources = gen)
    val t = target("//pkg:lib", "go_library", goInfo)

    // requiresTransitiveSelection should be true for Go targets
    assertTrue(plugin.requiresTransitiveSelection(t))

    // calculateAdditionalSources should include generated sources
    val additional = plugin.calculateAdditionalSources(t).toList()
    assertTrue(additional.any { it.fileName.toString() == "foo.go" })
  }

  @Test
  fun `path conversions local_to_remote behave as expected`() {
    val ws = Path.of("/wsroot")
    val pathsResolver = resolver(ws)
    val plugin = GoLanguagePlugin(pathsResolver)

    // Local to remote: inside workspace becomes workspace-relative
    val inside = ws.resolve("pkg/file.go").toString()
    val l2r = plugin.resolveLocalToRemote(BazelResolveLocalToRemoteParams(listOf(inside)))
    assertEquals("pkg/file.go", l2r.resolvedPaths[inside])

    // Local to remote: outside workspace remains absolute, forward-slashed
    val outside = "/opt/go/src/std/os.go"
    val l2r2 = plugin.resolveLocalToRemote(BazelResolveLocalToRemoteParams(listOf(outside)))
    assertEquals("/opt/go/src/std/os.go", l2r2.resolvedPaths[outside])

    // Remote to local: GOROOT prefix replaced with goRoot and normalized
    val goRoot = "/usr/local/go"
    val remote = "GOROOT/src/runtime/os_linux.go"
    val r2l = plugin.resolveRemoteToLocal(BazelResolveRemoteToLocalParams(listOf(remote), goRoot))
    val mapped = r2l.resolvedPaths[remote] ?: ""
    assertTrue(mapped.replace('\\', '/').endsWith("/usr/local/go/src/runtime/os_linux.go"))

    // Remote to local: build sandbox path trimmed to suffix under workspace root
    val sandbox = "/build/work/1234/sandbox/linux-sandbox/execroot/__main__/pkg/file.go"
    val r2l2 = plugin.resolveRemoteToLocal(BazelResolveRemoteToLocalParams(listOf(sandbox), goRoot))
    val mapped2 = r2l2.resolvedPaths[sandbox] ?: ""
    assertTrue(mapped2.replace('\\', '/').endsWith("/pkg/file.go"))
    assertTrue(mapped2.startsWith(ws.toString()))
  }
}

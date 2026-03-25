package org.jetbrains.bazel.commons

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.BspTargetInfo
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.test.assertEquals

class BazelPathsResolverTest {

  fun newResolver() : BazelPathsResolver {
    val bazelInfo =
      BazelInfo(
        execRoot = Paths.get("bazel-exec"),
        outputBase = Paths.get("bazel-out"),
        workspaceRoot = Paths.get("workspace"),
        bazelBin = Path("bazel-bin"),
        release = BazelRelease.fromReleaseString("release 9.0.0").orFallbackVersion(),
        true,
        false,
        emptyList(),
      )
    return BazelPathsResolver(bazelInfo)
  }

  fun sourceArtifact(root: String, relativePath: String) =
    BspTargetInfo.ArtifactLocation.newBuilder().setRootPath(root).setRelativePath(relativePath).setIsSource(true).build()

  fun nonSourceArtifact(root: String, relativePath: String) =
    BspTargetInfo.ArtifactLocation.newBuilder().setRootPath(root).setRelativePath(relativePath).build()

  @Test
  fun isExternalRecognizesLocalRepositories() {
    val resolver = newResolver()
    val localRepositories = LocalRepositoryMapping(mapOf("foo+" to Path("bar/baz")))

    resolver.isExternal(sourceArtifact("a/b", "c/d/E.java"), localRepositories) shouldBe false
    resolver.isExternal(sourceArtifact("external/bar+/a/b/c", "d/E.java"), localRepositories) shouldBe true
    resolver.isExternal(sourceArtifact("../bar+/a/b/c", "d/E.java"), localRepositories) shouldBe true
    resolver.isExternal(sourceArtifact("external/foo+/a/b/c", "d/E.java"), localRepositories) shouldBe false
    resolver.isExternal(sourceArtifact("../foo+/a/b/c", "d/E.java"), localRepositories) shouldBe false

    resolver.isExternal(nonSourceArtifact("a/b", "c/d/E.java"), localRepositories) shouldBe false
    resolver.isExternal(nonSourceArtifact("external/bar+/a/b/c", "d/E.java"), localRepositories) shouldBe true
    resolver.isExternal(nonSourceArtifact("../bar+/a/b/c", "d/E.java"), localRepositories) shouldBe true
    resolver.isExternal(nonSourceArtifact("external/foo+/a/b/c", "d/E.java"), localRepositories) shouldBe false
    resolver.isExternal(nonSourceArtifact("../foo+/a/b/c", "d/E.java"), localRepositories) shouldBe false

    resolver.isExternal(nonSourceArtifact("bazel-out/k8-fastbuild/bin", "external/bar+/lib.jar"), localRepositories) shouldBe true
    resolver.isExternal(nonSourceArtifact("bazel-out/k8-fastbuild/bin", "external/foo+/lib.jar"), localRepositories) shouldBe false
    resolver.isExternal(nonSourceArtifact("bazel-out/k8-fastbuild/bin", "some/internal/lib.jar"), localRepositories) shouldBe false
  }

  @Test
  fun pathResolutionWithExternalRepositories() {
    val resolver = newResolver()
    val localRepositories = LocalRepositoryMapping(mapOf("foo+" to Path("bar/baz")))

    assertEquals(Path("workspace/c/d/E.java"), resolver.resolve(sourceArtifact("", "c/d/E.java"), localRepositories))
    assertEquals(Path("workspace/bar/baz/a/b/E.java"), resolver.resolve(sourceArtifact("", "bar/baz/a/b/E.java"), localRepositories))
    assertEquals(Path("workspace/bar/baz/a/b/E.java"), resolver.resolve(sourceArtifact("external/foo+", "a/b/E.java"), localRepositories))

    assertEquals(Path("bazel-exec/a/b/c/d/E.java"), resolver.resolve(nonSourceArtifact("a/b","c/d/E.java"), localRepositories))
    assertEquals(Path("bazel-out/external/bar+/c/d/E.java"), resolver.resolve(nonSourceArtifact("external/bar+", "c/d/E.java"), localRepositories))
    assertEquals(Path("bazel-exec/bar/baz/c/d/E.java"), resolver.resolve(nonSourceArtifact("external/foo+", "c/d/E.java"), localRepositories))
  }
}

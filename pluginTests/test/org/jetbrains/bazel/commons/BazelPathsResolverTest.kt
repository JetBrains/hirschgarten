package org.jetbrains.bazel.commons

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.jetbrains.bazel.test.framework.testproject.testRepoMapping
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals

class BazelPathsResolverTest {

  private val bazelInfo = testProjectBazelInfo(workspaceRoot = Path("workspace"), outputBase = Path("bazel-out"), execRoot = Path("bazel-exec"))

  fun newResolver(): BazelPathsResolver = BazelPathsResolver(bazelInfo)

  fun sourceArtifact(root: String, relativePath: String) =
    ArtifactLocation.newBuilder().setRootPath(root).setRelativePath(relativePath).setIsSource(true).build()

  fun nonSourceArtifact(root: String, relativePath: String) =
    ArtifactLocation.newBuilder().setRootPath(root).setRelativePath(relativePath).build()

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

    assertEquals(Path("bazel-exec/a/b/c/d/E.java"), resolver.resolve(nonSourceArtifact("a/b", "c/d/E.java"), localRepositories))
    assertEquals(Path("bazel-out/external/bar+/c/d/E.java"),
                 resolver.resolve(nonSourceArtifact("external/bar+", "c/d/E.java"), localRepositories))
    assertEquals(Path("bazel-exec/bar/baz/c/d/E.java"),
                 resolver.resolve(nonSourceArtifact("external/foo+", "c/d/E.java"), localRepositories))
  }

  @Test
  fun directoryPathUsesRepoMapping() {
    val resolver = newResolver()
    val repoMapping = testRepoMapping(bazelInfo) { local("community+", "community", apparentName = "community") }

    assertEquals(
      Path("workspace/community/fleet/kernel"),
      resolver.toDirectoryPath(Label.parse("@community//fleet/kernel:...").assumeResolved(), repoMapping),
    )
    assertEquals(
      Path("workspace/community/fleet/kernel"),
      resolver.toDirectoryPath(Label.parse("@community//fleet/kernel/...").assumeResolved(), repoMapping),
    )
  }

  @Test
  fun repositoryPathUsesRepoMapping() {
    val resolver = newResolver()
    val repoMapping = testRepoMapping(bazelInfo) {
      local("local_repo+", "libs/local_repo", apparentName = "local_repo")
      external("remote_repo+", apparentName = "remote_repo")
    }

    fun repositoryPath(label: String) = resolver.toRepositoryPath(Label.parse(label).assumeResolved().repo, repoMapping)
    repositoryPath("//pkg:target") shouldBe Path("workspace")
    repositoryPath("@local_repo//:target") shouldBe Path("workspace/libs/local_repo")
    repositoryPath("@@local_repo+//sub:target") shouldBe Path("workspace/libs/local_repo")
    repositoryPath("@remote_repo//sub:target") shouldBe Path("bazel-out/external/remote_repo+")
  }

  @Test
  fun repositoryPathIsNormalized() {
    val resolver = newResolver()
    val repoMapping = testRepoMapping(bazelInfo) {
      local("dot_repo+", "./libs/dot_repo", apparentName = "dot_repo")
      local("parent_repo+", "libs/../libs/parent_repo", apparentName = "parent_repo")
    }

    fun repositoryPath(label: String) = resolver.toRepositoryPath(Label.parse(label).assumeResolved().repo, repoMapping)
    repositoryPath("@dot_repo//:target") shouldBe Path("workspace/libs/dot_repo")
    repositoryPath("@parent_repo//:target") shouldBe Path("workspace/libs/parent_repo")
  }

  @Test
  fun binRepositoryPathUsesCanonicalName() {
    val resolver = newResolver()
    val repoMapping = testRepoMapping(bazelInfo) { local("local_repo+", "libs/local_repo", apparentName = "local_repo") }

    fun binRepositoryPath(label: String) = resolver.toBinRepositoryPath(Label.parse(label).assumeResolved().repo, repoMapping)
    binRepositoryPath("//pkg:target") shouldBe bazelInfo.bazelBin
    binRepositoryPath("@local_repo//:target") shouldBe bazelInfo.bazelBin.resolve("external/local_repo+")
    binRepositoryPath("@@local_repo+//sub:target") shouldBe bazelInfo.bazelBin.resolve("external/local_repo+")
  }
}

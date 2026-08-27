package org.jetbrains.bazel.sync.workspace

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputRoot
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class DefaultOutputLocationResolverTest {

  private fun newResolver(): DefaultOutputLocationResolver =
    DefaultOutputLocationResolver(
      testBazelInfo(
        workspaceRoot = Path("workspace"),
        outputBase = Path("bazel-out-base"),
        execRoot = Path("bazel-exec"),
      ),
    )

  private val localOverride = BzlmodRepoMapping(
    canonicalRepoNameToLocalPath = mapOf(
      "foo+" to Path("bar/baz"),
      "abs+" to Path("/opt/checkout"),
    ),
    apparentRepoNameToCanonicalName = mapOf(),
    canonicalRepoNameToPath = mapOf(),
    nonLocalCanonicalRepoNames = setOf(),
  ).getLocalRepositories()

  @Test
  fun `resolves a workspace location against the workspace root`() {
    newResolver().resolve(OutputLocation.Workspace("c/d/E.java"), localOverride) shouldBe
      Path("workspace/c/d/E.java")
  }

  @Test
  fun `anchors a relative local override at the workspace root`() {
    // pins the semantics of BazelPathsResolverTest.pathResolutionWithExternalRepositories
    newResolver().resolve(OutputLocation.External("foo+", "a/b/E.java"), localOverride) shouldBe
      Path("workspace/bar/baz/a/b/E.java")
  }

  @Test
  fun `keeps an absolute local override`() {
    newResolver().resolve(OutputLocation.External("abs+", "a/b/E.java"), localOverride) shouldBe
      Path("/opt/checkout/a/b/E.java")
  }

  @Test
  fun `resolves a non-local external repo under the output base`() {
    newResolver().resolve(OutputLocation.External("bar+", "c/d/E.java"), localOverride) shouldBe
      Path("bazel-out-base/external/bar+/c/d/E.java")
  }

  @Test
  fun `resolves an output under the exec root`() {
    newResolver().resolve(OutputLocation.Output(OutputRoot.of(listOf("k8-fastbuild", "bin")), "c/d.jar"), localOverride) shouldBe
      Path("bazel-exec/bazel-out/k8-fastbuild/bin/c/d.jar")
  }

  @Test
  fun `resolves a host path as is`() {
    newResolver().resolve(OutputLocation.Host("/usr/bin/clang"), localOverride) shouldBe
      Path("/usr/bin/clang")
  }

  @Test
  fun `resolves a generated file of a locally overridden repo under the exec root`() {
    // a local override points at a source checkout, which never holds a generated file.
    // the sibling repository layout puts the file next to the exec root of the main repository.
    val resolver = DefaultOutputLocationResolver(
      testBazelInfo(
        workspaceRoot = Path("workspace"),
        outputBase = Path("bazel-out-base"),
        execRoot = Path("bazel-exec/_main"),
      ),
    )
    val generated = OutputLocation.External("foo+", "bazel-out/k8-fastbuild/bin/pkg/gen.h", siblingLayout = true)
    resolver.resolve(generated, localOverride) shouldBe
      Path("bazel-exec/foo+/bazel-out/k8-fastbuild/bin/pkg/gen.h")
  }

  @Test
  fun `ignores local overrides`() {
    newResolver().resolve(OutputLocation.External("foo+", "a/b/E.java"), localOverride = null) shouldBe
      Path("bazel-out-base/external/foo+/a/b/E.java")
  }
}

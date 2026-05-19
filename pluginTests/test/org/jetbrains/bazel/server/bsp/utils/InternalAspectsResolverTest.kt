package org.jetbrains.bazel.server.bsp.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class InternalAspectsResolverTest {
  @Test
  fun `should resolve label for bsp root at workspace root`() {
    // given
    val bspProjectRoot = "/Users/user/workspace/test-project"

    // when
    val internalAspectsResolver = createAspectsResolver(bspProjectRoot)
    val aspectLabel = internalAspectsResolver.resolveLabel("module.bzl%lang_aspect")

    // then
    aspectLabel shouldBe "//.bazelbsp/module.bzl%lang_aspect"
  }

  @Test
  fun `should resolve label for bsp root in subdirectory of workspace`() {
    // given
    val bspProjectRoot = "/Users/user/workspace/test-project/bsp-projects/test-project-bsp"

    // when
    val internalAspectsResolver = createAspectsResolver(bspProjectRoot)
    val aspectLabel = internalAspectsResolver.resolveLabel("module.bzl%lang_aspect")

    // then
    aspectLabel shouldBe "//.bazelbsp/module.bzl%lang_aspect"
  }

  private fun createAspectsResolver(bspProjectRoot: String): InternalAspectsResolver =
    InternalAspectsResolver(Paths.get(bspProjectRoot))
}

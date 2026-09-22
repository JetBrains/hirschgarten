package org.jetbrains.bsp.protocol

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.name

class OutputLocationPathsTest {

  private fun output(relativePath: String): OutputLocation.Output =
    OutputLocation.Output(OutputRoot.of(listOf("k8-fastbuild", "bin")), relativePath)

  private fun relativeKinds(relativePath: String): List<OutputLocation> = listOf(
    OutputLocation.Workspace(relativePath),
    output(relativePath),
    OutputLocation.External("foo+", relativePath),
    OutputLocation.External("llvm+", relativePath, siblingLayout = true),
  )

  private val host = OutputLocation.Host("/usr/bin/clang")

  @Test
  fun `reads the relative path of a relative kind`() {
    for (location in relativeKinds("a/b.py")) {
      location.relativeNioPath shouldBe Path("a/b.py")
    }
  }

  @Test
  fun `reads the absolute path of a host location`() {
    host.relativeNioPath shouldBe Path("/usr/bin/clang")
  }

  @Test
  fun `keeps an empty path empty and adds no leading separator`() {
    for (location in relativeKinds("")) {
      location.relativeNioPath shouldBe Path("")
      location.mapPath { it } shouldBe location
      location.mapPath { it.resolve("a") }.relativeNioPath shouldBe Path("a")
    }
  }

  @Test
  fun `writes a relative path with slashes`() {
    OutputLocation.Workspace("a").mapPath { it.resolve(Path("b", "c")) } shouldBe OutputLocation.Workspace("a/b/c")
  }

  @Test
  fun `keeps the root and the repository after a transform`() {
    output("a").mapPath { it.resolve("c") } shouldBe output("a/c")
    OutputLocation.External("foo+", "a", siblingLayout = true).mapPath { it.resolve("c") } shouldBe
      OutputLocation.External("foo+", "a/c", siblingLayout = true)
  }

  @Test
  fun `rejects an absolute result for a relative kind`() {
    for (location in relativeKinds("a/b.py")) {
      shouldThrow<IllegalArgumentException> { location.mapPath { Path("/abs/path") } }
    }
  }

  @Test
  fun `rejects a relative result for a host location`() {
    shouldThrow<IllegalArgumentException> { host.mapPath { Path("a/b") } }
  }
}

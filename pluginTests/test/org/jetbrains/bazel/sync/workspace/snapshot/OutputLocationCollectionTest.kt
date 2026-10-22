package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.jetbrains.bazel.test.framework.testproject.outputParser
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputRoot
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class OutputLocationCollectionTest {
  private val parser = testProjectBazelInfo(Path("/workspace")).outputParser()

  @Test
  fun `build of shared prefix paths yields all of them`() = runTest {
    val collection = OutputLocationCollectionBuilder.buildExecroot(
      listOf("src/a/x.h", "src/a/y.h", "src/b/z.h"),
      parser,
    )

    collection.getOutputLocations().toList() shouldContainExactlyInAnyOrder listOf(
      OutputLocation.Workspace("src/a/x.h"),
      OutputLocation.Workspace("src/a/y.h"),
      OutputLocation.Workspace("src/b/z.h"),
    )
    collection.isEmpty().shouldBeFalse()
  }

  @Test
  fun `build keeps every root separate`() = runTest {
    val collection = OutputLocationCollectionBuilder.buildExecroot(
      listOf(
        "src/a.h",
        "bazel-out/k8-fastbuild/bin/gen/a.h",
        "external/catch2+/src/a.h",
        "../catch2+/src/a.h",
        "/usr/include/a.h",
      ),
      parser,
    )

    collection.getOutputLocations().toList() shouldContainExactlyInAnyOrder listOf(
      OutputLocation.Workspace("src/a.h"),
      OutputLocation.Output(OutputRoot.of(listOf("k8-fastbuild", "bin")), "gen/a.h"),
      OutputLocation.External("catch2+", "src/a.h"),
      OutputLocation.External("catch2+", "src/a.h", siblingLayout = true),
      OutputLocation.Host("/usr/include/a.h"),
    )
  }

  @Test
  fun `equals ignore input order`() = runTest {
    val paths = listOf("src/a.h", "bazel-out/k8-fastbuild/bin/gen/a.h", "/usr/include/a.h")

    val a = OutputLocationCollectionBuilder.buildExecroot(paths, parser)
    val b = OutputLocationCollectionBuilder.buildExecroot(paths.reversed(), parser)

    (a == b).shouldBeTrue()
    a.hashCode() shouldBe b.hashCode()
  }

  @Test
  fun `equals tell sibling layout apart`() {
    val external = OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.External("catch2+", "src/a.h")))
    val sibling = OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.External("catch2+", "src/a.h", siblingLayout = true)))

    external shouldNotBe sibling
  }

  @Test
  fun `equals tell one changed element apart`() {
    val locations = List(100_000) { OutputLocation.Workspace("src/pkg${it / 100}/file$it.h") }

    val a = OutputLocationCollectionBuilder.ofLocations(locations)
    val reversed = OutputLocationCollectionBuilder.ofLocations(locations.reversed())
    val changed = OutputLocationCollectionBuilder.ofLocations(locations.dropLast(1) + OutputLocation.Workspace("src/other.h"))

    a shouldBe reversed
    a.hashCode() shouldBe reversed.hashCode()
    a shouldNotBe changed
  }

  @Test
  fun `empty collections are equal`() {
    OutputLocationCollectionBuilder.ofLocations(emptyList()) shouldBe OutputLocationCollection.EMPTY
  }
}

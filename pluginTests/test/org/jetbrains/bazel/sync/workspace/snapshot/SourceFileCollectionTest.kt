package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SourceFileCollectionTest {
  @TempDir
  lateinit var root: Path

  @Test
  fun `build of empty input returns empty set`() {
    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = emptyList())

    set.getFiles().toList() shouldBe emptyList()
    set.isEmpty().shouldBeTrue()
  }

  @Test
  fun `build of single relative path yields it once`() {
    val src = root.resolve("src/Main.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(src))

    set.getFiles().toList() shouldContainExactly listOf(src)
    set.isEmpty().shouldBeFalse()
  }

  @Test
  fun `build of multiple shared-prefix paths yields all of them`() {
    val paths = listOf(
      root.resolve("src/a/x.kt"),
      root.resolve("src/a/y.kt"),
      root.resolve("src/b/z.kt"),
    )

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = paths)

    set.getFiles().toList() shouldContainExactlyInAnyOrder paths
  }

  @Test
  fun `build of single deep chain yields the leaf path`() {
    val leaf = root.resolve("a/b/c/d/e.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(leaf))

    set.getFiles().toList() shouldContainExactly listOf(leaf)
  }

  @Test
  fun `build silently dedupes duplicate input paths`() {
    val src = root.resolve("src/Main.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(src, src))

    set.getFiles().toList() shouldContainExactly listOf(src)
  }

  @Test
  fun `build yields external path unchanged`(@TempDir other: Path) {
    val external = other.resolve("foo.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(external))

    set.getFiles().toList() shouldContainExactly listOf(external)
    set.isEmpty().shouldBeFalse()
  }

  @Test
  fun `build yields mixed relative and external paths`(@TempDir other: Path) {
    val relative = root.resolve("src/Main.kt")
    val external = other.resolve("foo.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(relative, external))

    set.getFiles().toList() shouldContainExactlyInAnyOrder listOf(relative, external)
  }

  @Test
  fun `build marks relativeRoot itself as terminal`() {
    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(root))

    set.getFiles().toList() shouldContainExactly listOf(root)
    set.isEmpty().shouldBeFalse()
  }

  @Test
  fun `build via no-root overload treats every path as external`(@TempDir a: Path, @TempDir b: Path) {
    val paths = listOf(
      a.resolve("foo.kt"),
      b.resolve("bar.kt"),
    )

    val set = SourceFileCollectionBuilder.build(paths = paths)

    set.getFiles().toList() shouldContainExactlyInAnyOrder paths
  }

  @Test
  fun `build round-trips many shared-prefix paths without dropping any`() {
    val paths = (1..20).map { root.resolve("pkg/sub/file$it.kt") }

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = paths)

    set.getFiles().toList() shouldContainExactlyInAnyOrder paths
  }

  @Test
  fun `build round-trips externals-only input`(@TempDir a: Path, @TempDir b: Path) {
    val externals = listOf(
      a.resolve("foo.kt"),
      b.resolve("bar.kt"),
    )

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = externals)

    set.getFiles().toList() shouldContainExactlyInAnyOrder externals
    set.isEmpty().shouldBeFalse()
  }

  @Test
  fun `build round-trips when trie has duplicate segment names under different parents`() {
    val paths = listOf(
      root.resolve("a/x/foo.kt"),
      root.resolve("b/x/foo.kt"),
    )

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = paths)

    set.getFiles().toList() shouldContainExactlyInAnyOrder paths
  }

  @Test
  fun `build round-trips when external and relative paths share segment names`(@TempDir other: Path) {
    val relative = root.resolve("src/Main.kt")
    val external = other.resolve("src/Other.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(relative, external))

    set.getFiles().toList() shouldContainExactlyInAnyOrder listOf(relative, external)
  }

  @Test
  fun `EMPTY is empty`() {
    SourceFileCollection.EMPTY.isEmpty().shouldBeTrue()
    SourceFileCollection.EMPTY.getFiles().toList() shouldBe emptyList()
  }

  @Test
  fun `set containing only relativeRoot reports not-empty`() {
    val set = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(root))

    set.isEmpty().shouldBeFalse()
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  fun `Windows drive letter paths round-trip`() {
    val winRoot = Path.of("C:\\workspace")
    val main = Path.of("C:\\workspace\\src\\Main.kt")
    val util = Path.of("C:\\workspace\\src\\Util.kt")
    // different drive: forces external bucket
    val external = Path.of("D:\\elsewhere\\Other.kt")

    val set = SourceFileCollectionBuilder.build(relativeRoot = winRoot, paths = listOf(main, util, external))

    set.getFiles().toList() shouldContainExactlyInAnyOrder listOf(main, util, external)
    set.isEmpty().shouldBeFalse()
  }

  @Test
  fun `equals and hashCode agree for sets built from identical input`() {
    val paths = listOf(root.resolve("src/a/x.kt"), root.resolve("src/b/y.kt"))

    val a = SourceFileCollectionBuilder.build(relativeRoot = root, paths = paths)
    val b = SourceFileCollectionBuilder.build(relativeRoot = root, paths = paths)

    (a == b).shouldBeTrue()
    a.hashCode() shouldBe b.hashCode()
  }

  @Test
  fun `equals ignores input order`(@TempDir other: Path) {
    val rel1 = root.resolve("src/a/x.kt")
    val rel2 = root.resolve("src/b/y.kt")
    val ext = other.resolve("foo.kt")

    val a = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(rel1, rel2, ext))
    val b = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(ext, rel2, rel1))

    (a == b).shouldBeTrue()
    a.hashCode() shouldBe b.hashCode()
  }

  @Test
  fun `equals distinguishes sets with different files`() {
    val a = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(root.resolve("src/a.kt")))
    val b = SourceFileCollectionBuilder.build(relativeRoot = root, paths = listOf(root.resolve("src/b.kt")))

    (a == b).shouldBeFalse()
  }

  @Test
  fun `two empty TrieBuildFileSets with same root are equal`() {
    val a = SourceFileCollectionBuilder.build(relativeRoot = root, paths = emptyList())
    val b = SourceFileCollectionBuilder.build(relativeRoot = root, paths = emptyList())

    (a == b).shouldBeTrue()
    a.hashCode() shouldBe b.hashCode()
  }
}

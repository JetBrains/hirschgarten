package org.jetbrains.bazel.sync.sharding

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.sync.sharding.BazelBuildTargetSharder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BazelBuildTargetSharderTest {
  private val targetA = Label.parse("//a:a")
  private val targetB = Label.parse("//b:b")
  private val targetC = Label.parse("//c:c")
  private val everything = Label.parse("//...")
  private val heavyPackages = Label.parse("//heavy/...")

  // An excluded target must reach Bazel as an exclusion, and never as a build request.

  @Test
  fun `batcher keeps an excluded target out of every batch`() {
    val collections =
      BazelBuildTargetSharder
        .shardTargetsToBatches(setOf(targetC, targetA, targetB), setOf(heavyPackages), 2)

    collections shouldHaveSize 2
    collections[0].values shouldContainExactly listOf(targetA, targetB)
    collections[1].values shouldContainExactly listOf(targetC)
    collections.forEach { it.excludedValues shouldContainExactly listOf(heavyPackages) }
  }

  @Test
  fun `an excluded pattern goes to every shard as an exclusion`() {
    val collections =
      BazelBuildTargetSharder
        .shardTargetsToBatches(listOf(everything, targetA), listOf(heavyPackages), 1)

    collections shouldHaveSize 2
    collections.flatMap { it.values } shouldContainExactly listOf(everything, targetA)
    collections.forEach { it.excludedValues shouldContainExactly listOf(heavyPackages) }
  }

  @Test
  fun `an exact excluded target disappears from the built targets`() {
    val collections =
      BazelBuildTargetSharder
        .shardTargetsToBatches(listOf(targetA, targetB), listOf(targetB, heavyPackages), 10)

    collections shouldHaveSize 1
    collections.single().values shouldContainExactly listOf(targetA)
    collections.single().excludedValues shouldContainExactly listOf(targetB, heavyPackages)
  }

  // A batch holds one repository only.

  @Test
  fun `two repositories never share a batch`() {
    val mainTarget = Label.parse("//a:a")
    val canonicalTarget = Label.parse("@@rules_java+//a:a")
    val apparentTarget = Label.parse("@rules_java//a:a")

    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(listOf(mainTarget, canonicalTarget, apparentTarget), softShardSize = 100)

    batches shouldHaveSize 3
    batches.forEach { it shouldHaveSize 1 }
    batches.flatten() shouldContainExactly listOf(mainTarget, canonicalTarget, apparentTarget)
  }

  @Test
  fun `a batch of one repository still fills up to the soft shard size`() {
    val mainTargets = listOf(Label.parse("//a:a"), Label.parse("//b:b"))
    val otherTargets = listOf(Label.parse("@@other//a:a"), Label.parse("@@other//b:b"))

    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(mainTargets + otherTargets, softShardSize = 4)

    batches shouldContainExactly listOf(mainTargets, otherTargets)
  }

  // A batch keeps the targets of one package together.

  @Test
  fun `the targets of one package stay in one batch`() {
    val packageTargets =
      listOf(Label.parse("//a:one"), Label.parse("//a:two"), Label.parse("//a:three"))

    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(packageTargets, softShardSize = 2)

    batches shouldHaveSize 1
    batches.single() shouldContainExactly packageTargets.sorted()
  }

  @Test
  fun `a package that passes the soft shard size does not take the next package`() {
    val bigPackage = listOf(Label.parse("//a:one"), Label.parse("//a:two"), Label.parse("//a:three"))
    val nextPackage = listOf(Label.parse("//b:one"))

    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(bigPackage + nextPackage, softShardSize = 2)

    batches shouldContainExactly listOf(bigPackage.sorted(), nextPackage)
  }

  @Test
  fun `a batch takes several small packages`() {
    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(listOf(targetA, targetB, targetC), softShardSize = 2)

    batches shouldContainExactly listOf(listOf(targetA, targetB), listOf(targetC))
  }

  @Test
  fun `a subpackage is a package of its own`() {
    val parentTarget = Label.parse("//a:a")
    val childTarget = Label.parse("//a/b:b")

    val batches =
      BazelBuildTargetSharder
        .calculateTargetBatches(listOf(parentTarget, childTarget), softShardSize = 1)

    batches shouldHaveSize 2
    batches.flatten() shouldContainExactly listOf(childTarget, parentTarget)
  }

  @Test
  fun `no target gives no batch`() {
    BazelBuildTargetSharder.calculateTargetBatches(emptyList(), softShardSize = 10) shouldHaveSize 0
  }

  @Test
  fun `a shard size of zero is rejected`() {
    assertThrows<IllegalArgumentException> {
      BazelBuildTargetSharder.calculateTargetBatches(listOf(targetA), softShardSize = 0)
    }
  }
}

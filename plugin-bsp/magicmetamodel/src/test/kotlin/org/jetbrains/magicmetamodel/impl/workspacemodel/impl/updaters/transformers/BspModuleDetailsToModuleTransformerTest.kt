@file:Suppress("LongMethod")
package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BspModuleDetailsToModuleTransformer.transform(bspModuleDetails) tests")
class BspModuleDetailsToModuleTransformerTest {

  @Test
  fun `should return no modules for no bsp module details items`() {
    // given
    val emptyBspModuleDetails = listOf<BspModuleDetails>()

    // when
    val modules = BspModuleDetailsToModuleTransformer.transform(emptyBspModuleDetails)

    // then
    modules shouldBe emptyList()
  }

  @Test
  fun `should return module with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"

    val targetName = "//target1"
    val targetId = BuildTargetIdentifier(targetName)

    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities()
    )

    val dependencySourceItem1 = DependencySourcesItem(
      targetId,
      listOf(dependencySource1, dependencySource2)
    )

    val bspModuleDetails = BspModuleDetails(
      target = target,
      allTargetsIds = listOf(
        BuildTargetIdentifier("//target1"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("//target4"),
      ),
      dependencySources = listOf(dependencySourceItem1),
      type = "JAVA_MODULE",
    )

    // when
    val module = BspModuleDetailsToModuleTransformer.transform(bspModuleDetails)

    // then
    val expectedModule = Module(
      name = targetName,
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target2",
        ),
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = listOf(
        LibraryDependency(
          libraryName = dependencySource1,
        ),
        LibraryDependency(
          libraryName = dependencySource2,
        ),
      )
    )

    shouldBeIgnoringDependenciesOrder(module, expectedModule)
  }

  @Test
  fun `should return multiple modules with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"

    val target1Name = "//target1"
    val target1Id = BuildTargetIdentifier(target1Name)

    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities()
    )

    val dependencySourceItem1 = DependencySourcesItem(
      target1Id,
      listOf(dependencySource1, dependencySource2)
    )

    val bspModuleDetails1 = BspModuleDetails(
      target = target1,
      allTargetsIds = listOf(
        BuildTargetIdentifier("//target1"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("//target4"),
      ),
      dependencySources = listOf(dependencySourceItem1),
      type = "JAVA_MODULE",
    )

    val target2Name = "//target2"
    val target2Id = BuildTargetIdentifier(target2Name)

    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities()
    )

    val dependencySourceItem2 = DependencySourcesItem(
      target2Id,
      listOf(dependencySource1)
    )

    val bspModuleDetails2 = BspModuleDetails(
      target = target2,
      allTargetsIds = listOf(
        BuildTargetIdentifier("//target1"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("//target4"),
      ),
      dependencySources = listOf(dependencySourceItem2),
      type = "JAVA_MODULE",
    )

    val bspModuleDetails = listOf(bspModuleDetails1, bspModuleDetails2)

    // when
    val modules = BspModuleDetailsToModuleTransformer.transform(bspModuleDetails)

    // then
    val expectedModule1 = Module(
      name = target1Name,
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target2",
        ),
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = listOf(
        LibraryDependency(
          libraryName = dependencySource1,
        ),
        LibraryDependency(
          libraryName = dependencySource2,
        ),
      )
    )

    val expectedModule2 = Module(
      name = target2Name,
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = listOf(
        LibraryDependency(
          libraryName = dependencySource1,
        ),
      )
    )

    modules shouldContainExactlyInAnyOrder Pair(
      listOf(expectedModule1, expectedModule2),
      this::shouldBeIgnoringDependenciesOrder
    )
  }

  // TODO
  private infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>
  ) {
    val expectedValues = expectedWithAssertion.first
    val assertion = expectedWithAssertion.second

    this shouldHaveSize expectedValues.size

    this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
  }

  private fun shouldBeIgnoringDependenciesOrder(actual: Module, expected: Module) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.modulesDependencies shouldContainExactlyInAnyOrder expected.modulesDependencies
    actual.librariesDependencies shouldContainExactlyInAnyOrder expected.librariesDependencies
  }
}

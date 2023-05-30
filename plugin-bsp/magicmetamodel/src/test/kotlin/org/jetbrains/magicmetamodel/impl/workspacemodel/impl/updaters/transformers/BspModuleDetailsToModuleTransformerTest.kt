package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
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
    val modules = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(emptyBspModuleDetails)

    // then
    modules shouldBe emptyList()
  }

  @Test
  fun `should return module with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0-sources.jar"

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
    val javacOptions = JavacOptionsItem(
      targetId,
      emptyList(),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
      ),
      "file:///compiler/output.jar",
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
      javacOptions = javacOptions,
    )

    // when
    val module = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

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
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        ),
        LibraryDependency(
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
        ),
      )
    )

    shouldBeIgnoringDependenciesOrder(module, expectedModule)
  }

  @Test
  fun `should return multiple modules with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0-sources.jar"

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
    val javacOptionsItem1 = JavacOptionsItem(
      target1Id,
      emptyList(),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar"
      ),
      "file:///compiler/output1.jar",
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
      javacOptions = javacOptionsItem1,
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
    val javacOptionsItem2 = JavacOptionsItem(
      target2Id,
      emptyList(),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
      ),
      "file:///compiler/output2.jar",
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
      javacOptions = javacOptionsItem2,
    )

    val bspModuleDetails = listOf(bspModuleDetails1, bspModuleDetails2)

    // when
    val modules = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

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
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        ),
        LibraryDependency(
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
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
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        ),
      )
    )

    modules shouldContainExactlyInAnyOrder (listOf(expectedModule1, expectedModule2) to { actual, expected -> shouldBeIgnoringDependenciesOrder(actual, expected) } )
  }

  @Test
  fun `should rename module using the given provider`() {
    // given
    val targetName = "//target1"
    val targetId = BuildTargetIdentifier(targetName)

    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      emptyList(),
      BuildTargetCapabilities()
    )

    val javacOptions = JavacOptionsItem(
      targetId,
      emptyList(),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
      ),
      "file:///compiler/output.jar",
    )

    val bspModuleDetails = BspModuleDetails(
      target = target,
      allTargetsIds = listOf(
        BuildTargetIdentifier("//target1"),
        BuildTargetIdentifier("//target2"),
      ),
      dependencySources = emptyList(),
      type = "JAVA_MODULE",
      javacOptions = javacOptions,
    )

    // when
    val transformer = BspModuleDetailsToModuleTransformer { "${it.uri}${it.uri}" }
    val module = transformer.transform(bspModuleDetails)

    // then
    module.name shouldBe "$targetName$targetName"
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

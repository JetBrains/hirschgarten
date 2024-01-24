package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
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
  fun `should return java module with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0-sources.jar"

    val targetName = "//target1"
    val targetId = BuildTargetIdentifier(targetName)

    val target = BuildTarget(
      targetId,
      emptyList(),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem1 = DependencySourcesItem(
      targetId,
      listOf(dependencySource1, dependencySource2),
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
      dependencySources = listOf(dependencySourceItem1),
      javacOptions = javacOptions,
      pythonOptions = null,
      type = "JAVA_MODULE",
      moduleDependencies = listOf(
        "//target2",
        "//target3",
      ),
      libraryDependencies = null,
      scalacOptions = null,
    )

    // when
    val module = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

    // then
    val expectedModule = GenericModuleInfo(
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
          false,
        ),
        LibraryDependency(
          libraryName = "BSP: file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
          false,
        ),
      ),
    )

    shouldBeIgnoringDependenciesOrder(module, expectedModule)
  }

  @Test
  fun `should return module with associates when specified in BspModuleDetails`() {
    // given
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
      BuildTargetCapabilities(),
    )
    val bspModuleDetails = BspModuleDetails(
      target = target,
      dependencySources = listOf(),
      type = "JAVA_MODULE",
      javacOptions = null,
      pythonOptions = null,
      associates = listOf(
        "//target4",
        "//target5",
      ),
      moduleDependencies = listOf(
        "//target2",
        "//target3",
      ),
      libraryDependencies = listOf(
        "@maven//:test",
      ),
      scalacOptions = null,
    )
    // when
    val module = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

    // then
    val expectedModule = GenericModuleInfo(
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
        LibraryDependency("@maven//:test", true),
      ),
      associates = listOf(
        ModuleDependency(
          moduleName = "//target4",
        ),
        ModuleDependency(
          moduleName = "//target5",
        ),
      ),
    )

    shouldBeIgnoringDependenciesOrder(module, expectedModule)
  }

  @Test
  fun `should return python module with dependencies to other targets`() {
    // given
    val targetName = "//target1"
    val targetId = BuildTargetIdentifier(targetName)

    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem1 = DependencySourcesItem(
      targetId,
      emptyList(),
    )
    val pythonOptions = PythonOptionsItem(
      targetId,
      emptyList(),
    )

    val bspModuleDetails = BspModuleDetails(
      target = target,
      dependencySources = listOf(dependencySourceItem1),
      type = "PYTHON_MODULE",
      javacOptions = null,
      pythonOptions = pythonOptions,
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(
        "//target2",
        "//target3",
      ),
      scalacOptions = null,
    )

    // when
    val module = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

    // then
    val expectedModule = GenericModuleInfo(
      name = targetName,
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target2",
        ),
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = emptyList(),
      associates = emptyList(),
    )

    shouldBeIgnoringDependenciesOrder(module, expectedModule)
  }

  @Test
  fun `should return multiple java modules with dependencies to other targets and libraries`() {
    // given
    val dependencySource1 = "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0-sources.jar"

    val target1Name = "//target1"
    val target1Id = BuildTargetIdentifier(target1Name)

    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem1 = DependencySourcesItem(
      target1Id,
      listOf(dependencySource1, dependencySource2),
    )
    val javacOptionsItem1 = JavacOptionsItem(
      target1Id,
      emptyList(),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/1.0.0/test2-1.0.0.jar",
      ),
      "file:///compiler/output1.jar",
    )

    val bspModuleDetails1 = BspModuleDetails(
      target = target1,
      dependencySources = listOf(dependencySourceItem1),
      javacOptions = javacOptionsItem1,
      type = "JAVA_MODULE",
      pythonOptions = null,
      moduleDependencies = listOf(
        "//target2",
        "//target3",
      ),
      libraryDependencies = null,
      scalacOptions = null,
    )

    val target2Name = "//target2"
    val target2Id = BuildTargetIdentifier(target2Name)

    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("@maven//:test"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem2 = DependencySourcesItem(
      target2Id,
      listOf(dependencySource1),
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
      dependencySources = listOf(dependencySourceItem2),
      javacOptions = javacOptionsItem2,
      type = "JAVA_MODULE",
      moduleDependencies = listOf(
        "//target3",
      ),
      libraryDependencies = null,
      pythonOptions = null,
      scalacOptions = null,
    )

    val bspModuleDetails = listOf(bspModuleDetails1, bspModuleDetails2)
    // when
    val modules = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

    // then
    val expectedModule1 = GenericModuleInfo(
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
      ),
    )

    val expectedModule2 = GenericModuleInfo(
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
      ),
    )

    modules shouldContainExactlyInAnyOrder (listOf(expectedModule1, expectedModule2) to { actual, expected -> shouldBeIgnoringDependenciesOrder(actual, expected) })
  }

  @Test
  fun `should return multiple python modules with dependencies to other targets`() {
    // given
    val target1Name = "//target1"
    val target1Id = BuildTargetIdentifier(target1Name)

    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("python"),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem1 = DependencySourcesItem(
      target1Id,
      emptyList(),
    )
    val pythonOptionsItem1 = PythonOptionsItem(
      target1Id,
      emptyList(),
    )

    val bspModuleDetails1 = BspModuleDetails(
      target = target1,
      dependencySources = listOf(dependencySourceItem1),
      type = "PYTHON_MODULE",
      javacOptions = null,
      pythonOptions = pythonOptionsItem1,
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(
        "//target2",
        "//target3",
      ),
      scalacOptions = null,
    )

    val target2Name = "//target2"
    val target2Id = BuildTargetIdentifier(target2Name)

    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      listOf("python"),
      listOf(
        BuildTargetIdentifier("//target3"),
      ),
      BuildTargetCapabilities(),
    )

    val dependencySourceItem2 = DependencySourcesItem(
      target2Id,
      emptyList(),
    )
    val pythonOptionsItem2 = PythonOptionsItem(
      target2Id,
      emptyList(),
    )
    val bspModuleDetails2 = BspModuleDetails(
      target = target2,
      dependencySources = listOf(dependencySourceItem2),
      type = "PYTHON_MODULE",
      javacOptions = null,
      pythonOptions = pythonOptionsItem2,
      libraryDependencies = emptyList(),
      moduleDependencies = listOf(
        "//target3",
      ),
      scalacOptions = null,
    )

    val bspModuleDetails = listOf(bspModuleDetails1, bspModuleDetails2)

    // when
    val modules = BspModuleDetailsToModuleTransformer(DefaultModuleNameProvider).transform(bspModuleDetails)

    // then
    val expectedModule1 = GenericModuleInfo(
      name = target1Name,
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target2",
        ),
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = emptyList(),
    )

    val expectedModule2 = GenericModuleInfo(
      name = target2Name,
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(
        ModuleDependency(
          moduleName = "//target3",
        ),
      ),
      librariesDependencies = emptyList(),
    )

    modules shouldContainExactlyInAnyOrder (listOf(expectedModule1, expectedModule2) to { actual, expected -> shouldBeIgnoringDependenciesOrder(actual, expected) })
  }

  @Test
  fun `should rename module using the given provider`() {
    // given
    val targetName = "//target1"
    val targetId = BuildTargetIdentifier(targetName)

    val target = BuildTarget(
      targetId,
      emptyList(),
      listOf("java"),
      emptyList(),
      BuildTargetCapabilities(),
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
      dependencySources = emptyList(),
      javacOptions = javacOptions,
      type = "JAVA_MODULE",
      moduleDependencies = emptyList(),
      libraryDependencies = emptyList(),
      pythonOptions = null,
      scalacOptions = null,
    )
    // when
    val transformer = BspModuleDetailsToModuleTransformer(
      moduleNameProvider = { "$it$it" },
    )
    val module = transformer.transform(bspModuleDetails)

    // then
    module.name shouldBe "$targetName$targetName"
  }

  private infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>,
  ) {
    val expectedValues = expectedWithAssertion.first
    val assertion = expectedWithAssertion.second

    this shouldHaveSize expectedValues.size

    this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
  }

  private fun shouldBeIgnoringDependenciesOrder(actual: GenericModuleInfo, expected: GenericModuleInfo) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.modulesDependencies shouldContainExactlyInAnyOrder expected.modulesDependencies
    actual.librariesDependencies shouldContainExactlyInAnyOrder expected.librariesDependencies
  }
}

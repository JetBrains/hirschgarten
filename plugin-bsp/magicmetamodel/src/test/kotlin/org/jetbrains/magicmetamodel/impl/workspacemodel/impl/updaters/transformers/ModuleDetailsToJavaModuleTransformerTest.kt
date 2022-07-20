@file:Suppress("LongMethod")
package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import kotlin.io.path.toPath

@DisplayName("ModuleDetailsToJavaModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToJavaModuleTransformerTest {

  @Test
  fun `should return no java modules roots for no modules details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val javaModules = ModuleDetailsToJavaModuleTransformer.transform(emptyModulesDetails)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should return single java module for single module details`() {
    // given
    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget.baseDirectory = "file:///root/dir/"
    buildTarget.dataKind = BuildTargetDataKind.JVM
    buildTarget.data = JvmBuildTarget("file:///java/home", "11")

    val sourcesItem = SourcesItem(
      buildTargetId,
      listOf(
        SourceItem("file:///root/dir/example/package/one/File1.java", SourceItemKind.FILE, false),
        SourceItem("file:///root/dir/example/package/one/File2.java", SourceItemKind.FILE, false),
        SourceItem("file:///root/dir/another/example/package/", SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem.roots = listOf("file:///root/dir/")

    val resourceFilePath = Files.createTempFile("resource", "File.txt")
    val resourcesItem = ResourcesItem(
      buildTargetId,
      listOf(resourceFilePath.toUri().toString())
    )

    val dependencySourcesItem = DependencySourcesItem(
      buildTargetId,
      listOf(
        "file:///library/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///library/test2/2.0.0/test2-2.0.0-sources.jar",
      )
    )

    val moduleDetails = ModuleDetails(
      target = buildTarget,
      allTargetsIds = listOf(
        BuildTargetIdentifier("module1"),
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
      sources = listOf(sourcesItem),
      resources = listOf(resourcesItem),
      dependenciesSources = listOf(dependencySourcesItem),
    )

    // when
    val javaModule = ModuleDetailsToJavaModuleTransformer.transform(moduleDetails)

    // then
    val expectedModule = Module(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = listOf(
        LibraryDependency("file:///library/test1/1.0.0/test1-1.0.0-sources.jar"),
        LibraryDependency("file:///library/test2/2.0.0/test2-2.0.0-sources.jar"),
      )
    )

    val expectedBaseDirContentRoot = ContentRoot(URI.create("file:///root/dir/").toPath())

    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/one/").toPath(),
      generated = false,
      packagePrefix = "example.package.one",
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package",
    )

    val expectedJavaResourceRoot1 = JavaResourceRoot(
      resourcePath = resourceFilePath.parent,
    )

    val expectedLibrary1 = Library(
      displayName = "file:///library/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///library/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///library/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary2 = Library(
      displayName = "file:///library/test2/2.0.0/test2-2.0.0-sources.jar",
      sourcesJar = "jar:///library/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///library/test2/2.0.0/test2-2.0.0.jar!/",
    )

    val expectedJavaModule = JavaModule(
      module = expectedModule,
      baseDirContentRoot = expectedBaseDirContentRoot,
      sourceRoots = listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2),
      resourceRoots = listOf(expectedJavaResourceRoot1),
      libraries = listOf(expectedLibrary1, expectedLibrary2),
//      sdk =
    )

    validateJavaModule(javaModule, expectedJavaModule)
  }

  @Test
  fun `should return multiple java module for multiple module details`() {
    // given
    val buildTargetId1 = BuildTargetIdentifier("module1")
    val buildTarget1 = BuildTarget(
      buildTargetId1,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget1.baseDirectory = "file:///root/dir/"

    val sourcesItem1 = SourcesItem(
      buildTargetId1,
      listOf(
        SourceItem("file:///root/dir/example/package/one/File1.java", SourceItemKind.FILE, false),
        SourceItem("file:///root/dir/example/package/one/File2.java", SourceItemKind.FILE, false),
        SourceItem("file:///root/dir/another/example/package/", SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem1.roots = listOf("file:///root/dir/")

    val resourceFilePath11 = Files.createTempFile("resource", "File1.txt")
    val resourceFilePath12 = Files.createTempFile("resource", "File2.txt")
    val resourcesItem1 = ResourcesItem(
      buildTargetId1,
      listOf(
        resourceFilePath11.toUri().toString(),
        resourceFilePath12.toUri().toString(),
      )
    )

    val dependencySourcesItem1 = DependencySourcesItem(
      buildTargetId1,
      listOf(
        "file:///library/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///library/test2/2.0.0/test2-2.0.0-sources.jar",
      )
    )

    val moduleDetails1 = ModuleDetails(
      target = buildTarget1,
      allTargetsIds = listOf(
        BuildTargetIdentifier("module1"),
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
      sources = listOf(sourcesItem1),
      resources = listOf(resourcesItem1),
      dependenciesSources = listOf(dependencySourcesItem1),
    )

    val buildTargetId2 = BuildTargetIdentifier("module2")
    val buildTarget2 = BuildTarget(
      buildTargetId2,
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget2.baseDirectory = "file:///another/root/dir/"

    val sourcesItem2 = SourcesItem(
      buildTargetId2,
      listOf(
        SourceItem("file:///another/root/dir/example/package/", SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem2.roots = listOf("file:///another/root/dir/")

    val resourceDirPath21 = Files.createTempDirectory("resource")
    val resourcesItem2 = ResourcesItem(
      buildTargetId2,
      listOf(resourceDirPath21.toUri().toString())
    )

    val dependencySourcesItem2 = DependencySourcesItem(
      buildTargetId2,
      listOf("file:///library/test1/1.0.0/test1-1.0.0-sources.jar")
    )

    val moduleDetails2 = ModuleDetails(
      target = buildTarget2,
      allTargetsIds = listOf(
        BuildTargetIdentifier("module1"),
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
      sources = listOf(sourcesItem2),
      resources = listOf(resourcesItem2),
      dependenciesSources = listOf(dependencySourcesItem2),
    )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)

    // when
    val javaModules = ModuleDetailsToJavaModuleTransformer.transform(modulesDetails)

    // then
    val expectedModule1 = Module(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = listOf(
        LibraryDependency("file:///library/test1/1.0.0/test1-1.0.0-sources.jar"),
        LibraryDependency("file:///library/test2/2.0.0/test2-2.0.0-sources.jar"),
      )
    )

    val expectedBaseDirContentRoot1 = ContentRoot(URI.create("file:///root/dir/").toPath())

    val expectedJavaSourceRoot11 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/one/").toPath(),
      generated = false,
      packagePrefix = "example.package.one",
    )
    val expectedJavaSourceRoot12 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package",
    )

    val expectedJavaResourceRoot11 = JavaResourceRoot(
      resourcePath = resourceFilePath11.parent,
    )

    val expectedLibrary11 = Library(
      displayName = "file:///library/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///library/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///library/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary12 = Library(
      displayName = "file:///library/test2/2.0.0/test2-2.0.0-sources.jar",
      sourcesJar = "jar:///library/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///library/test2/2.0.0/test2-2.0.0.jar!/",
    )

    val expectedJavaModule1 = JavaModule(
      module = expectedModule1,
      baseDirContentRoot = expectedBaseDirContentRoot1,
      sourceRoots = listOf(expectedJavaSourceRoot11, expectedJavaSourceRoot12),
      resourceRoots = listOf(expectedJavaResourceRoot11),
      libraries = listOf(expectedLibrary11, expectedLibrary12),
    )

    val expectedModule2 = Module(
      name = "module2",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module3")),
      librariesDependencies = listOf(LibraryDependency("file:///library/test1/1.0.0/test1-1.0.0-sources.jar")),
    )

    val expectedBaseDirContentRoot2 = ContentRoot(URI.create("file:///another/root/dir/").toPath())

    val expectedJavaSourceRoot21 = JavaSourceRoot(
      sourceDir = URI.create("file:///another/root/dir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package",
    )

    val expectedJavaResourceRoot21 = JavaResourceRoot(
      resourcePath = resourceDirPath21,
    )

    val expectedLibrary21 = Library(
      displayName = "file:///library/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///library/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///library/test1/1.0.0/test1-1.0.0.jar!/",
    )

    val expectedJavaModule2 = JavaModule(
      module = expectedModule2,
      baseDirContentRoot = expectedBaseDirContentRoot2,
      sourceRoots = listOf(expectedJavaSourceRoot21),
      resourceRoots = listOf(expectedJavaResourceRoot21),
      libraries = listOf(expectedLibrary21),
    )

    javaModules shouldContainExactlyInAnyOrder Pair(
      listOf(expectedJavaModule1, expectedJavaModule2), this::validateJavaModule
    )
  }

  private infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>
  ) {
    val expectedValues = expectedWithAssertion.first
    val assertion = expectedWithAssertion.second

    this shouldHaveSize expectedValues.size

    this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
  }

  private fun validateJavaModule(actual: JavaModule, expected: JavaModule) {
    validateModule(actual.module, expected.module)

    actual.baseDirContentRoot shouldBe expected.baseDirContentRoot
    actual.sourceRoots shouldContainExactlyInAnyOrder expected.sourceRoots
    actual.resourceRoots shouldContainExactlyInAnyOrder expected.resourceRoots
    actual.libraries shouldContainExactlyInAnyOrder expected.libraries
  }

  // TODO
  private fun validateModule(actual: Module, expected: Module) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.modulesDependencies shouldContainExactlyInAnyOrder expected.modulesDependencies
    actual.librariesDependencies shouldContainExactlyInAnyOrder expected.librariesDependencies
  }
}

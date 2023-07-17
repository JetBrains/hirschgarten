@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.PythonBuildTarget
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.google.gson.JsonObject
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.GenericSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonLibrary
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonResourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonSdkInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@DisplayName("ModuleDetailsToPythonModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToPythonModuleTransformerTest {

  val projectBasePath = Path("")

  @Test
  fun `should return no python modules roots for no modules details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val pythonModules = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(emptyModulesDetails)

    // then
    pythonModules shouldBe emptyList()
  }

  @Test
  fun `should return single python module for single module details`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val version = "3"
    val originalName = "fake-original-name"

    val sdkInfoJsonObject = JsonObject()
    sdkInfoJsonObject.addProperty("version", version)
    sdkInfoJsonObject.addProperty("originalName", originalName)

    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      listOf("library"),
      listOf("python"),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget.baseDirectory = projectRoot.toUri().toString()
    buildTarget.dataKind = BuildTargetDataKind.PYTHON
    buildTarget.data = sdkInfoJsonObject

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".py")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".py")
    file2APath.toFile().deleteOnExit()

    val packageB1Path = createTempDirectory(projectRoot, "packageB1")
    packageB1Path.toFile().deleteOnExit()
    val packageB2Path = createTempDirectory(packageB1Path, "packageB2")
    packageB2Path.toFile().deleteOnExit()
    val dir1BPath = createTempDirectory(packageB2Path, "dir1")
    dir1BPath.toFile().deleteOnExit()

    val sourcesItem = SourcesItem(
      buildTargetId,
      listOf(
        SourceItem(file1APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(file2APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(dir1BPath.toUri().toString(), SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem.roots = listOf(projectRoot.toUri().toString())

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val resourcesItem = ResourcesItem(
      buildTargetId,
      listOf(resourceFilePath.toUri().toString()),
    )

    val dependencySourcesItem = DependencySourcesItem(
      buildTargetId,
      emptyList(),
    )
    val pythonOptionsItem = PythonOptionsItem(
      buildTargetId,
      listOf("opt1", "opt2", "opt3"),
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
      javacOptions = null,
      pythonOptions = pythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
    )

    // when
    val pythonModule = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(moduleDetails)

    // then
    val expectedModule = Module(
      name = "module1",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot1 = GenericSourceRoot(
      sourcePath = file1APath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedGenericSourceRoot2 = GenericSourceRoot(
      sourcePath = file2APath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedGenericSourceRoot3 = GenericSourceRoot(
      sourcePath = dir1BPath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )

    val expectedPythonResourceRoot1 = PythonResourceRoot(
      resourcePath = resourceFilePath.parent,
    )

    val expectedPythonModule = PythonModule(
      module = expectedModule,
      sourceRoots = listOf(expectedGenericSourceRoot1, expectedGenericSourceRoot2, expectedGenericSourceRoot3),
      resourceRoots = listOf(expectedPythonResourceRoot1),
      libraries = emptyList(),
      sdkInfo = PythonSdkInfo(version = version, originalName = originalName),
    )

    validatePythonModule(pythonModule, expectedPythonModule)
  }

  @Test
  fun `should return multiple python modules for multiple module details`() {
    // given
    val projectRoot = createTempDirectory("project")
    projectRoot.toFile().deleteOnExit()

    val module1Root = createTempDirectory(projectBasePath, "module1").toAbsolutePath()
    module1Root.toFile().deleteOnExit()

    val buildTargetId1 = BuildTargetIdentifier("module1")
    val buildTargetId2 = BuildTargetIdentifier("module2")
    val buildTargetId3 = BuildTargetIdentifier("module3")

    val buildTarget1 = BuildTarget(
      buildTargetId1,
      listOf("library"),
      listOf("python"),
      listOf(buildTargetId2, buildTargetId2),
      BuildTargetCapabilities()
    )
    buildTarget1.baseDirectory = module1Root.toUri().toString()

    val packageA1Path = createTempDirectory(module1Root, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".py")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".py")
    file2APath.toFile().deleteOnExit()

    val packageB1Path = createTempDirectory(module1Root, "packageB1")
    packageB1Path.toFile().deleteOnExit()
    val packageB2Path = createTempDirectory(packageB1Path, "packageB2")
    packageB2Path.toFile().deleteOnExit()
    val dir1BPath = createTempDirectory(packageB2Path, "dir1")
    dir1BPath.toFile().deleteOnExit()

    val sourcesItem1 = SourcesItem(
      buildTargetId1,
      listOf(
        SourceItem(file1APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(file2APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(dir1BPath.toUri().toString(), SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem1.roots = listOf(module1Root.toUri().toString())

    val resourceFilePath11 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File1.txt")
    resourceFilePath11.toFile().deleteOnExit()
    val resourceFilePath12 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File2.txt")
    resourceFilePath12.toFile().deleteOnExit()
    val resourcesItem1 = ResourcesItem(
      buildTargetId1,
      listOf(
        resourceFilePath11.toUri().toString(),
        resourceFilePath12.toUri().toString(),
      )
    )

    val dependencySourcesItem1 = DependencySourcesItem(
      buildTargetId1,
      listOf("file:///example/externalSource1.py"),
    )
    val target1PythonOptionsItem = PythonOptionsItem(
      buildTargetId1,
      listOf("opt1.1", "opt1.2", "opt1.3"),
    )

    val moduleDetails1 = ModuleDetails(
      target = buildTarget1,
      allTargetsIds = listOf(
        buildTargetId1,
        buildTargetId2,
        buildTargetId3
      ),
      sources = listOf(sourcesItem1),
      resources = listOf(resourcesItem1),
      dependenciesSources = listOf(dependencySourcesItem1),
      javacOptions = null,
      pythonOptions = target1PythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(
        buildTargetId2,
        buildTargetId3
      )
    )

    val module2Root = createTempDirectory(projectBasePath, "module2").toAbsolutePath()
    module2Root.toFile().deleteOnExit()

    val buildTarget2 = BuildTarget(
      buildTargetId2,
      listOf("test"),
      listOf("python"),
      listOf(buildTargetId3),
      BuildTargetCapabilities()
    )
    buildTarget2.baseDirectory = module2Root.toUri().toString()

    val packageC1Path = createTempDirectory(module2Root, "packageC1")
    packageC1Path.toFile().deleteOnExit()
    val packageC2Path = createTempDirectory(packageC1Path, "packageC2")
    packageC2Path.toFile().deleteOnExit()
    val dir1CPath = createTempDirectory(packageC2Path, "dir1")
    dir1CPath.toFile().deleteOnExit()

    val sourcesItem2 = SourcesItem(
      buildTargetId2,
      listOf(
        SourceItem(dir1CPath.toUri().toString(), SourceItemKind.DIRECTORY, false),
      )
    )
    sourcesItem2.roots = listOf(module2Root.toUri().toString())

    val resourceDirPath21 = Files.createTempDirectory(projectBasePath.toAbsolutePath(), "resource")
    val resourcesItem2 = ResourcesItem(
      buildTargetId2,
      listOf(resourceDirPath21.toUri().toString())
    )

    val dependencySourcesItem2 = DependencySourcesItem(
      buildTargetId2,
      emptyList()
    )
    val target2PythonOptionsItem = PythonOptionsItem(
      buildTargetId2,
      listOf("opt2.1", "opt2.2"),
    )

    val moduleDetails2 = ModuleDetails(
      target = buildTarget2,
      allTargetsIds = listOf(
        buildTargetId1,
        buildTargetId2,
        buildTargetId3
      ),
      sources = listOf(sourcesItem2),
      resources = listOf(resourcesItem2),
      dependenciesSources = listOf(dependencySourcesItem2),
      javacOptions = null,
      pythonOptions = target2PythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(buildTargetId3)
    )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)

    // when
    val pythonModules = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(modulesDetails)

    // then
    val expectedModule1 = Module(
      name = "module1",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot11 = GenericSourceRoot(
      sourcePath = file1APath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedGenericSourceRoot12 = GenericSourceRoot(
      sourcePath = file2APath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedGenericSourceRoot13 = GenericSourceRoot(
      sourcePath = dir1BPath,
      rootType = "python-source",
      targetId = BuildTargetIdentifier("module1"),
    )

    val expectedPythonResourceRoot11 = PythonResourceRoot(
      resourcePath = resourceFilePath11.parent,
    )

    val expectedPythonModule1 = PythonModule(
      module = expectedModule1,
      sourceRoots = listOf(expectedGenericSourceRoot11, expectedGenericSourceRoot12, expectedGenericSourceRoot13),
      resourceRoots = listOf(expectedPythonResourceRoot11),
      libraries = listOf(PythonLibrary(dependencySourcesItem1.sources.first())),
      sdkInfo = null,
    )

    val expectedModule2 = Module(
      name = "module2",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot21 = GenericSourceRoot(
      sourcePath = dir1CPath,
      rootType = "python-test",
      targetId = BuildTargetIdentifier("module2"),
    )

    val expectedPythonResourceRoot21 = PythonResourceRoot(
      resourcePath = resourceDirPath21,
    )

    val expectedPythonModule2 = PythonModule(
      module = expectedModule2,
      sourceRoots = listOf(expectedGenericSourceRoot21),
      resourceRoots = listOf(expectedPythonResourceRoot21),
      libraries = emptyList(),
      sdkInfo = null,
    )

    pythonModules shouldContainExactlyInAnyOrder Pair(
      listOf(expectedPythonModule1, expectedPythonModule2), this::validatePythonModule
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

  private fun validatePythonModule(actual: PythonModule, expected: PythonModule) {
    validateModule(actual.module, expected.module)

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

class ExtractPythonBuildTargetTest {
  @Test
  fun `extractPythonBuildTarget should return PythonBuildTarget successfully when given non-null sdk information`() {
    // given
    val version = "3"
    val interpreter = "/fake/path/to/test/interpreter"
    val sdkInfoJsonObject = JsonObject()
    sdkInfoJsonObject.addProperty("version", version)
    sdkInfoJsonObject.addProperty("interpreter", interpreter)

    val buildTarget = buildDummyTarget()
    buildTarget.dataKind = BuildTargetDataKind.PYTHON
    buildTarget.data = sdkInfoJsonObject

    // when
    val extractedPythonBuildTarget = extractPythonBuildTarget(buildTarget)

    // then
    extractedPythonBuildTarget shouldBe PythonBuildTarget(version, interpreter)
  }

  @Test
  fun `extractPythonBuildTarget should return null when given null sdk information`() {
    // given
    val buildTarget = buildDummyTarget()

    // when
    val extractedPythonBuildTarget = extractPythonBuildTarget(buildTarget)

    // then
    extractedPythonBuildTarget shouldBe null
  }

  private fun buildDummyTarget(): BuildTarget {
    val buildTarget = BuildTarget(
      BuildTargetIdentifier("target"),
      listOf("tag1", "tag2"),
      listOf("language1"),
      listOf(BuildTargetIdentifier("dep1"), BuildTargetIdentifier("dep2")),
      BuildTargetCapabilities(true, false, true, true)
    )
    buildTarget.displayName = "target name"
    buildTarget.baseDirectory = "/base/dir"
    return buildTarget
  }
}

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
import org.jetbrains.bsp.utils.extractPythonBuildTarget
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonLibrary
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@DisplayName("ModuleDetailsToPythonModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToPythonModuleTransformerTest {
  val projectBasePath = Path("")
  private val defaultPythonVersion = "PY3"
  private val hasDefaultPythonInterpreter = true

  @Test
  fun `should return no python modules roots for no modules details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val pythonModules = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath, hasDefaultPythonInterpreter).transform(emptyModulesDetails)

    // then
    pythonModules shouldBe emptyList()
  }

  @Test
  fun `should return single python module for single module details`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val version = "PY3"
    val interpreter = "/path/to/interpreter"

    val sdkInfoJsonObject = JsonObject()
    sdkInfoJsonObject.addProperty("version", version)
    sdkInfoJsonObject.addProperty("interpreter", interpreter)

    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      listOf("library"),
      listOf("python"),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
      ),
      BuildTargetCapabilities(),
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
      ),
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
      sources = listOf(sourcesItem),
      resources = listOf(resourcesItem),
      dependenciesSources = listOf(dependencySourcesItem),
      javacOptions = null,
      scalacOptions = null,
      pythonOptions = pythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(
        "module2",
        "module3",
      ),
      defaultJdkName = null,
    )

    // when
    val pythonModule = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath, hasDefaultPythonInterpreter).transform(moduleDetails)

    // then
    val expectedModule = GenericModuleInfo(
      name = "module1",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot1 = GenericSourceRoot(
      sourcePath = file1APath,
      rootType = "python-source",
    )
    val expectedGenericSourceRoot2 = GenericSourceRoot(
      sourcePath = file2APath,
      rootType = "python-source",
    )
    val expectedGenericSourceRoot3 = GenericSourceRoot(
      sourcePath = dir1BPath,
      rootType = "python-source",
    )

    val expectedResourceRoot1 = ResourceRoot(
      resourcePath = resourceFilePath.parent,
      rootType = "",
    )

    val expectedPythonSdkInfo = PythonSdkInfo(version = version, originalName = buildTargetId.uri)

    val expectedPythonModule = PythonModule(
      module = expectedModule,
      sourceRoots = listOf(expectedGenericSourceRoot1, expectedGenericSourceRoot2, expectedGenericSourceRoot3),
      resourceRoots = listOf(expectedResourceRoot1),
      libraries = emptyList(),
      sdkInfo = expectedPythonSdkInfo,
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
      BuildTargetCapabilities(),
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
      ),
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
      ),
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
      sources = listOf(sourcesItem1),
      resources = listOf(resourcesItem1),
      dependenciesSources = listOf(dependencySourcesItem1),
      javacOptions = null,
      scalacOptions = null,
      pythonOptions = target1PythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(
        buildTargetId2.uri,
        buildTargetId3.uri,
      ),
      defaultJdkName = null,
    )

    val module2Root = createTempDirectory(projectBasePath, "module2").toAbsolutePath()
    module2Root.toFile().deleteOnExit()

    val buildTarget2 = BuildTarget(
      buildTargetId2,
      listOf("test"),
      listOf("python"),
      listOf(buildTargetId3),
      BuildTargetCapabilities(),
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
      ),
    )
    sourcesItem2.roots = listOf(module2Root.toUri().toString())

    val resourceDirPath21 = Files.createTempDirectory(projectBasePath.toAbsolutePath(), "resource")
    val resourcesItem2 = ResourcesItem(
      buildTargetId2,
      listOf(resourceDirPath21.toUri().toString()),
    )

    val dependencySourcesItem2 = DependencySourcesItem(
      buildTargetId2,
      emptyList(),
    )
    val target2PythonOptionsItem = PythonOptionsItem(
      buildTargetId2,
      listOf("opt2.1", "opt2.2"),
    )

    val moduleDetails2 = ModuleDetails(
      target = buildTarget2,
      sources = listOf(sourcesItem2),
      resources = listOf(resourcesItem2),
      dependenciesSources = listOf(dependencySourcesItem2),
      javacOptions = null,
      scalacOptions = null,
      pythonOptions = target2PythonOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = listOf(buildTargetId3.uri),
      defaultJdkName = null,
    )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)

    // when
    val pythonModules = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath, hasDefaultPythonInterpreter).transform(modulesDetails)

    // then
    val expectedModule1 = GenericModuleInfo(
      name = "module1",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot11 = GenericSourceRoot(
      sourcePath = file1APath,
      rootType = "python-source",
    )
    val expectedGenericSourceRoot12 = GenericSourceRoot(
      sourcePath = file2APath,
      rootType = "python-source",
    )
    val expectedGenericSourceRoot13 = GenericSourceRoot(
      sourcePath = dir1BPath,
      rootType = "python-source",
    )

    val expectedResourceRoot11 = ResourceRoot(
      resourcePath = resourceFilePath11.parent,
      rootType = "",
    )

    val expectedPythonSdk1 = PythonSdkInfo(version = defaultPythonVersion, originalName = "${buildTargetId1.uri}-detected")

    val expectedPythonModule1 = PythonModule(
      module = expectedModule1,
      sourceRoots = listOf(expectedGenericSourceRoot11, expectedGenericSourceRoot12, expectedGenericSourceRoot13),
      resourceRoots = listOf(expectedResourceRoot11),
      libraries = listOf(PythonLibrary(dependencySourcesItem1.sources)),
      sdkInfo = expectedPythonSdk1,
    )

    val expectedModule2 = GenericModuleInfo(
      name = "module2",
      type = "PYTHON_MODULE",
      modulesDependencies = listOf(ModuleDependency("module3")),
      librariesDependencies = emptyList(),
    )

    val expectedGenericSourceRoot21 = GenericSourceRoot(
      sourcePath = dir1CPath,
      rootType = "python-test",
    )

    val expectedResourceRoot21 = ResourceRoot(
      resourcePath = resourceDirPath21,
      rootType = "",
    )

    val expectedPythonSdk2 = PythonSdkInfo(version = defaultPythonVersion, originalName = "${buildTargetId2.uri}-detected")

    val expectedPythonModule2 = PythonModule(
      module = expectedModule2,
      sourceRoots = listOf(expectedGenericSourceRoot21),
      resourceRoots = listOf(expectedResourceRoot21),
      libraries = emptyList(),
      sdkInfo = expectedPythonSdk2,
    )

    pythonModules shouldContainExactlyInAnyOrder Pair(
      listOf(expectedPythonModule1, expectedPythonModule2), this::validatePythonModule,
    )
  }

  @Test
  fun `should set default sdk info if there's no sdk provided`() {
    fun emptyModuleDetails(target: BuildTarget) =
      ModuleDetails(
        target = target,
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = null,
        scalacOptions = null,
        pythonOptions = null,
        outputPathUris = emptyList(),
        libraryDependencies = null,
        moduleDependencies = emptyList(),
        defaultJdkName = null,
      )

    fun emptyExpectedModule(name: String, sdkInfo: PythonSdkInfo): PythonModule {
      val expectedModule = GenericModuleInfo(
        name = name,
        type = "PYTHON_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
      )

      return PythonModule(
        module = expectedModule,
        sourceRoots = emptyList(),
        resourceRoots = emptyList(),
        libraries = emptyList(),
        sdkInfo = sdkInfo,
      )
    }

    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val version1 = "PY2"
    val interpreter1: String? = null

    val version2: String? = null
    val interpreter2 = "/path/to/interpreter"

    val sdkInfoJsonObject1 = JsonObject()
    sdkInfoJsonObject1.addProperty("version", version1)
    sdkInfoJsonObject1.addProperty("interpreter", interpreter1)

    val sdkInfoJsonObject2 = JsonObject()
    sdkInfoJsonObject2.addProperty("version", version2)
    sdkInfoJsonObject2.addProperty("interpreter", interpreter2)

    val sdkInfoJsonObject3 = JsonObject()

    val buildTarget1Id = BuildTargetIdentifier("module1")
    val buildTarget1 = BuildTarget(
      buildTarget1Id,
      listOf("library"),
      listOf("python"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    buildTarget1.baseDirectory = projectRoot.toUri().toString()
    buildTarget1.dataKind = BuildTargetDataKind.PYTHON
    buildTarget1.data = sdkInfoJsonObject1

    val moduleDetails1 = emptyModuleDetails(buildTarget1)

    val buildTarget2Id = BuildTargetIdentifier("module2")
    val buildTarget2 = BuildTarget(
      buildTarget2Id,
      listOf("library"),
      listOf("python"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    buildTarget2.baseDirectory = projectRoot.toUri().toString()
    buildTarget2.dataKind = BuildTargetDataKind.PYTHON
    buildTarget2.data = sdkInfoJsonObject2

    val moduleDetails2 = emptyModuleDetails(buildTarget2)

    val buildTarget3Id = BuildTargetIdentifier("module3")
    val buildTarget3 = BuildTarget(
      buildTarget3Id,
      listOf("library"),
      listOf("python"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    buildTarget3.baseDirectory = projectRoot.toUri().toString()
    buildTarget3.dataKind = BuildTargetDataKind.PYTHON
    buildTarget3.data = sdkInfoJsonObject3

    val moduleDetails3 = emptyModuleDetails(buildTarget3)

    val modulesDetails = listOf(moduleDetails1, moduleDetails2, moduleDetails3)

    // when
    val pythonModules = ModuleDetailsToPythonModuleTransformer(DefaultModuleNameProvider, projectBasePath, hasDefaultPythonInterpreter).transform(modulesDetails)

    // then
    val expectedSdkInfo1 = PythonSdkInfo(version = version1, "${buildTarget1.id.uri}-detected")
    val expectedSdkInfo2 = PythonSdkInfo(version = defaultPythonVersion, "${buildTarget2.id.uri}-detected")
    val expectedSdkInfo3 = PythonSdkInfo(version = defaultPythonVersion, "${buildTarget3.id.uri}-detected")

    val expectedModule1 = emptyExpectedModule("module1", expectedSdkInfo1)
    val expectedModule2 = emptyExpectedModule("module2", expectedSdkInfo2)
    val expectedModule3 = emptyExpectedModule("module3", expectedSdkInfo3)

    val expectedModules = listOf(expectedModule1, expectedModule2, expectedModule3)

    pythonModules shouldContainExactlyInAnyOrder Pair(
      expectedModules, this::validatePythonModule,
    )
  }

  private infix fun <T, C : Collection<T>, E> C.shouldContainExactlyInAnyOrder(
    expectedWithAssertion: Pair<Collection<E>, (T, E) -> Unit>,
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
    actual.sdkInfo shouldBe expected.sdkInfo
  }

  private fun validateModule(actual: GenericModuleInfo, expected: GenericModuleInfo) {
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
    extractedPythonBuildTarget shouldBe PythonBuildTarget().also {
      it.version = version
      it.interpreter = interpreter
    }
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
      BuildTargetCapabilities().also {
        it.canCompile = true
        it.canTest = false
        it.canRun = true
        it.canDebug = true
      }
    )
    buildTarget.displayName = "target name"
    buildTarget.baseDirectory = "/base/dir"
    return buildTarget
  }
}

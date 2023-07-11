package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.google.gson.JsonObject
import com.intellij.openapi.module.ModuleTypeId
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.name

class ModuleDetailsToDummyJavaModuleTransformerHACKTest {

  val projectBasePath = Path("")

  @Test
  fun `should return no dummy java modules for no module details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val javaModules = ModuleDetailsToDummyJavaModulesTransformerHACK(projectBasePath).transform(emptyModulesDetails)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should return single dummy java module for single source root found from module details`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name

    val javaHome = "/fake/path/to/local_jdk"
    val javaVersion = "11"

    val jdkInfoJsonObject = JsonObject()
    jdkInfoJsonObject.addProperty("javaVersion", javaVersion)
    jdkInfoJsonObject.addProperty("javaHome", javaHome)

    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      listOf("library"),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget.baseDirectory = projectRoot.toUri().toString()
    buildTarget.dataKind = BuildTargetDataKind.JVM
    buildTarget.data = jdkInfoJsonObject

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = kotlin.io.path.createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = kotlin.io.path.createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val sourcesItem = SourcesItem(
      buildTargetId,
      listOf(
        SourceItem(file1APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(file2APath.toUri().toString(), SourceItemKind.FILE, false),
      )
    )

    val sourceRoot = projectRoot.toUri()

    sourcesItem.roots = listOf(sourceRoot.toString())
    val javacOptionsItem = JavacOptionsItem(buildTargetId, listOf(), listOf(), "file:///compiler/output.jar")

    val moduleDetails = ModuleDetails(
      target = buildTarget,
      allTargetsIds = listOf(),
      sources = listOf(sourcesItem),
      resources = listOf(),
      dependenciesSources = listOf(),
      javacOptions = javacOptionsItem,
      outputPathUris = emptyList(),
      libraryDependencies = null,
      moduleDependencies = emptyList()
    )

    // when
    val javaModules = ModuleDetailsToDummyJavaModulesTransformerHACK(projectBasePath).transform(moduleDetails)

    // then
    val expectedModule = Module(
      name = projectRootName,
      type = ModuleTypeId.JAVA_MODULE,
      modulesDependencies = listOf(),
      librariesDependencies = listOf()
    )

    val expectedJavaModule = JavaModule(
      module = expectedModule,
      baseDirContentRoot = ContentRoot(url=projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot.toAbsolutePath(),
          generated = true,
          packagePrefix = "",
          rootType = ModuleDetailsToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
          targetId = BuildTargetIdentifier(projectRootName)
        )
      ),
      resourceRoots = listOf(),
      moduleLevelLibraries  = emptyList(),
      compilerOutput = null,
      jvmJdkInfo = null,
      kotlinAddendum = null
    )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  @Test
  fun `should return multiple dummy java modules for single source root found from module details`() {
    // given
    val projectRoot1 = createTempDirectory(projectBasePath, "module1")
    projectRoot1.toFile().deleteOnExit()
    val projectRoot1Name = projectRoot1.name

    val projectRoot2 = createTempDirectory(projectBasePath, "module1")
    projectRoot2.toFile().deleteOnExit()
    val projectRoot2Name = projectRoot2.name

    val javaHome = "/fake/path/to/local_jdk"
    val javaVersion = "11"

    val jdkInfoJsonObject = JsonObject()
    jdkInfoJsonObject.addProperty("javaVersion", javaVersion)
    jdkInfoJsonObject.addProperty("javaHome", javaHome)

    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      listOf("library"),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget.baseDirectory = projectRoot1.toUri().toString()
    buildTarget.dataKind = BuildTargetDataKind.JVM
    buildTarget.data = jdkInfoJsonObject

    val packageA1Path = createTempDirectory(projectRoot1, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = kotlin.io.path.createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = kotlin.io.path.createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val sourcesItem = SourcesItem(
      buildTargetId,
      listOf(
        SourceItem(file1APath.toUri().toString(), SourceItemKind.FILE, false),
        SourceItem(file2APath.toUri().toString(), SourceItemKind.FILE, false),
      )
    )

    val sourceRoot1 = projectRoot1.toUri()
    val sourceRoot2 = projectRoot2.toUri()

    sourcesItem.roots = listOf(sourceRoot1.toString(), sourceRoot2.toString())
    val javacOptionsItem = JavacOptionsItem(buildTargetId, listOf(), listOf(), "file:///compiler/output.jar")

    val moduleDetails = ModuleDetails(
      target = buildTarget,
      allTargetsIds = listOf(),
      sources = listOf(sourcesItem),
      resources = listOf(),
      dependenciesSources = listOf(),
      javacOptions = javacOptionsItem,
      outputPathUris = emptyList(),
      moduleDependencies = emptyList(),
      libraryDependencies = null
    )

    // when
    val javaModules = ModuleDetailsToDummyJavaModulesTransformerHACK(projectBasePath).transform(moduleDetails)

    // then
    val expectedModule1 = Module(
      name = projectRoot1Name,
      type = ModuleTypeId.JAVA_MODULE,
      modulesDependencies = listOf(),
      librariesDependencies = listOf()
    )

    val expectedJavaModule1 = JavaModule(
      module = expectedModule1,
      baseDirContentRoot = ContentRoot(url=projectRoot1.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot1.toAbsolutePath(),
          generated = true,
          packagePrefix = "",
          rootType = ModuleDetailsToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
          targetId = BuildTargetIdentifier(projectRoot1Name)
        )
      ),
      resourceRoots = listOf(),
      moduleLevelLibraries = emptyList(),
      compilerOutput = null,
      jvmJdkInfo = null,
      kotlinAddendum = null
    )

    val expectedModule2 = Module(
      name = projectRoot2Name,
      type = ModuleTypeId.JAVA_MODULE,
      modulesDependencies = listOf(),
      librariesDependencies = listOf()
    )

    val expectedJavaModule2 = JavaModule(
      module = expectedModule2,
      baseDirContentRoot = ContentRoot(url=projectRoot2.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot2.toAbsolutePath(),
          generated = true,
          packagePrefix = "",
          rootType = ModuleDetailsToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
          targetId = BuildTargetIdentifier(projectRoot2Name)
        )
      ),
      resourceRoots = listOf(),
      moduleLevelLibraries = emptyList(),
      compilerOutput = null,
      jvmJdkInfo = null,
      kotlinAddendum = null
    )

    javaModules shouldContainExactlyInAnyOrder (listOf(expectedJavaModule1, expectedJavaModule2) to { actual, expected -> validateJavaModule(actual, expected) })
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
    actual.moduleLevelLibraries shouldContainExactlyInAnyOrder expected.moduleLevelLibraries
  }

  private fun validateModule(actual: Module, expected: Module) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.modulesDependencies shouldContainExactlyInAnyOrder expected.modulesDependencies
    actual.librariesDependencies shouldContainExactlyInAnyOrder expected.librariesDependencies
  }
}

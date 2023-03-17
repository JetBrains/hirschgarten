@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.*
import com.google.gson.JsonObject
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.*
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JvmJdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

@DisplayName("ModuleDetailsToJavaModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToJavaModuleTransformerTest {

  val projectBasePath = Path("")

  @Test
  fun `should return no java modules roots for no modules details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val javaModules = ModuleDetailsToJavaModuleTransformer(null, projectBasePath).transform(emptyModulesDetails)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should return single java module for single module details`() {
    // given
    val projectRoot = createTempDirectory("project")
    projectRoot.toFile().deleteOnExit()

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
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
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

    val resourceFilePath = createTempFile("resource", "File.txt")
    val resourcesItem = ResourcesItem(
      buildTargetId,
      listOf(resourceFilePath.toUri().toString())
    )

    val dependencySourcesItem = DependencySourcesItem(
      buildTargetId,
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
      )
    )
    val javacOptionsItem = JavacOptionsItem(
      buildTargetId,
      listOf("opt1", "opt2", "opt3"),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar"
      ),
      "file:///compiler/output.jar",
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
      javacOptions = javacOptionsItem,
    )

    // when
    val javaModule = ModuleDetailsToJavaModuleTransformer(null, projectBasePath).transform(moduleDetails)

    // then
    val expectedModule = Module(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = listOf(
        LibraryDependency("BSP: test1-1.0.0"),
        LibraryDependency("BSP: test2-2.0.0"),
      )
    )

    val expectedBaseDirContentRoot = ContentRoot(projectRoot)

    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourcePath = file1APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourcePath = file2APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedJavaSourceRoot3 = JavaSourceRoot(
      sourcePath = dir1BPath,
      generated = false,
      packagePrefix = "${packageB1Path.name}.${packageB2Path.name}.${dir1BPath.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )

    val expectedJavaResourceRoot1 = JavaResourceRoot(
      resourcePath = resourceFilePath.parent,
    )

    val expectedLibrary1 = Library(
      displayName = "BSP: test1-1.0.0",
      sourcesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary2 = Library(
      displayName = "BSP: test2-2.0.0",
      sourcesJar = "jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/",
    )

    val expectedJavaModule = JavaModule(
      module = expectedModule,
      baseDirContentRoot = expectedBaseDirContentRoot,
      sourceRoots = listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2, expectedJavaSourceRoot3),
      resourceRoots = listOf(expectedJavaResourceRoot1),
      libraries = listOf(expectedLibrary1, expectedLibrary2),
      compilerOutput = Path("/compiler/output.jar"),
      jvmJdkInfo = JvmJdkInfo(javaVersion = javaVersion, javaHome = javaHome),
    )

    validateJavaModule(javaModule, expectedJavaModule)
  }

  @Test
  fun `should return multiple java module for multiple module details`() {
    // given
    val projectRoot = createTempDirectory("project")
    projectRoot.toFile().deleteOnExit()

    val module1Root = createTempDirectory("module1")
    module1Root.toFile().deleteOnExit()

    val buildTargetId1 = BuildTargetIdentifier("module1")
    val buildTarget1 = BuildTarget(
      buildTargetId1,
      listOf("library"),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities()
    )
    buildTarget1.baseDirectory = module1Root.toUri().toString()

    val packageA1Path = createTempDirectory(module1Root, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
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

    val resourceFilePath11 = createTempFile("resource", "File1.txt")
    val resourceFilePath12 = createTempFile("resource", "File2.txt")
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
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
      )
    )
    val target1JavacOptionsItem = JavacOptionsItem(
      buildTargetId1,
      listOf("opt1.1", "opt1.2", "opt1.3"),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      ),
      "file:///compiler/output1.jar",
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
      javacOptions = target1JavacOptionsItem,
    )

    val module2Root = createTempDirectory("module2")
    module2Root.toFile().deleteOnExit()

    val buildTargetId2 = BuildTargetIdentifier("module2")
    val buildTarget2 = BuildTarget(
      buildTargetId2,
      listOf("test"),
      emptyList(),
      listOf(
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
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

    val resourceDirPath21 = Files.createTempDirectory("resource")
    val resourcesItem2 = ResourcesItem(
      buildTargetId2,
      listOf(resourceDirPath21.toUri().toString())
    )

    val dependencySourcesItem2 = DependencySourcesItem(
      buildTargetId2,
      listOf("file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar")
    )
    val target2JavacOptionsItem = JavacOptionsItem(
      buildTargetId2,
      listOf("opt2.1", "opt2.2"),
      listOf("file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar"),
      "file:///compiler/output2.jar",
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
      javacOptions = target2JavacOptionsItem,
    )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)

    // when
    val javaModules = ModuleDetailsToJavaModuleTransformer(null, projectBasePath).transform(modulesDetails)

    // then
    val expectedModule1 = Module(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module2"), ModuleDependency("module3")),
      librariesDependencies = listOf(
        LibraryDependency("BSP: test1-1.0.0"),
        LibraryDependency("BSP: test2-2.0.0"),
      )
    )

    val expectedBaseDirContentRoot1 = ContentRoot(module1Root)

    val expectedJavaSourceRoot11 = JavaSourceRoot(
      sourcePath = file1APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedJavaSourceRoot12 = JavaSourceRoot(
      sourcePath = file2APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )
    val expectedJavaSourceRoot13 = JavaSourceRoot(
      sourcePath = dir1BPath,
      generated = false,
      packagePrefix = "${packageB1Path.name}.${packageB2Path.name}.${dir1BPath.name}",
      rootType = "java-source",
      targetId = BuildTargetIdentifier("module1"),
    )

    val expectedJavaResourceRoot11 = JavaResourceRoot(
      resourcePath = resourceFilePath11.parent,
    )

    val expectedLibrary11 = Library(
      displayName = "BSP: test1-1.0.0",
      sourcesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary12 = Library(
      displayName = "BSP: test2-2.0.0",
      sourcesJar = "jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/",
    )

    val expectedJavaModule1 = JavaModule(
      module = expectedModule1,
      baseDirContentRoot = expectedBaseDirContentRoot1,
      sourceRoots = listOf(expectedJavaSourceRoot11, expectedJavaSourceRoot12, expectedJavaSourceRoot13),
      resourceRoots = listOf(expectedJavaResourceRoot11),
      libraries = listOf(expectedLibrary11, expectedLibrary12),
      compilerOutput = Path("/compiler/output1.jar"),
      jvmJdkInfo = null,
    )

    val expectedModule2 = Module(
      name = "module2",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(ModuleDependency("module3")),
      librariesDependencies = listOf(LibraryDependency("BSP: test1-1.0.0")),
    )

    val expectedBaseDirContentRoot2 = ContentRoot(module2Root)

    val expectedJavaSourceRoot21 = JavaSourceRoot(
      sourcePath = dir1CPath,
      generated = false,
      packagePrefix = "${packageC1Path.name}.${packageC2Path.name}.${dir1CPath.name}",
      rootType = "java-test",
      targetId = BuildTargetIdentifier("module2"),
    )

    val expectedJavaResourceRoot21 = JavaResourceRoot(
      resourcePath = resourceDirPath21,
    )

    val expectedLibrary21 = Library(
      displayName = "BSP: test1-1.0.0",
      sourcesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/",
    )

    val expectedJavaModule2 = JavaModule(
      module = expectedModule2,
      baseDirContentRoot = expectedBaseDirContentRoot2,
      sourceRoots = listOf(expectedJavaSourceRoot21),
      resourceRoots = listOf(expectedJavaResourceRoot21),
      libraries = listOf(expectedLibrary21),
      compilerOutput = Path("/compiler/output2.jar"),
      jvmJdkInfo = null,
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

class ExtractJvmBuildTargetTest {
  @Test
  fun `extractJvmBuildTarget should return JvmBuildTarget successfully when given non-null jdk information`() {
    // given
    val javaVersion = "17"
    val javaHome = "/fake/path/to/test/local_jdk"
    val jdkInfoJsonObject = JsonObject()
    jdkInfoJsonObject.addProperty("javaVersion", javaVersion)
    jdkInfoJsonObject.addProperty("javaHome", javaHome)

    val buildTarget = buildDummyTarget()
    buildTarget.dataKind = BuildTargetDataKind.JVM
    buildTarget.data = jdkInfoJsonObject

    // when
    val extractedJvmBuildTarget = extractJvmBuildTarget(buildTarget)

    // then
    extractedJvmBuildTarget shouldBe JvmBuildTarget(javaHome, javaVersion)
  }

  @Test
  fun `extractJvmBuildTarget should return null when given null jdk information`() {
    // given
    val buildTarget = buildDummyTarget()

    // when
    val extractedJvmBuildTarget = extractJvmBuildTarget(buildTarget)

    // then
    extractedJvmBuildTarget shouldBe null
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

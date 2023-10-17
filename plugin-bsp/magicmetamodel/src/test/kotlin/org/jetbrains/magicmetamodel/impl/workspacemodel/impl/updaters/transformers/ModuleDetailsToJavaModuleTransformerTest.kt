package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.google.gson.JsonObject
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.KotlinBuildTarget
import org.jetbrains.bsp.utils.extractJvmBuildTarget
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.workspace.model.constructors.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name
import kotlin.io.path.toPath

@DisplayName("ModuleDetailsToJavaModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToJavaModuleTransformerTest {
  val projectBasePath: Path = Path("").toAbsolutePath()

  @Test
  fun `should return no java modules roots for no modules details`() {
    // given
    val emptyModulesDetails = listOf<ModuleDetails>()

    // when
    val javaModules =
      ModuleDetailsToJavaModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(emptyModulesDetails)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should return single java module for single module details`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
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
      listOf("java"),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities(),
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
      ),
    )
    sourcesItem.roots = listOf(projectRoot.toUri().toString())

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val resourcesItem = ResourcesItem(
      buildTargetId,
      listOf(resourceFilePath.toUri().toString()),
    )

    val dependencySourcesItem = DependencySourcesItem(
      buildTargetId,
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
      ),
    )
    val javacOptionsItem = JavacOptionsItem(
      buildTargetId,
      listOf("opt1", "opt2", "opt3"),
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      ),
      "file:///compiler/output.jar",
    )

    val outputPathUris = listOf("file:///output/file1.out", "file:///output/file2.out")

    val moduleDetails = ModuleDetails(
      target = buildTarget,
      sources = listOf(sourcesItem),
      resources = listOf(resourcesItem),
      dependenciesSources = listOf(dependencySourcesItem),
      javacOptions = javacOptionsItem,
      pythonOptions = null,
      outputPathUris = outputPathUris,
      libraryDependencies = null,
      moduleDependencies = listOf(
        "module2",
        "module3",
      ),
      scalacOptions = null
    )
    // when
    val javaModule =
      ModuleDetailsToJavaModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(moduleDetails)

    // then
    val expectedModule = GenericModuleInfo(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency("module2"),
        ModuleDependency("module3"),
        ModuleDependency(calculateDummyJavaModuleName(projectRoot, projectBasePath)),
      ),
      librariesDependencies = listOf(
        LibraryDependency("BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar"),
        LibraryDependency("BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar"),
      ),
    )

    val expectedBaseDirContentRoot = ContentRoot(
      path = projectRoot.toAbsolutePath(),
      excludedPaths = outputPathUris.map { URI.create(it).toPath() },
    )

    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourcePath = file1APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourcePath = file2APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot3 = JavaSourceRoot(
      sourcePath = dir1BPath,
      generated = false,
      packagePrefix = "${packageB1Path.name}.${packageB2Path.name}.${dir1BPath.name}",
      rootType = "java-source",
    )

    val expectedResourceRoot1 = ResourceRoot(
      resourcePath = resourceFilePath,
    )

    val expectedLibrary1 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/"),
    )
    val expectedLibrary2 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/"),
    )

    val expectedJavaModule = JavaModule(
      genericModuleInfo = expectedModule,
      baseDirContentRoot = expectedBaseDirContentRoot,
      sourceRoots = listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2, expectedJavaSourceRoot3),
      resourceRoots = listOf(expectedResourceRoot1),
      compilerOutput = Path("/compiler/output.jar"),
      jvmJdkName = "${projectBasePath.name}-$javaVersion",
      kotlinAddendum = null,
      moduleLevelLibraries = listOf(
        expectedLibrary1,
        expectedLibrary2,
      ),
    )

    validateJavaModule(javaModule, expectedJavaModule)
  }

  @Test
  fun `should return java module with associates as dependencies when specified`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val javaHome = "/fake/path/to/local_jdk"
    val javaVersion = "11"

    val kotlinBuildTarget = KotlinBuildTarget(
      languageVersion = "1.8",
      apiVersion = "1.8",
      kotlincOptions = listOf(),
      associates = listOf(
        BuildTargetIdentifier("//target4"),
        BuildTargetIdentifier("//target5"),
      ),
      jvmBuildTarget = JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = javaVersion
      }
    )

    val buildTargetId = BuildTargetIdentifier("module1")
    val buildTarget = BuildTarget(
      buildTargetId,
      listOf("library"),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities(),
    )
    buildTarget.baseDirectory = projectRoot.toUri().toString()
    buildTarget.dataKind = "kotlin"
    buildTarget.data = kotlinBuildTarget

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val moduleDetails = ModuleDetails(
      target = buildTarget,
      sources = listOf(),
      resources = listOf(),
      dependenciesSources = listOf(),
      javacOptions = null,
      pythonOptions = null,
      outputPathUris = listOf(),
      moduleDependencies = listOf(
        "module2",
        "module3",
      ),
      libraryDependencies = listOf(
        "@maven//:lib1",
      ),
      scalacOptions = null,
    )

    // when
    val javaModule = ModuleDetailsToJavaModuleTransformer(
      DefaultModuleNameProvider,
      projectBasePath,
    ).transform(moduleDetails)

    // then
    val expectedModule = GenericModuleInfo(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency("module2"),
        ModuleDependency("module3"),
      ),
      librariesDependencies = listOf(
        LibraryDependency("@maven//:lib1", true),
      ),
      associates = listOf(
        ModuleDependency("//target4"),
        ModuleDependency("//target5"),
      ),
    )

    val expectedBaseDirContentRoot = ContentRoot(
      path = projectRoot.toAbsolutePath(),
      excludedPaths = listOf(),
    )

    val expectedJavaModule = JavaModule(
      genericModuleInfo = expectedModule,
      baseDirContentRoot = expectedBaseDirContentRoot,
      sourceRoots = listOf(),
      resourceRoots = listOf(),
      moduleLevelLibraries = null,
      compilerOutput = Path("/compiler/output.jar"),
      jvmJdkName = "${projectBasePath.name}-$javaVersion",
      kotlinAddendum = KotlinAddendum(
        languageVersion = kotlinBuildTarget.languageVersion,
        apiVersion = kotlinBuildTarget.apiVersion,
        kotlincOptions = kotlinBuildTarget.kotlincOptions,
      ),
    )

    validateJavaModule(javaModule, expectedJavaModule)
  }

  @Test
  fun `should return multiple java modules for multiple module details`() {
    // given

    val module1Root = createTempDirectory(projectBasePath, "module1").toAbsolutePath()
    module1Root.toFile().deleteOnExit()

    val buildTargetId1 = BuildTargetIdentifier("module1")
    val buildTarget1 = BuildTarget(
      buildTargetId1,
      listOf("library"),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("module2"),
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities(),
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
      listOf(
        "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
      ),
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
    val target1OutputPathUris = listOf("file:///output/dir1a", "file:///output/file1b.out")

    val moduleDetails1 = ModuleDetails(
      target = buildTarget1,
      sources = listOf(sourcesItem1),
      resources = listOf(resourcesItem1),
      dependenciesSources = listOf(dependencySourcesItem1),
      javacOptions = target1JavacOptionsItem,
      pythonOptions = null,
      outputPathUris = target1OutputPathUris,
      libraryDependencies = null,
      moduleDependencies = listOf(
        "module2",
        "module3",
      ),
      scalacOptions = null,
    )

    val module2Root = createTempDirectory(projectBasePath, "module2").toAbsolutePath()
    module2Root.toFile().deleteOnExit()

    val buildTargetId2 = BuildTargetIdentifier("module2")
    val buildTarget2 = BuildTarget(
      buildTargetId2,
      listOf("test"),
      listOf("java"),
      listOf(
        BuildTargetIdentifier("module3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
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
      listOf("file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar"),
    )
    val target2JavacOptionsItem = JavacOptionsItem(
      buildTargetId2,
      listOf("opt2.1", "opt2.2"),
      listOf("file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar"),
      "file:///compiler/output2.jar",
    )
    val target2OutputPathUris = listOf("file:///output/dir2a", "file:///output/file2b.out")

    val moduleDetails2 = ModuleDetails(
      target = buildTarget2,
      sources = listOf(sourcesItem2),
      resources = listOf(resourcesItem2),
      dependenciesSources = listOf(dependencySourcesItem2),
      javacOptions = target2JavacOptionsItem,
      pythonOptions = null,
      outputPathUris = target2OutputPathUris,
      libraryDependencies = null,
      moduleDependencies = listOf(
        "module3",
      ),
      scalacOptions = null,
    )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)
    // when
    val javaModules =
      ModuleDetailsToJavaModuleTransformer(DefaultModuleNameProvider, projectBasePath).transform(modulesDetails)

    // then
    val expectedModule1 = GenericModuleInfo(
      name = "module1",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency("module2"),
        ModuleDependency("module3"),
        ModuleDependency(calculateDummyJavaModuleName(module1Root, projectBasePath)),
      ),
      librariesDependencies = listOf(
        LibraryDependency("BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar"),
        LibraryDependency("BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar"),
      ),
    )

    val expectedBaseDirContentRoot1 = ContentRoot(
      path = module1Root,
      excludedPaths = target1OutputPathUris.map { URI.create(it).toPath() },
    )

    val expectedJavaSourceRoot11 = JavaSourceRoot(
      sourcePath = file1APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot12 = JavaSourceRoot(
      sourcePath = file2APath,
      generated = false,
      packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
      rootType = "java-source",
    )
    val expectedJavaSourceRoot13 = JavaSourceRoot(
      sourcePath = dir1BPath,
      generated = false,
      packagePrefix = "${packageB1Path.name}.${packageB2Path.name}.${dir1BPath.name}",
      rootType = "java-source",
    )

    val expectedResourceRoot11 = ResourceRoot(
      resourcePath = resourceFilePath11,
    )
    val expectedResourceRoot12 = ResourceRoot(
      resourcePath = resourceFilePath12,
    )
    val expectedLibrary1 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/"),
    )
    val expectedLibrary2 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/"),
    )

    val expectedJavaModule1 = JavaModule(
      genericModuleInfo = expectedModule1,
      baseDirContentRoot = expectedBaseDirContentRoot1,
      sourceRoots = listOf(expectedJavaSourceRoot11, expectedJavaSourceRoot12, expectedJavaSourceRoot13),
      resourceRoots = listOf(expectedResourceRoot11, expectedResourceRoot12),
      compilerOutput = Path("/compiler/output1.jar"),
      jvmJdkName = null,
      kotlinAddendum = null,
      moduleLevelLibraries = listOf(expectedLibrary1, expectedLibrary2),
    )

    val expectedModule2 = GenericModuleInfo(
      name = "module2",
      type = "JAVA_MODULE",
      modulesDependencies = listOf(
        ModuleDependency("module3"),
        ModuleDependency(calculateDummyJavaModuleName(module2Root, projectBasePath)),
      ),
      librariesDependencies = listOf(
        LibraryDependency("BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar"),
      ),
    )

    val expectedBaseDirContentRoot2 = ContentRoot(
      path = module2Root,
      excludedPaths = target2OutputPathUris.map { URI.create(it).toPath() },
    )

    val expectedJavaSourceRoot21 = JavaSourceRoot(
      sourcePath = dir1CPath,
      generated = false,
      packagePrefix = "${packageC1Path.name}.${packageC2Path.name}.${dir1CPath.name}",
      rootType = "java-test",
    )

    val expectedResourceRoot21 = ResourceRoot(
      resourcePath = resourceDirPath21,
    )

    val expectedJavaModule2 = JavaModule(
      genericModuleInfo = expectedModule2,
      baseDirContentRoot = expectedBaseDirContentRoot2,
      sourceRoots = listOf(expectedJavaSourceRoot21),
      resourceRoots = listOf(expectedResourceRoot21),
      compilerOutput = Path("/compiler/output2.jar"),
      jvmJdkName = null,
      kotlinAddendum = null,
      moduleLevelLibraries = listOf(expectedLibrary1),
    )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule1, expectedJavaModule2) to { actual, expected -> validateJavaModule(actual, expected) }
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

  private fun validateJavaModule(actual: JavaModule, expected: JavaModule) {
    validateModule(actual.genericModuleInfo, expected.genericModuleInfo)

    actual.baseDirContentRoot shouldBe expected.baseDirContentRoot
    actual.sourceRoots shouldContainExactlyInAnyOrder expected.sourceRoots
    actual.resourceRoots shouldContainExactlyInAnyOrder expected.resourceRoots
    actual.jvmJdkName shouldBe expected.jvmJdkName
  }

  private fun validateModule(actual: GenericModuleInfo, expected: GenericModuleInfo) {
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
    extractedJvmBuildTarget shouldBe JvmBuildTarget().also {
      it.javaVersion = javaVersion
      it.javaHome = javaHome
    }
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

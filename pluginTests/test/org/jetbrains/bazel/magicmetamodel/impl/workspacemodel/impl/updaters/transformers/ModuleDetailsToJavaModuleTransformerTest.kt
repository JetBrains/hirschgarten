package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.toDefaultTargetsMap
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ResourceRoot
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

@DisplayName("ModuleDetailsToJavaModuleTransformer.transform(moduleDetails) tests")
class ModuleDetailsToJavaModuleTransformerTest : WorkspaceModelBaseTest() {
  @Test
  fun `should return single java module for single module details`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

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

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()

    val data = JvmBuildTarget(javaHome, javaVersion)

    val buildTargetId = Label.parse("module1")
    val buildTarget =
      RawBuildTarget(
        buildTargetId,
        listOf(),
        listOf(
          DependencyLabel(Label.parse("module2")),
          DependencyLabel(Label.parse("module3")),
          DependencyLabel(Label.parse("@maven//:lib1")),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = projectRoot,
        data = data,
        sources =
          listOf(
            SourceItem(
              path = file1APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
            SourceItem(
              path = file2APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
          ),
        resources =
          listOf(
            resourceFilePath,
          ),
      )

    val moduleDetails =
      ModuleDetails(
        target = buildTarget,
        javacOptions = listOf("opt1", "opt2", "opt3"),
        dependencies =
          listOf(
            DependencyLabel(Label.parse("module2")),
            DependencyLabel(Label.parse("module3")),
          ),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    val targetsMap = listOf(buildTargetId.toString(), "module2", "module3").toDefaultTargetsMap()
    // when
    val javaModule =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        emptyMap(),
        projectBasePath,
        project,
      ).transform(
        moduleDetails,
      ).first()

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "module1.module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2.module2",
            "module3.module3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedBaseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath())

    val expectedMergedJavaSourceRoot =
      JavaSourceRoot(
        sourcePath = projectRoot,
        generated = false,
        packagePrefix = "",
        rootType = SourceRootTypeId("java-source"),
      )

    val expectedResourceRoot1 =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaAddendum = JavaAddendum(languageVersion = javaVersion, javacOptions = emptyList())

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = expectedBaseDirContentRoot,
        sourceRoots = listOf(expectedMergedJavaSourceRoot),
        resourceRoots = listOf(expectedResourceRoot1),
        jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
        kotlinAddendum = null,
        javaAddendum = expectedJavaAddendum,
      )

    validateJavaModule(javaModule, expectedJavaModule)
  }

  @Test
  fun `should return java module with associates as dependencies when specified`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

    val kotlinBuildTarget =
      KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = listOf(),
        associates =
          listOf(
            Label.parse("//target4"),
            Label.parse("//target5"),
          ),
        jvmBuildTarget =
          JvmBuildTarget(
            javaHome = javaHome,
            javaVersion = javaVersion,
          ),
      )

    val buildTargetId = Label.parse("module1")
    val buildTarget =
      RawBuildTarget(
        buildTargetId,
        listOf(),
        listOf(
          DependencyLabel(Label.parse("module2")),
          DependencyLabel(Label.parse("module3")),
          DependencyLabel(Label.parse("@maven//:lib1")),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = projectRoot,
        data = kotlinBuildTarget,
        sources = emptyList(),
        resources = emptyList(),
      )

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val moduleDetails =
      ModuleDetails(
        target = buildTarget,
        javacOptions = listOf(),
        dependencies =
          listOf(
            DependencyLabel(Label.parse("module2")),
            DependencyLabel(Label.parse("module3")),
          ),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    val targetsMap = listOf(buildTargetId.toString(), "module2", "module3").toDefaultTargetsMap()

    // when
    val javaModule =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        emptyMap(),
        projectBasePath,
        project,
      ).transform(moduleDetails).first()

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "module1.module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2.module2",
            "module3.module3",
          ),
        associates =
          listOf(
            "//target4",
            "//target5",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedBaseDirContentRoot =
      ContentRoot(
        path = projectRoot.toAbsolutePath(),
      )

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = expectedBaseDirContentRoot,
        sourceRoots = listOf(),
        resourceRoots = listOf(),
        jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
        kotlinAddendum =
          KotlinAddendum(
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

    val packageA1Path = createTempDirectory(module1Root, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val resourceFilePath11 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File1.txt")
    resourceFilePath11.toFile().deleteOnExit()
    val resourceFilePath12 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File2.txt")
    resourceFilePath12.toFile().deleteOnExit()

    val buildTargetId1 = Label.parse("module1")
    val buildTarget1 =
      RawBuildTarget(
        buildTargetId1,
        listOf(),
        listOf(
          DependencyLabel(Label.parse("module2")),
          DependencyLabel(Label.parse("module3")),
          DependencyLabel(Label.parse("@maven//:lib1")),
        ),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = module1Root,
        sources =
          listOf(
            SourceItem(
              path = file1APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
            SourceItem(
              path = file2APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
          ),
        resources =
          listOf(
            resourceFilePath11,
            resourceFilePath12,
          ),
      )

    val moduleDetails1 =
      ModuleDetails(
        target = buildTarget1,
        javacOptions = listOf("opt1.1", "opt1.2", "opt1.3"),
        dependencies =
          listOf(
            DependencyLabel(Label.parse("module2")),
            DependencyLabel(Label.parse("module3")),
          ),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    val module2Root = createTempDirectory(projectBasePath, "module2").toAbsolutePath()
    module2Root.toFile().deleteOnExit()

    val packageC1Path = createTempDirectory(module2Root, "packageC1")
    packageC1Path.toFile().deleteOnExit()
    val packageC2Path = createTempDirectory(packageC1Path, "packageC2")
    packageC2Path.toFile().deleteOnExit()
    val dir1CPath = createTempFile(packageC2Path, "File1", ".java")
    dir1CPath.toFile().deleteOnExit()

    val resourceDirPath21 = Files.createTempDirectory(projectBasePath.toAbsolutePath(), "resource")

    val buildTargetId2 = Label.parse("module2")
    val buildTarget2 =
      RawBuildTarget(
        buildTargetId2,
        listOf(),
        listOf(
          DependencyLabel(Label.parse("module3")),
          DependencyLabel(Label.parse("@maven//:lib1")),
        ),
        TargetKind(
          kindString = "java_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = module2Root,
        sources =
          listOf(
            SourceItem(
              path = dir1CPath,
              generated = false,
              jvmPackagePrefix = "${packageC1Path.name}.${packageC2Path.name}",
            ),
          ),
        resources =
          listOf(
            resourceDirPath21,
          ),
      )

    val moduleDetails2 =
      ModuleDetails(
        target = buildTarget2,
        javacOptions = listOf("opt2.1", "opt2.2"),
        dependencies =
          listOf(
            DependencyLabel(Label.parse("module3")),
          ),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    val modulesDetails = listOf(moduleDetails1, moduleDetails2)

    val targetsMap = listOf("module1", "module2", "module3").toDefaultTargetsMap()
    // when
    val javaModules =
      modulesDetails.map { entity ->
        ModuleDetailsToJavaModuleTransformer(
          targetsMap,
          emptyMap(),
          projectBasePath,
          project,
        ).transform(entity).first()
      }

    // then
    val expectedModule1 =
      GenericModuleInfo(
        name = "module1.module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2.module2",
            "module3.module3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedBaseDirContentRoot1 =
      ContentRoot(
        path = module1Root,
      )

    val expectedMergedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = module1Root,
        generated = false,
        packagePrefix = "",
        rootType = SourceRootTypeId("java-source"),
      )

    val expectedResourceRoot11 =
      ResourceRoot(
        resourcePath = resourceFilePath11,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedResourceRoot12 =
      ResourceRoot(
        resourcePath = resourceFilePath12,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaModule1 =
      JavaModule(
        genericModuleInfo = expectedModule1,
        baseDirContentRoot = expectedBaseDirContentRoot1,
        sourceRoots = listOf(expectedMergedJavaSourceRoot1),
        resourceRoots = listOf(expectedResourceRoot11, expectedResourceRoot12),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val expectedModule2 =
      GenericModuleInfo(
        name = "module2.module2",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module3.module3",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedBaseDirContentRoot2 =
      ContentRoot(
        path = module2Root,
      )

    val expectedMergedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = module2Root,
        generated = false,
        packagePrefix = "",
        rootType = SourceRootTypeId("java-test"),
      )

    val expectedResourceRoot21 =
      ResourceRoot(
        resourcePath = resourceDirPath21,
        rootType = SourceRootTypeId("java-test-resource"),
      )

    val expectedJavaModule2 =
      JavaModule(
        genericModuleInfo = expectedModule2,
        baseDirContentRoot = expectedBaseDirContentRoot2,
        sourceRoots = listOf(expectedMergedJavaSourceRoot2),
        resourceRoots = listOf(expectedResourceRoot21),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule1, expectedJavaModule2) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  @Test
  fun `should add dependency on dummy module properly`() {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    projectRoot.toFile().deleteOnExit()

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

    val data = JvmBuildTarget(javaHome, javaVersion)

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val outOfSourceFile = createTempFile(packageA2Path, "File3", ".java")
    outOfSourceFile.toFile().deleteOnExit()

    val packageB1Path = createTempDirectory(projectRoot, "packageB1")
    packageB1Path.toFile().deleteOnExit()
    val packageB2Path = createTempDirectory(packageB1Path, "packageB2")
    packageB2Path.toFile().deleteOnExit()

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()

    val buildTargetId = Label.parse("module1")
    val buildTarget =
      RawBuildTarget(
        buildTargetId,
        listOf(),
        listOf(
          DependencyLabel(Label.parse("module2")),
          DependencyLabel(Label.parse("module3")),
          DependencyLabel(Label.parse("@maven//:lib1")),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = projectRoot,
        data = data,
        sources =
          listOf(
            SourceItem(
              path = file1APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
            SourceItem(
              path = file2APath,
              generated = false,
              jvmPackagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
            ),
          ),
        resources = listOf(resourceFilePath),
      )

    val moduleDetails =
      ModuleDetails(
        target = buildTarget,
        javacOptions = listOf("opt1", "opt2", "opt3"),
        dependencies =
          listOf(
            DependencyLabel(Label.parse("module2")),
            DependencyLabel(Label.parse("module3")),
          ),
        defaultJdkName = null,
        jvmBinaryJars = emptyList(),
      )

    val targetsMap = listOf(buildTargetId.toString(), "module2", "module3").toDefaultTargetsMap()
    // when
    val javaModules =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        emptyMap(),
        projectBasePath,
        project,
      ).transform(
        moduleDetails,
      )

    // then
    val dummyJavaModuleName = calculateDummyJavaModuleName(projectRoot, projectBasePath)
    val expectedModule =
      GenericModuleInfo(
        name = "module1.module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2.module2",
            "module3.module3",
            dummyJavaModuleName,
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val expectedBaseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath())

    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = file1APath,
        generated = false,
        packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = file2APath,
        generated = false,
        packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
        rootType = SourceRootTypeId("java-source"),
      )

    val expectedResourceRoot1 =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaAddendum = JavaAddendum(languageVersion = javaVersion, javacOptions = emptyList())

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = expectedBaseDirContentRoot,
        sourceRoots = listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2),
        resourceRoots = listOf(expectedResourceRoot1),
        jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
        kotlinAddendum = null,
        javaAddendum = expectedJavaAddendum,
      )

    validateJavaModule(javaModules.first(), expectedJavaModule)
    javaModules.size shouldBe 2
    javaModules[1].getModuleName() shouldBe dummyJavaModuleName
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
    actual.dependencies shouldBe expected.dependencies
  }
}

class ExtractJvmBuildTargetTest {
  @Test
  fun `extractJvmBuildTarget should return JvmBuildTarget successfully when given non-null jdk information`() {
    // given
    val javaVersion = "17"
    val javaHome = Path("/fake/path/to/test/local_jdk")
    val data = JvmBuildTarget(javaHome, javaVersion)

    val buildTarget = buildDummyTarget(data)

    // when
    val extractedJvmBuildTarget = extractJvmBuildTarget(buildTarget)

    // then
    extractedJvmBuildTarget shouldBe
      JvmBuildTarget(
        javaVersion = javaVersion,
        javaHome = javaHome,
      )
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

  private fun buildDummyTarget(data: BuildTargetData? = null): RawBuildTarget {
    val buildTarget =
      RawBuildTarget(
        Label.parse("target"),
        listOf("tag1", "tag2"),
        listOf(DependencyLabel(Label.parse("dep1")), DependencyLabel(Label.parse("dep2"))),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("/base/dir"),
        data = data,
        sources = listOf(SourceItem(Path("\$WORKSPACE/src/Main.java"), false)),
        resources = emptyList(),
      )
    return buildTarget
  }
}

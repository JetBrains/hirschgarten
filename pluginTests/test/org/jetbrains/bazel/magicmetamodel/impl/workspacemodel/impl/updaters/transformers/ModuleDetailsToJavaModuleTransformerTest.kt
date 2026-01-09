package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

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
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createJavaModule
import org.jetbrains.bazel.workspace.model.test.framework.createModuleDetails
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.JavaAddendum
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.KotlinAddendum
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ResourceItem
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.UltimateBuildTarget
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

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")

    val buildTargetId = Label.parse("module1")
    val buildTarget = createRawBuildTarget(
      id = buildTargetId,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
        DependencyLabel.parse("@maven//:lib1"),
      ),
      kind = TargetKind(
        kindString = "java_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirectory = projectRoot,
      data = JvmBuildTarget(javaHome, javaVersion),
      sources = listOf(
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
      resources = listOf(ResourceItem.File(resourceFilePath)),
    )

    val moduleDetails = createModuleDetails(
      target = buildTarget,
      javacOptions = listOf("opt1", "opt2", "opt3"),
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
      ),
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
    val expectedJavaModule = createJavaModule(
      name = "module1.module1",
      dependencies = listOf(
        Dependency("module2.module2"),
        Dependency("module3.module3"),
      ),
      kind = TargetKind(
        kindString = "java_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot,
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("java-source"),
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath,
          rootType = SourceRootTypeId("java-resource"),
        ),
      ),
      jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
      javaAddendum = JavaAddendum(languageVersion = javaVersion, javacOptions = listOf("opt1", "opt2", "opt3")),
    )

    javaModule shouldBe expectedJavaModule
  }

  @Test
  fun `should return java module with resource roots for resource targets with relative paths`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()
    val baseDir = createTempDirectory(projectRoot, "base")
    val dir1 = createTempDirectory(baseDir, "dir1")
    val dir2 = createTempDirectory(baseDir, "dir2")
    val resourceTargetId1 = Label.parse("//:resources_1")
    val resourceTarget1 = createRawBuildTarget(
      id = resourceTargetId1,
      kind = TargetKind("resourcegroup", setOf(LanguageClass.ULTIMATE), RuleType.UNKNOWN),
      data = UltimateBuildTarget(
        resources = listOf(dir1.resolve("resource1.txt")),
        stripPrefix = dir1,
        addPrefix = "new/prefix",
      ),
    )

    val resourceTargetId2 = Label.parse("//:resources_2")
    val resourceTarget2 = createRawBuildTarget(
      id = resourceTargetId2,
      kind = TargetKind("resourcegroup", setOf(LanguageClass.ULTIMATE), RuleType.UNKNOWN),
      data = UltimateBuildTarget(
        resources = listOf(dir2.resolve("resource2.txt")),
        stripPrefix = dir2,
        addPrefix = null,
      ),
    )

    val buildTargetId = Label.parse("module1")
    val buildTarget = createRawBuildTarget(
      id = buildTargetId,
      baseDirectory = projectRoot,
      resources = listOf(
        ResourceItem.Target(resourceTargetId1),
        ResourceItem.Target(resourceTargetId2),
      ),
    )

    val moduleDetails = createModuleDetails(target = buildTarget)

    val targetsMap = (listOf(buildTargetId.toString()) + resourceTargetId1.toString() + resourceTargetId2.toString())
                       .toDefaultTargetsMap() + (resourceTargetId1 to resourceTarget1) + (resourceTargetId2 to resourceTarget2)

    // when
    val javaModule =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        emptyMap(),
        projectBasePath,
        project,
      ).transform(moduleDetails).first()

    // then
    val expectedJavaModule = createJavaModule(
      name = "module1.module1",
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = dir1,
          rootType = SourceRootTypeId("java-resource"),
          relativePath = "new/prefix",
        ),
        ResourceRoot(
          resourcePath = dir2,
          rootType = SourceRootTypeId("java-resource"),
          relativePath = null,
        ),
      ),
    )

    javaModule shouldBe expectedJavaModule
  }

  @Test
  fun `should return java module with associates as dependencies when specified`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

    val kotlinBuildTarget =
      KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = listOf(),
        associates =
          listOf(
            Label.parse("module4"),
            Label.parse("module5"),
          ),
        jvmBuildTarget =
          JvmBuildTarget(
            javaHome = javaHome,
            javaVersion = javaVersion,
          ),
      )

    val buildTargetId = Label.parse("module1")
    val buildTarget = createRawBuildTarget(
      id = buildTargetId,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
        DependencyLabel.parse("@maven//:lib1"),
      ),
      kind = TargetKind(
        kindString = "java_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirectory = projectRoot,
      data = kotlinBuildTarget,
    )

    createTempFile(projectBasePath, "resource", "File.txt")

    val moduleDetails = createModuleDetails(
      target = buildTarget,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
      ),
    )

    val targetsMap = listOf(buildTargetId.toString(), "module2", "module3", "module4", "module5").toDefaultTargetsMap()

    // when
    val javaModule =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        emptyMap(),
        projectBasePath,
        project,
      ).transform(moduleDetails).first()

    // then
    val expectedJavaModule = createJavaModule(
      name = "module1.module1",
      dependencies = listOf(
        Dependency("module2.module2"),
        Dependency("module3.module3"),
      ),
      associates = listOf(
        "module4.module4",
        "module5.module5",
      ),
      kind = TargetKind(
        kindString = "java_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
      kotlinAddendum = KotlinAddendum(
        languageVersion = kotlinBuildTarget.languageVersion,
        apiVersion = kotlinBuildTarget.apiVersion,
        kotlincOptions = kotlinBuildTarget.kotlincOptions,
        moduleName = null,
      ),
      javaAddendum = JavaAddendum(languageVersion = javaVersion, javacOptions = emptyList()),
    )

    javaModule shouldBe expectedJavaModule
  }

  @Test
  fun `should return multiple java modules for multiple module details`() {
    // given
    val module1Root = createTempDirectory(projectBasePath, "module1").toAbsolutePath()

    val packageA1Path = createTempDirectory(module1Root, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath11 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File1.txt")
    val resourceFilePath12 = createTempFile(projectBasePath.toAbsolutePath(), "resource", "File2.txt")

    val buildTargetId1 = Label.parse("module1")
    val buildTarget1 = createRawBuildTarget(
      id = buildTargetId1,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
        DependencyLabel.parse("@maven//:lib1"),
      ),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirectory = module1Root,
      sources = listOf(
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
      resources = listOf(
        ResourceItem.File(resourceFilePath11),
        ResourceItem.File(resourceFilePath12),
      ),
    )

    val moduleDetails1 = createModuleDetails(
      target = buildTarget1,
      javacOptions = listOf("opt1.1", "opt1.2", "opt1.3"),
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
      ),
    )

    val module2Root = createTempDirectory(projectBasePath, "module2").toAbsolutePath()
    val packageC1Path = createTempDirectory(module2Root, "packageC1")
    val packageC2Path = createTempDirectory(packageC1Path, "packageC2")
    val dir1CPath = createTempFile(packageC2Path, "File1", ".java")

    val resourceDirPath21 = Files.createTempDirectory(projectBasePath.toAbsolutePath(), "resource")

    val buildTargetId2 = Label.parse("module2")
    val buildTarget2 = createRawBuildTarget(
      id = buildTargetId2,
      dependencies = listOf(
        DependencyLabel.parse("module3"),
        DependencyLabel.parse("@maven//:lib1"),
      ),
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirectory = module2Root,
      sources = listOf(
        SourceItem(
          path = dir1CPath,
          generated = false,
          jvmPackagePrefix = "${packageC1Path.name}.${packageC2Path.name}",
        ),
      ),
      resources = listOf(ResourceItem.File(resourceDirPath21)),
    )

    val moduleDetails2 = createModuleDetails(
      target = buildTarget2,
      javacOptions = listOf("opt2.1", "opt2.2"),
      dependencies = listOf(DependencyLabel.parse("module3")),
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
    val expectedJavaModule1 = createJavaModule(
      name = "module1.module1",
      dependencies = listOf(
        Dependency("module2.module2"),
        Dependency("module3.module3"),
      ),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = module1Root),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = module1Root,
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("java-source"),
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath11,
          rootType = SourceRootTypeId("java-resource"),
        ),
        ResourceRoot(
          resourcePath = resourceFilePath12,
          rootType = SourceRootTypeId("java-resource"),
        ),
      ),
    )

    val expectedJavaModule2 = createJavaModule(
      name = "module2.module2",
      dependencies = listOf(
        Dependency("module3.module3"),
      ),
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = module2Root),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = module2Root,
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("java-test"),
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceDirPath21,
          rootType = SourceRootTypeId("java-test-resource"),
        ),
      ),
      javaAddendum = null,
    )

    javaModules shouldContainExactlyInAnyOrder listOf(expectedJavaModule1, expectedJavaModule2)
  }

  @Test
  fun `should add dependency on dummy module properly`() {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return
    // given
    val projectRoot = createTempDirectory(projectBasePath, "project").toAbsolutePath()

    val javaHome = Path("/fake/path/to/local_jdk")
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")

    val buildTargetId = Label.parse("module1")
    val buildTarget = createRawBuildTarget(
      id = buildTargetId,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
        DependencyLabel.parse("@maven//:lib1"),
      ),
      kind = TargetKind(
        kindString = "java_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirectory = projectRoot,
      data = JvmBuildTarget(javaHome, javaVersion),
      sources = listOf(
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
      resources = listOf(ResourceItem.File(resourceFilePath)),
    )

    val moduleDetails = createModuleDetails(
      target = buildTarget,
      dependencies = listOf(
        DependencyLabel.parse("module2"),
        DependencyLabel.parse("module3"),
      ),
    )

    val targetsMap = listOf(buildTargetId.toString(), "module2", "module3").toDefaultTargetsMap()

    val dummyJavaModuleName = "dummy"
    val fileToTargetWithoutLowPrioritySharedSources = mapOf(file1APath to listOf(Label.parse(dummyJavaModuleName)))

    // when
    val javaModules =
      ModuleDetailsToJavaModuleTransformer(
        targetsMap,
        fileToTargetWithoutLowPrioritySharedSources,
        projectBasePath,
        project,
      ).transform(moduleDetails)

    // then
    val expectedJavaModule = createJavaModule(
      name = "module1.module1",
      dependencies = listOf(
        Dependency("module2.module2"),
        Dependency("module3.module3"),
        Dependency(dummyJavaModuleName),
      ),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath,
          generated = false,
          packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = file2APath,
          generated = false,
          packagePrefix = "${packageA1Path.name}.${packageA2Path.name}",
          rootType = SourceRootTypeId("java-source"),
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath,
          rootType = SourceRootTypeId("java-resource"),
        ),
      ),
      jvmJdkName = projectBasePath.name.projectNameToJdkName(javaHome),
      javaAddendum = JavaAddendum(languageVersion = javaVersion, javacOptions = emptyList()),
    )

    javaModules.first() shouldBe expectedJavaModule
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
        listOf(DependencyLabel.parse("dep1"), DependencyLabel(Label.parse("dep2"))),
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

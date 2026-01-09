package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.JavaModuleType.JAVA_MODULE_ENTITY_TYPE_ID_NAME
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createJavaModule
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

class JavaModuleToDummyJavaModulesTransformerHACKTest : WorkspaceModelBaseTest() {
  @Test
  fun `should merge sources of module with sources in common root`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val irrelevantFilePath = createTempFile(projectRoot, "irrelevant", ".xml")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      dependencies = listOf(
        Dependency("module2"),
        Dependency("module3"),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = irrelevantFilePath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = irrelevantFilePath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should merge roots with sources and with resources in common path`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val resourcesPath = createTempDirectory(mainPath, "resources")
    val messagesPath = createTempDirectory(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      dependencies = listOf(Dependency("@maven//:lib1")),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = javaPath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    val expectedMergedResourceRoots =
      listOf(
        ResourceRoot(
          resourcePath = messagesPath,
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      )

    (mergedRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
    mergedRoots.mergedResourceRoots shouldContainExactlyInAnyOrder expectedMergedResourceRoots
  }

  @Test
  fun `should merge roots for module with no sources and with single resources in common path`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val resourcesPath = createTempDirectory(mainPath, "resources")
    val messagesPath = createTempDirectory(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      dependencies = listOf(Dependency("@maven//:lib1")),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots = emptyList<JavaSourceRoot>()

    val expectedMergedResourceRoots =
      listOf(
        ResourceRoot(
          resourcePath = messagesPath,
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      )

    (mergedRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
    mergedRoots.mergedResourceRoots shouldContainExactlyInAnyOrder expectedMergedResourceRoots
  }

  @Test
  fun `should merge roots for module with no sources and with multiple resources in common path`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val resourcesPath = createTempDirectory(mainPath, "resources")
    val messagesPath = createTempDirectory(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    val iconsPath = createTempDirectory(resourcesPath, "icons")
    val iconFilePath = createTempFile(iconsPath, "icon", ".png")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      dependencies = listOf(Dependency("@maven//:lib1")),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
        ResourceRoot(
          resourcePath = iconFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots = emptyList<JavaSourceRoot>()

    val expectedMergedResourceRoots =
      listOf(
        ResourceRoot(
          resourcePath = resourcesPath,
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      )

    (mergedRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
    mergedRoots.mergedResourceRoots shouldContainExactlyInAnyOrder expectedMergedResourceRoots
  }

  @Test
  fun `should return unmerged files if additional files would be covered`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val resourcesPath = createTempDirectory(mainPath, "resources")
    val messagesPath = createTempDirectory(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    createTempFile(messagesPath, "AnotherResource", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    mergedRoots shouldBe
      JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots(
        mergedSourceRoots = emptyList(),
        mergedResourceRoots = givenJavaModule.resourceRoots,
      )
  }

  @Test
  fun `should return dummy module with out-of-project sources`() {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    createTempFile(packageA2Path, "File3", ".java")

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val javaModules = transformIntoDummyModules(givenJavaModule)

    // then
    val expectedJavaModule =
      createJavaModule(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${javaPath.name}".addIntelliJDummyPrefix(),
        type = BazelDummyModuleType.ID,
        kind = TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA, LanguageClass.KOTLIN),
        ),
        dependencies = givenJavaModule.genericModuleInfo.dependencies,
        baseDirContentRoot = ContentRoot(path = javaPath.toAbsolutePath()),
        sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = javaPath.toAbsolutePath(),
            generated = false,
            packagePrefix = "",
            rootType = JAVA_SOURCE_ROOT_TYPE,
          ),
        ),
        resourceRoots = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
                                               )
  }

  @Test
  fun `should merge sources for module with sources and with no resources outside project root`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(javaPath, "packageA2")
    val file1APath = createTempFile(packageA1Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    createTempFile(packageA2Path, "File3", ".java.non.source")
    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should merge sources for module with nested source roots`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA1Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should return no dummy java module for module with sources in directories`() {
    // given
    val projectRoot1 = createTempDirectory(projectBasePath, "module1")
    val projectRoot1Name = projectRoot1.name

    val projectRoot2 = createTempDirectory(projectBasePath, "module2")
    val projectRoot2Name = projectRoot2.name

    val givenJavaModule1 =
      createJavaModule(
        name = projectRoot1Name,
        type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
        kind = TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirContentRoot = ContentRoot(path = projectRoot1.toAbsolutePath()),
        sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = projectRoot1.toAbsolutePath(),
            generated = false,
            packagePrefix = "",
            rootType = JAVA_SOURCE_ROOT_TYPE,
          ),
        ),
      )

    val givenJavaModule2 = createJavaModule(
      name = projectRoot2Name,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot2.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = projectRoot2.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    val modulesList = listOf(givenJavaModule1, givenJavaModule2)

    // when
    val javaModules = transformIntoDummyModules(modulesList)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should not create dummy modules for generated source roots`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module")
    val projectRootName = projectRoot.name
    val filePath = createTempFile(projectRoot, "File", ".java")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = filePath.toAbsolutePath(),
          generated = true,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
    )

    val modulesList = listOf(givenJavaModule)

    // when
    val javaModules = transformIntoDummyModules(modulesList)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should merge sources of module with test sources in common root`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should prefer test root if test and production sources are together`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      dependencies = listOf(Dependency("@maven//:lib1")),
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = resourceFilePath.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = javaPath.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_TEST_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should not go higher than the BUILD file`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = packageA2Path.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = packageA2Path.name,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should stop going up when directories stop matching package segments`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val packagePrefix = "org.example.${packageA2Path.name}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "org.example",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should not merge sources if there are shared sources`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val packagePrefix = packageA2Path.name

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    val fileToTarget =
      mapOf(
        file1APath to listOf(Label.parse("//:target1"), Label.parse("//:target2")),
      )

    // when
    val dummyModules = transformIntoDummyModules(givenJavaModule, fileToTarget)

    // then
    dummyModules.size shouldBe 1
  }

  @Test
  fun `should prefer source root that has more votes`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val srcPath = createTempDirectory(projectRoot, "src")
    val mainPath = createTempDirectory(srcPath, "main")
    val javaPath = createTempDirectory(mainPath, "java")
    val packageA1Path = createTempDirectory(javaPath, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    val file3APath = createTempFile(packageA2Path, "File2", ".java")

    val packagePrefixWithMoreVotes = "${packageA2Path.fileName}"
    val packagePrefixWithFewerVotes = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
      kind =
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = file1APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithMoreVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file2APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithMoreVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
        JavaSourceRoot(
          sourcePath = file3APath.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefixWithFewerVotes,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA1Path.toAbsolutePath(),
          generated = false,
          packagePrefix = "",
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  @Test
  fun `should handle different relative paths for resource roots`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val resourcesPathA = createTempDirectory(projectRoot, "resources-a")
    val resourcesPathB = createTempDirectory(projectRoot, "resources-b")
    val resourcesPathC = createTempDirectory(projectRoot, "resources-c")
    val fileA1 = createTempFile(resourcesPathA, "file1", ".properties")
    val fileA2 = createTempFile(resourcesPathA, "file2", ".properties")
    val fileB1 = createTempFile(resourcesPathB, "file1", ".properties")
    val fileB2 = createTempFile(resourcesPathB, "file2", ".properties")

    val givenJavaModule = createJavaModule(
      name = projectRootName,
      baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
      resourceRoots = listOf(
        ResourceRoot(
          resourcePath = fileA1.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
          relativePath = "a",
        ),
        ResourceRoot(
          resourcePath = fileA2.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
          relativePath = "a",
        ),
        ResourceRoot(
          resourcePath = fileB1.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
          relativePath = null,
        ),
        ResourceRoot(
          resourcePath = fileB2.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
          relativePath = null,
        ),
        ResourceRoot(
          resourcePath = resourcesPathC.toAbsolutePath(),
          rootType = JAVA_RESOURCE_ROOT_TYPE,
          relativePath = "c",
        ),
      ),
      jvmJdkName = javaVersion,
    )

    // when
    val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedResourceRoots = listOf(
      ResourceRoot(
        resourcePath = resourcesPathA.toAbsolutePath(),
        rootType = JAVA_RESOURCE_ROOT_TYPE,
        relativePath = "a",
      ),
      ResourceRoot(
        resourcePath = resourcesPathB.toAbsolutePath(),
        rootType = JAVA_RESOURCE_ROOT_TYPE,
        relativePath = null,
      ),
      ResourceRoot(
        resourcePath = resourcesPathC.toAbsolutePath(),
        rootType = JAVA_RESOURCE_ROOT_TYPE,
        relativePath = "c",
      ),
    )

    (result as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedResourceRoots shouldContainExactlyInAnyOrder expectedMergedResourceRoots
  }

  @Test
  fun `should fall back to parent directory if can't add package`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"
    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    createTempFile(packageA1Path, "File2", ".java")
    val packagePrefix = "${packageA1Path.name}.${packageA2Path.name}"

    val givenJavaModule =
      createJavaModule(
        name = projectRootName,
        type = JAVA_MODULE_ENTITY_TYPE_ID_NAME,
        kind = TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = file1APath.toAbsolutePath(),
            generated = false,
            packagePrefix = packagePrefix,
            rootType = JAVA_SOURCE_ROOT_TYPE,
          ),
        ),
        jvmJdkName = javaVersion,
      )

    // when
    val mergedSourceRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    val expectedMergedSourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = packageA2Path.toAbsolutePath(),
          generated = false,
          packagePrefix = packagePrefix,
          rootType = JAVA_SOURCE_ROOT_TYPE,
        ),
      )

    (mergedSourceRoots as JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots).mergedSourceRoots shouldContainExactlyInAnyOrder
      expectedMergedSourceRoots
  }

  private fun transformIntoDummyModules(module: JavaModule, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    transformIntoDummyModules(listOf(module), fileToTarget)

  private fun transformIntoDummyModules(modules: List<JavaModule>, fileToTarget: Map<Path, List<Label>> = emptyMap()): List<JavaModule> =
    modules
      .flatMap { module ->
        val result = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, fileToTarget, project).transform(module)
        (result as JavaModuleToDummyJavaModulesTransformerHACK.DummyModulesToAdd).dummyModules
      }.distinctBy { it.getModuleName() }

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
  }

  private fun validateModule(actual: GenericModuleInfo, expected: GenericModuleInfo) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.dependencies shouldBe expected.dependencies
  }
}

package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.JavaModuleType.JAVA_MODULE_ENTITY_TYPE_ID_NAME
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies =
          listOf(
            Dependency("module2"),
            Dependency("module3"),
            Dependency("@maven//:lib1"),
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val irrelevantFilePath = createTempFile(projectRoot, "irrelevant", ".xml")
    irrelevantFilePath.toFile().deleteOnExit()
    val packagePrefix = packageA2Path.name

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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

  private fun createTempDirectoryAndDeleteItOnExit(path: Path, prefix: String) =
    createTempDirectory(path, prefix).also {
      it.toFile().deleteOnExit()
    }

  @Test
  fun `should merge roots with sources and with resources in common path`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val resourcesPath = createTempDirectoryAndDeleteItOnExit(mainPath, "resources")
    val messagesPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val resourcesPath = createTempDirectoryAndDeleteItOnExit(mainPath, "resources")
    val messagesPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val resourcesPath = createTempDirectoryAndDeleteItOnExit(mainPath, "resources")
    val messagesPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()
    val iconsPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "icons")
    val iconFilePath = createTempFile(iconsPath, "icon", ".png")
    iconFilePath.toFile().deleteOnExit()

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(),
        resourceRoots =
          listOf(
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
        kotlinAddendum = null,
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
  fun `should not merge roots if additional files would be covered`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val resourcesPath = createTempDirectoryAndDeleteItOnExit(mainPath, "resources")
    val messagesPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()
    val anotherResourceFilePath = createTempFile(messagesPath, "AnotherResource", ".properties")
    anotherResourceFilePath.toFile().deleteOnExit()

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val mergedRoots = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, emptyMap(), project).transform(givenJavaModule)

    // then
    mergedRoots shouldBe
      JavaModuleToDummyJavaModulesTransformerHACK.MergedRoots(
        mergedSourceRoots = emptyList(),
        mergedResourceRoots = null,
      )
  }

  @Test
  fun `should return dummy module with out-of-project sources`() {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val outOfProjectSource = createTempFile(packageA2Path, "File3", ".java")
    outOfProjectSource.toFile().deleteOnExit()

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = transformIntoDummyModules(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${javaPath.name}".addIntelliJDummyPrefix(),
        type = ModuleTypeId(BazelDummyModuleType.ID),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA, LanguageClass.KOTLIN),
          ),
        dependencies = givenJavaModule.genericModuleInfo.dependencies,
      )

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = ContentRoot(path = javaPath.toAbsolutePath()),
        sourceRoots =
          listOf(
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA2")
    val file1APath = createTempFile(packageA1Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val outOfProjectNonSourceFile = createTempFile(packageA2Path, "File3", ".java.non.source")
    outOfProjectNonSourceFile.toFile().deleteOnExit()

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA1Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    projectRoot1.toFile().deleteOnExit()
    val projectRoot1Name = projectRoot1.name

    val projectRoot2 = createTempDirectory(projectBasePath, "module2")
    projectRoot2.toFile().deleteOnExit()
    val projectRoot2Name = projectRoot2.name

    val givenModule1 =
      GenericModuleInfo(
        name = projectRoot1Name,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val givenJavaModule1 =
      JavaModule(
        genericModuleInfo = givenModule1,
        baseDirContentRoot = ContentRoot(path = projectRoot1.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = projectRoot1.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val givenModule2 =
      GenericModuleInfo(
        name = projectRoot2Name,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val givenJavaModule2 =
      JavaModule(
        genericModuleInfo = givenModule2,
        baseDirContentRoot = ContentRoot(path = projectRoot2.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = projectRoot2.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name

    val filePath = createTempFile(projectRoot, "File", ".java")
    filePath.toFile().deleteOnExit()

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = filePath.toAbsolutePath(),
              generated = true,
              packagePrefix = "",
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val packagePrefix = packageA2Path.name

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = listOf(Dependency("@maven//:lib1")),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()

    val resourceFilePath = createTempFile(projectBasePath.toAbsolutePath().parent, "Resources", ".properties")
    resourceFilePath.toFile().deleteOnExit()
    val packagePrefix = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          ),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val packagePrefix = packageA2Path.name

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = packageA2Path.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val packagePrefix = "org.example.${packageA2Path.name}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = file1APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val packagePrefix = packageA2Path.name

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = file1APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val javaPath = createTempDirectoryAndDeleteItOnExit(mainPath, "java")
    val packageA1Path = createTempDirectoryAndDeleteItOnExit(javaPath, "packageA1")
    val packageA2Path = createTempDirectoryAndDeleteItOnExit(packageA1Path, "packageA2")
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val file3APath = createTempFile(packageA2Path, "File2", ".java")
    file3APath.toFile().deleteOnExit()

    val packagePrefixWithMoreVotes = "${packageA2Path.fileName}"
    val packagePrefixWithFewerVotes = "${packageA1Path.fileName}.${packageA2Path.fileName}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
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
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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
  fun `should fall back to parent directory if can't add package`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA1Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
    val packagePrefix = "${packageA1Path.name}.${packageA2Path.name}"

    val givenJavaModule =
      JavaModule(
        genericModuleInfo = givenModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = file1APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JAVA_SOURCE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
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

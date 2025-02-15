package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.workspacemodel.entities.ContentRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.Library
import org.jetbrains.plugins.bsp.workspacemodel.entities.ResourceRoot
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

class JavaModuleToDummyJavaModulesTransformerHACKTest : WorkspaceModelBaseTest() {
  @Test
  fun `should return no dummy java modules for no module details`() {
    // given
    val emptyModulesDetails = listOf<JavaModule>()

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(emptyModulesDetails)

    // then
    javaModules shouldBe emptyList()
  }

  @Test
  fun `should return single dummy java module for module with sources in common root`() {
    // given
    val projectRoot = createTempDirectory(projectBasePath, "module1")
    projectRoot.toFile().deleteOnExit()
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies =
          listOf(
            IntermediateModuleDependency("module2"),
            IntermediateModuleDependency("module3"),
          ),
        librariesDependencies = listOf(IntermediateLibraryDependency("@maven//:lib1")),
      )

    val packageA1Path = createTempDirectory(projectRoot, "packageA1")
    packageA1Path.toFile().deleteOnExit()
    val packageA2Path = createTempDirectory(packageA1Path, "packageA2")
    packageA2Path.toFile().deleteOnExit()
    val file1APath = createTempFile(packageA2Path, "File1", ".java")
    file1APath.toFile().deleteOnExit()
    val file2APath = createTempFile(packageA2Path, "File2", ".java")
    file2APath.toFile().deleteOnExit()
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
            JavaSourceRoot(
              sourcePath = file2APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(Library(displayName = "lib1")),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = projectRootName.addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = givenJavaModule.genericModuleInfo.librariesDependencies,
        languageIds = listOf("java", "kotlin", "scala"),
      )

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = projectRoot.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  private fun createTempDirectoryAndDeleteItOnExit(path: Path, prefix: String) =
    createTempDirectory(path, prefix).also {
      it.toFile().deleteOnExit()
    }

  @Test
  fun `should return dummy module with sources and with resources in common path`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "hirschgarten")
    val projectRootName = projectRoot.name
    VirtualFileManager.getInstance().findFileByNioPath(projectRoot)?.also { project.rootDir = it }
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = emptyList(),
        librariesDependencies = listOf(IntermediateLibraryDependency("@maven//:lib1")),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
            JavaSourceRoot(
              sourcePath = file2APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = listOf(Library(displayName = "lib1")),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${javaPath.name}".addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = givenJavaModule.genericModuleInfo.librariesDependencies,
        languageIds = listOf("java", "scala", "kotlin"),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourcesPath.toAbsolutePath(),
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  @Test
  fun `should return dummy module with no sources and with single resources in common path`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "hirschgarten")
    VirtualFileManager.getInstance().findFileByNioPath(projectRoot)?.also { project.rootDir = it }
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = emptyList(),
        librariesDependencies = listOf(IntermediateLibraryDependency("@maven//:lib1")),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = listOf(Library(displayName = "lib1")),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${resourcesPath.name}.${messagesPath.name}".addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = givenJavaModule.genericModuleInfo.librariesDependencies,
        languageIds = listOf("java", "scala", "kotlin"),
      )

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = messagesPath.toAbsolutePath(),
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  fun `should return dummy module with no sources and with multiple resources in common path`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = emptyList(),
        librariesDependencies = listOf(IntermediateLibraryDependency("@maven//:lib1")),
      )
    val srcPath = createTempDirectoryAndDeleteItOnExit(projectRoot, "src")
    val mainPath = createTempDirectoryAndDeleteItOnExit(srcPath, "main")
    val resourcesPath = createTempDirectoryAndDeleteItOnExit(mainPath, "resources")
    val messagesPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "messages")
    val resourceFilePath = createTempFile(messagesPath, "Resources", ".properties")
    val iconsPath = createTempDirectoryAndDeleteItOnExit(resourcesPath, "icons")
    val iconFilePath = createTempFile(iconsPath, "icon", ".png")
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = listOf(Library(displayName = "lib1")),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${resourcesPath.name}".addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = givenJavaModule.genericModuleInfo.librariesDependencies,
        languageIds = listOf("java", "scala", "kotlin"),
      )

    val expectedJavaModule =
      JavaModule(
        genericModuleInfo = expectedModule,
        baseDirContentRoot = ContentRoot(path = projectRoot.toAbsolutePath()),
        sourceRoots =
          listOf(),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourcesPath.toAbsolutePath(),
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  @Test
  fun `should return dummy module with sources and with no resources outside project root`() {
    // given
    val projectRoot = createTempDirectoryAndDeleteItOnExit(projectBasePath, "module1")
    val projectRootName = projectRoot.name
    val javaVersion = "11"

    val givenModule =
      GenericModuleInfo(
        name = projectRootName,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = emptyList(),
        librariesDependencies = listOf(IntermediateLibraryDependency("@maven//:lib1")),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
            JavaSourceRoot(
              sourcePath = file2APath.toAbsolutePath(),
              generated = false,
              packagePrefix = packagePrefix,
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourceFilePath.toAbsolutePath(),
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        moduleLevelLibraries = listOf(Library(displayName = "lib1")),
        jvmJdkName = javaVersion,
        kotlinAddendum = null,
      )

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(givenJavaModule)

    // then
    val expectedModule =
      GenericModuleInfo(
        name = "$projectRootName.${srcPath.name}.${mainPath.name}.${javaPath.name}".addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = givenJavaModule.genericModuleInfo.librariesDependencies,
        languageIds = listOf("java", "scala", "kotlin"),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = givenJavaModule.jvmJdkName,
        kotlinAddendum = givenJavaModule.kotlinAddendum,
      )

    javaModules shouldContainExactlyInAnyOrder (
      listOf(expectedJavaModule) to { actual, expected -> validateJavaModule(actual, expected) }
    )
  }

  @Test
  fun `should return multiple dummy java modules for multiple modules`() {
    // given
    val projectRoot1 = createTempDirectory(projectBasePath, "module1")
    projectRoot1.toFile().deleteOnExit()
    val projectRoot1Name = projectRoot1.name

    val projectRoot2 = createTempDirectory(projectBasePath, "module2")
    projectRoot2.toFile().deleteOnExit()
    val projectRoot2Name = projectRoot2.name

    val file1Path = createTempFile(projectRoot1, "File1", ".java")
    file1Path.toFile().deleteOnExit()
    val file2Path = createTempFile(projectRoot2, "File2", ".java")
    file2Path.toFile().deleteOnExit()

    val givenModule1 =
      GenericModuleInfo(
        name = projectRoot1Name,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
      )

    val givenJavaModule1 =
      JavaModule(
        genericModuleInfo = givenModule1,
        baseDirContentRoot = ContentRoot(path = projectRoot1.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = file1Path.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val givenModule2 =
      GenericModuleInfo(
        name = projectRoot2Name,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
      )

    val givenJavaModule2 =
      JavaModule(
        genericModuleInfo = givenModule2,
        baseDirContentRoot = ContentRoot(path = projectRoot2.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = file2Path.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val modulesList = listOf(givenJavaModule1, givenJavaModule2)

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(modulesList)

    // then
    val expectedModule1 =
      GenericModuleInfo(
        name = projectRoot1Name.addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
      )

    val expectedJavaModule1 =
      JavaModule(
        genericModuleInfo = expectedModule1,
        baseDirContentRoot = ContentRoot(path = projectRoot1.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = projectRoot1.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val expectedModule2 =
      GenericModuleInfo(
        name = projectRoot2Name.addIntelliJDummyPrefix(),
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
      )

    val expectedJavaModule2 =
      JavaModule(
        genericModuleInfo = expectedModule2,
        baseDirContentRoot = ContentRoot(path = projectRoot2.toAbsolutePath()),
        sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = projectRoot2.toAbsolutePath(),
              generated = false,
              packagePrefix = "",
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    javaModules shouldContainExactlyInAnyOrder
      (listOf(expectedJavaModule1, expectedJavaModule2) to { actual, expected -> validateJavaModule(actual, expected) })
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
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val givenModule2 =
      GenericModuleInfo(
        name = projectRoot2Name,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val modulesList = listOf(givenJavaModule1, givenJavaModule2)

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(modulesList)

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
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
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
              rootType = JavaModuleToDummyJavaModulesTransformerHACK.DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            ),
          ),
        resourceRoots = listOf(),
        moduleLevelLibraries = listOf(),
        jvmJdkName = null,
        kotlinAddendum = null,
      )

    val modulesList = listOf(givenJavaModule)

    // when
    val javaModules = JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath, project).transform(modulesList)

    // then
    javaModules shouldBe emptyList()
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
    actual.moduleLevelLibraries shouldContainExactlyInAnyOrder expected.moduleLevelLibraries
  }

  private fun validateModule(actual: GenericModuleInfo, expected: GenericModuleInfo) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.modulesDependencies shouldContainExactlyInAnyOrder expected.modulesDependencies
    actual.librariesDependencies shouldContainExactlyInAnyOrder expected.librariesDependencies
  }
}

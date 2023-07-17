package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleWithSourcesUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleWithoutSourcesUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JvmJdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleCapabilities
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

@DisplayName("WorkspaceModelToProjectDetailsTransformer tests")
class WorkspaceModelToProjectDetailsTransformerTest : WorkspaceModelBaseTest() {

  @Nested
  @DisplayName("WorkspaceModelToProjectDetailsTransformer.JavaSourceRootToSourceItemTransformer tests")
  inner class JavaSourceRootToSourceItemTransformerTest {

    @Test
    fun `should return correct SourceItem for a file`() {
      // given
      val sourcePath11 = URI.create("file:///root/dir/example/package/one").toPath()
      val sourcePackagePrefix11 = "example.package.one"
      val sourceRoot1 = JavaSourceRoot(
        sourcePath = sourcePath11,
        generated = false,
        packagePrefix = sourcePackagePrefix11,
        rootType = "java-source",
        targetId = BuildTargetIdentifier("target"),
      )
      val module1 = Module(
        name = "module1",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
        ModuleCapabilities(canRun = true, canTest = true, canCompile = false, canDebug = false)
      )
      val compilerOutput1 = Path("compiler/output1")
      val baseDirContentRoot1 = ContentRoot(
        url = URI.create("file:///root/dir/example/").toPath()
      )
      val javaModule1 = JavaModule(
        module = module1,
        sourceRoots = listOf(sourceRoot1),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        baseDirContentRoot = baseDirContentRoot1,
        compilerOutput = compilerOutput1,
        jvmJdkInfo = JvmJdkInfo(name = "11", javaHome = "fake/path/to/local_jdk"),
        kotlinAddendum = null,
      )

      val expectedSourceItem = SourceItem("file:///root/dir/example/package/one", SourceItemKind.FILE, false)

      // when
      val workspaceModelEntityUpdaterConfig =
        WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)
      runTestWriteAction {
        JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig).addEntries(listOf(javaModule1))
      }
      // then
      val actual = loadedEntries(JavaSourceRootPropertiesEntity::class.java).map { WorkspaceModelToProjectDetailsTransformer.JavaSourceRootToSourceItemTransformer(it) }
      actual shouldBe listOf(expectedSourceItem)
    }

    @Test
    fun `should return correct SourceItem for a directory`() {
      // given
      val sourcePath11 = FileUtil.createTempDirectory("directory", "1", true).toPath()
      val sourcePackagePrefix11 = "example.package.one"
      val sourceRoot1 = JavaSourceRoot(
        sourcePath = sourcePath11,
        generated = false,
        packagePrefix = sourcePackagePrefix11,
        rootType = "java-source",
        targetId = BuildTargetIdentifier("target"),
      )
      val module1 = Module(
        name = "module1",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
        ModuleCapabilities(canRun = true, canTest = true, canCompile = false, canDebug = false)
      )
      val compilerOutput1 = Path("compiler/output1")
      val baseDirContentRoot1 = ContentRoot(
        url = URI.create("file:///root/dir/example/").toPath()
      )
      val javaModule1 = JavaModule(
        module = module1,
        sourceRoots = listOf(sourceRoot1),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        baseDirContentRoot = baseDirContentRoot1,
        compilerOutput = compilerOutput1,
        jvmJdkInfo = null,
        kotlinAddendum = null,
      )

      val expectedSourceItem = SourceItem(sourcePath11.toUri().toString(), SourceItemKind.DIRECTORY, false)

      // when
      val workspaceModelEntityUpdaterConfig =
        WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)
      runTestWriteAction {
        JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig).addEntries(listOf(javaModule1))
      }
      // then
      val actual = loadedEntries(JavaSourceRootPropertiesEntity::class.java).map { WorkspaceModelToProjectDetailsTransformer.JavaSourceRootToSourceItemTransformer(it) }
      actual shouldBe listOf(expectedSourceItem)
    }

  }

  @Nested
  @DisplayName("WorkspaceModelToProjectDetailsTransformer.EntitiesToProjectDetailsTransformer tests")
  inner class EntitiesToProjectDetailsTransformerTest {

    @Test
    fun `should return correct ProjectDetails`() {
      // given
      val module1 = Module(
        name = "module1",
        type = "JAVA_MODULE",
        modulesDependencies = listOf(
          ModuleDependency(
            moduleName = "module2",
          ),
          ModuleDependency(
            moduleName = "module3",
          ),
        ),
        librariesDependencies = listOf(
          LibraryDependency(
            libraryName = "lib1",
            isProjectLevelLibrary = false
          ),
          LibraryDependency(
            libraryName = "lib2",
            isProjectLevelLibrary = false
          ),
        ),
        ModuleCapabilities(canRun = true, canTest = true, canCompile = false, canDebug = false)
      )

      val baseDirContentRoot1 = ContentRoot(
        url = URI.create("file:///root/dir/example/").toPath()
      )

      val sourcePath11 = URI.create("file:///root/dir/example/package/one").toPath()
      val sourcePackagePrefix11 = "example.package.one"
      val sourcePath12 = URI.create("file:///root/dir/example/package/two").toPath()
      val sourcePackagePrefix12 = "example.package.two"
      val sourceRoots1 = listOf(
        JavaSourceRoot(
          sourcePath = sourcePath11,
          generated = false,
          packagePrefix = sourcePackagePrefix11,
          rootType = "java-source",
          targetId = BuildTargetIdentifier("target"),
        ),
        JavaSourceRoot(
          sourcePath = sourcePath12,
          generated = false,
          packagePrefix = sourcePackagePrefix12,
          rootType = "java-source",
          targetId = BuildTargetIdentifier("target"),
        ),
      )

      val resourcePath11 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
      val resourcePath12 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
      val resourceRoots1 = listOf(
        JavaResourceRoot(
          resourcePath = resourcePath11,
        ),
        JavaResourceRoot(
          resourcePath = resourcePath12,
        ),
      )

      val library1 = Library(
        displayName = "lib1",
        sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
        classesJar = "jar:///lib1/1.0.0/lib1-1.0.0.jar!/",
      )
      val library2 = Library(
        displayName = "lib2",
        sourcesJar = "jar:///lib2/2.0.0/lib2-2.0.0-sources.jar!/",
        classesJar = "jar:///lib2/2.0.0/lib2-2.0.0.jar!/",
      )

      val libraries1 = listOf(
        library1,
        library2,
      )
      val compilerOutput1 = Path("compiler/output1")

      val javaModule1 = JavaModule(
        module = module1,
        sourceRoots = sourceRoots1,
        resourceRoots = resourceRoots1,
        moduleLevelLibraries = libraries1,
        baseDirContentRoot = baseDirContentRoot1,
        compilerOutput = compilerOutput1,
        jvmJdkInfo = JvmJdkInfo(name = "11", javaHome = "fake/path/to/local_jdk"),
        kotlinAddendum = null,
      )

      val module2 = Module(
        name = "module2",
        type = "JAVA_MODULE",
        modulesDependencies = listOf(
          ModuleDependency(
            moduleName = "module3",
          ),
        ),
        librariesDependencies = listOf(
          LibraryDependency(
            libraryName = "lib1",
            isProjectLevelLibrary = false
          ),
        ),
        ModuleCapabilities(canRun = true, canTest = false, canCompile = true, canDebug = false)
      )

      val baseDirContentRoot2 = ContentRoot(
        url = URI.create("file:///another/root/dir/example/").toPath()
      )

      val sourcePath21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
      val sourcePackagePrefix21 = "another.example.package"
      val sourceRoots2 = listOf(
        JavaSourceRoot(
          sourcePath = sourcePath21,
          generated = false,
          packagePrefix = sourcePackagePrefix21,
          rootType = "java-test",
          targetId = BuildTargetIdentifier("target"),
        ),
      )

      val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
      val resourceRoots2 = listOf(
        JavaResourceRoot(
          resourcePath = resourcePath21,
        ),
      )

      val libraries2 = listOf(
        library1,
      )
      val compilerOutput2 = Path("compiler/output2")

      val javaModule2 = JavaModule(
        module = module2,
        baseDirContentRoot = baseDirContentRoot2,
        sourceRoots = sourceRoots2,
        resourceRoots = resourceRoots2,
        moduleLevelLibraries = libraries2,
        compilerOutput = compilerOutput2,
        jvmJdkInfo = null,
        kotlinAddendum = null,
      )

      val module3 = Module(
        name = "module3",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
        ModuleCapabilities(canRun = true, canTest = true, canCompile = true, canDebug = true)
      )
      val baseDirContentRoot3 = ContentRoot(
        url = URI.create("file:///another/root/dir/example3/").toPath()
      )
      val compilerOutput3 = Path("compiler/output3")

      val javaModule3 = JavaModule(
        module = module3,
        baseDirContentRoot = baseDirContentRoot3,
        sourceRoots = emptyList(),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        compilerOutput = compilerOutput3,
        jvmJdkInfo = JvmJdkInfo(name = "13", javaHome = "fake/path/to/local_jdk"),
        kotlinAddendum = null,
      )

      val rootModule = Module(
        name = ".root",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
        ModuleCapabilities()
      )
      val rootModuleBaseDirContentRoot = ContentRoot(
        url = projectBasePath,
        excludedPaths = listOf("file:///output/dir", "file:///output/file.out").map { URI.create(it).toPath() }
      )

      val rootJavaModule = JavaModule(
        module = rootModule,
        baseDirContentRoot = rootModuleBaseDirContentRoot,
        sourceRoots = emptyList(),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        compilerOutput = null,
        jvmJdkInfo = null,
        kotlinAddendum = null,
      )

      // when

      val workspaceModelEntityUpdaterConfig =
        WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)
      runTestWriteAction {
        JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig).addEntries(listOf(javaModule1, javaModule2, rootJavaModule))
        JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig).addEntries(listOf(javaModule3))
      }
      val loadedModules = loadedEntries(ModuleEntity::class.java)
      val sourceRoots = loadedEntries(SourceRootEntity::class.java)
      val libraries = loadedEntries(LibraryEntity::class.java)
      val projectDetails =
        WorkspaceModelToProjectDetailsTransformer.EntitiesToProjectDetailsTransformer(
          loadedModules.asSequence(),
          sourceRoots.asSequence(),
          libraries.asSequence(),
          listOf(module1, module2, rootModule, module3).associate {
            it.name to BuildTargetIdentifier(it.name)
          }
        )

      // then

      val expectedBuildTargetId1 = BuildTargetIdentifier("module1")
      val expectedBuildTargetId2 = BuildTargetIdentifier("module2")
      val expectedBuildTargetId3 = BuildTargetIdentifier("module3")
      val expectedRootBuildTargetId = BuildTargetIdentifier(".root")
      val expectedBuildTarget1 = BuildTarget(
        expectedBuildTargetId1,
        emptyList(),
        emptyList(),
        listOf(
          expectedBuildTargetId2,
          expectedBuildTargetId3
        ),
        BuildTargetCapabilities(false, true, true, false)
      ).apply {
        dataKind = "jvm"
        data = """{"javaHome":"","javaVersion":"11"}"""
      }
      val expectedBuildTarget2 = BuildTarget(
        expectedBuildTargetId2,
        emptyList(),
        emptyList(),
        listOf(
          expectedBuildTargetId3
        ),
        BuildTargetCapabilities(true, false, true, false)
      )
      val expectedBuildTarget3 = BuildTarget(
        expectedBuildTargetId3,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities(true, true, true, true)
      )
      val expectedRootBuildTarget = BuildTarget(
        expectedRootBuildTargetId,
        emptyList(),
        emptyList(),
        emptyList(),
        BuildTargetCapabilities()
      ).also { it.baseDirectory = projectBasePath.toUri().toString() }

      val expectedSourcesItem1 = SourcesItem(
        expectedBuildTargetId1,
        listOf(
          SourceItem("file:///root/dir/example/package/one", SourceItemKind.FILE, false),
          SourceItem("file:///root/dir/example/package/two", SourceItemKind.FILE, false)
        )
      )
      val expectedSourcesItem2 = SourcesItem(
        expectedBuildTargetId2,
        listOf(
          SourceItem("file:///another/root/dir/another/example/package", SourceItemKind.FILE, false)
        )
      )
      val expectedResourceItem1 = ResourcesItem(
        expectedBuildTargetId1, listOf(
          "file:///root/dir/example/resource/File1.txt",
          "file:///root/dir/example/resource/File2.txt",
        )
      )
      val expectedResourceItem2 = ResourcesItem(
        expectedBuildTargetId2, listOf(
          "file:///another/root/dir/another/example/resource/File1.txt"
        )
      )
      val expectedDependencySourceItem1 = DependencySourcesItem(
        expectedBuildTargetId1,
        listOf(
          "file:///lib1/1.0.0/lib1-1.0.0-sources.jar",
          "file:///lib2/2.0.0/lib2-2.0.0-sources.jar"
        )
      )
      val expectedDependencySourceItem2 = DependencySourcesItem(
        expectedBuildTargetId2,
        listOf("file:///lib1/1.0.0/lib1-1.0.0-sources.jar")
      )
      val expectedJavacSourceItem1 = JavacOptionsItem(
        expectedBuildTargetId1,
        emptyList(),
        listOf(
          "file:///lib1/1.0.0/lib1-1.0.0.jar",
          "file:///lib2/2.0.0/lib2-2.0.0.jar"
        ),
        "",
      )
      val expectedJavacSourceItem2 = JavacOptionsItem(
        expectedBuildTargetId2,
        emptyList(),
        listOf("file:///lib1/1.0.0/lib1-1.0.0.jar"),
        "",
      )
      val expectedProjectDetails = ProjectDetails(
        targetsId =
          listOf(expectedBuildTargetId1, expectedBuildTargetId2, expectedRootBuildTargetId, expectedBuildTargetId3),
        targets = setOf(expectedBuildTarget1, expectedBuildTarget2, expectedRootBuildTarget, expectedBuildTarget3),
        sources = listOf(expectedSourcesItem1, expectedSourcesItem2),
        resources = listOf(expectedResourceItem1, expectedResourceItem2),
        dependenciesSources = listOf(expectedDependencySourceItem1, expectedDependencySourceItem2),
        javacOptions = listOf(expectedJavacSourceItem1, expectedJavacSourceItem2),
        pythonOptions = emptyList(),
        outputPathUris = rootModuleBaseDirContentRoot.excludedPaths.map { it.toUri().toString() },
        libraries = null
      )

      projectDetails shouldBe expectedProjectDetails
    }
  }
}

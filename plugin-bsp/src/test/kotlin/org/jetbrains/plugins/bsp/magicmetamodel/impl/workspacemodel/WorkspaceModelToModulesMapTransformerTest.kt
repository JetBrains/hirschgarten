package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleWithSourcesUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleWithoutSourcesUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("WorkspaceModelToProjectDetailsTransformer tests")
class WorkspaceModelToModulesMapTransformerTest : WorkspaceModelBaseTest() {
  @Nested
  @DisplayName("WorkspaceModelToProjectDetailsTransformer.EntitiesToProjectDetailsTransformer tests")
  inner class WorkspaceModelToMagicMetamodelTransformerTest {
    @Test
    fun `should return correct ProjectDetails`() {
      // given
      val module1 = GenericModuleInfo(
        name = "module1",
        type = "JAVA_MODULE",
        modulesDependencies = listOf(
          IntermediateModuleDependency(
            moduleName = "module2",
          ),
          IntermediateModuleDependency(
            moduleName = "module3",
          ),
        ),
        librariesDependencies = listOf(
          IntermediateLibraryDependency(
            libraryName = "lib1",
          ),
          IntermediateLibraryDependency(
            libraryName = "lib2",
          ),
        ),
      )

      val baseDirContentRoot1 = ContentRoot(
        path = URI.create("file:///root/dir/example/").toPath(),
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
        ),
        JavaSourceRoot(
          sourcePath = sourcePath12,
          generated = false,
          packagePrefix = sourcePackagePrefix12,
          rootType = "java-source",
        ),
      )

      val resourcePath11 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
      val resourcePath12 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
      val resourceRoots1 = listOf(
        ResourceRoot(
          resourcePath = resourcePath11,
          rootType = "",
        ),
        ResourceRoot(
          resourcePath = resourcePath12,
          rootType = "",
        ),
      )

      val library1 = Library(
        displayName = "lib1",
        sourceJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/"),
        classJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0.jar!/"),
      )
      val library2 = Library(
        displayName = "lib2",
        sourceJars = listOf("jar:///lib2/2.0.0/lib2-2.0.0-sources.jar!/"),
        classJars = listOf("jar:///lib2/2.0.0/lib2-2.0.0.jar!/"),
      )

      val libraries1 = listOf(
        library1,
        library2,
      )

      val javaModule1 = JavaModule(
        genericModuleInfo = module1,
        sourceRoots = sourceRoots1,
        resourceRoots = resourceRoots1,
        moduleLevelLibraries = libraries1,
        baseDirContentRoot = baseDirContentRoot1,
        jvmJdkName = "11",
      )

      val module2 = GenericModuleInfo(
        name = "module2",
        type = "JAVA_MODULE",
        modulesDependencies = listOf(
          IntermediateModuleDependency(
            moduleName = "module3",
          ),
        ),
        librariesDependencies = listOf(
          IntermediateLibraryDependency(
            libraryName = "lib1",
          ),
        ),
      )

      val baseDirContentRoot2 = ContentRoot(
        path = URI.create("file:///another/root/dir/example/").toPath(),
      )

      val sourcePath21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
      val sourcePackagePrefix21 = "another.example.package"
      val sourceRoots2 = listOf(
        JavaSourceRoot(
          sourcePath = sourcePath21,
          generated = false,
          packagePrefix = sourcePackagePrefix21,
          rootType = "java-test",
        ),
      )

      val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
      val resourceRoots2 = listOf(
        ResourceRoot(
          resourcePath = resourcePath21,
          rootType = "",
        ),
      )

      val libraries2 = listOf(
        library1,
      )

      val javaModule2 = JavaModule(
        genericModuleInfo = module2,
        baseDirContentRoot = baseDirContentRoot2,
        sourceRoots = sourceRoots2,
        resourceRoots = resourceRoots2,
        moduleLevelLibraries = libraries2,
        jvmJdkName = null,
      )

      val module3 = GenericModuleInfo(
        name = "module3",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
      )
      val baseDirContentRoot3 = ContentRoot(
        path = URI.create("file:///another/root/dir/example3/").toPath(),
      )

      val javaModule3 = JavaModule(
        genericModuleInfo = module3,
        baseDirContentRoot = baseDirContentRoot3,
        sourceRoots = emptyList(),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
      )

      val rootModule = GenericModuleInfo(
        name = ".root",
        type = "JAVA_MODULE",
        modulesDependencies = emptyList(),
        librariesDependencies = emptyList(),
      )

      val rootJavaModule = JavaModule(
        genericModuleInfo = rootModule,
        baseDirContentRoot = null,
        sourceRoots = emptyList(),
        resourceRoots = emptyList(),
        moduleLevelLibraries = emptyList(),
        jvmJdkName = null,
      )

      // when

      val workspaceModelEntityUpdaterConfig =
        WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
      runTestWriteAction {
        JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig, projectBasePath, false).addEntries(listOf(javaModule1, javaModule2, rootJavaModule))
        JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig).addEntries(listOf(javaModule3))
      }
      val loadedModules = loadedEntries(ModuleEntity::class.java)
      val sourceRoots = loadedEntries(SourceRootEntity::class.java)
      val libraries = loadedEntries(LibraryEntity::class.java)
      val parseResult =
        WorkspaceModelToModulesMapTransformer.WorkspaceModelToMagicMetamodelTransformer(
          loadedModules.asSequence(),
          sourceRoots.asSequence(),
          libraries.asSequence(),
          listOf(module1, module2, rootModule, module3).associate {
            it.name to it.name
          },
        )

      // then
      val actualJavaModule1 = javaModule1.copy(
        // not a root module, so baseDirContentRoot won't be serialized
        baseDirContentRoot = null,
      )

      val actualJavaModule2 = javaModule2.copy(
        // not a root module, so baseDirContentRoot won't be serialized
        baseDirContentRoot = null,
      )

      val actualJavaModule3 = javaModule3.copy(
        // not a root module, so baseDirContentRoot won't be serialized
        baseDirContentRoot = null,
        // no sources, so compiler output won't be serialized
      )

      val expectedMap = mapOf(
        ".root" to rootJavaModule,
        "module1" to actualJavaModule1,
        "module2" to actualJavaModule2,
        "module3" to actualJavaModule3,
      )

      parseResult shouldBe expectedMap
    }
  }
}

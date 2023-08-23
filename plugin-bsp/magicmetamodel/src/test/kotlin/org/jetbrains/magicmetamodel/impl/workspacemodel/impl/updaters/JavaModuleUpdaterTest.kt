@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

// TODO add libraries tests
internal class JavaModuleUpdaterTest : WorkspaceModelBaseTest() {
  @Nested
  @DisplayName("javaModuleWithSourcesUpdater.addEntity(entityToAdd) tests")
  inner class JavaModuleWithSourcesUpdaterTest {
    @Test
    fun `should add one java module with sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module = GenericModuleInfo(
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
              isProjectLevelLibrary = false,
            ),
            LibraryDependency(
              libraryName = "lib2",
              isProjectLevelLibrary = false,
            ),
          ),
        )

        val baseDirContentRoot = ContentRoot(
          path = URI.create("file:///root/dir/example/").toPath(),
        )

        val sourcePath1 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePackagePrefix1 = "example.package.one"
        val sourcePath2 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourcePackagePrefix2 = "example.package.two"
        val sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = sourcePath1,
            generated = false,
            packagePrefix = sourcePackagePrefix1,
            rootType = "java-source",
          ),
          JavaSourceRoot(
            sourcePath = sourcePath2,
            generated = false,
            packagePrefix = sourcePackagePrefix2,
            rootType = "java-source",
          ),
        )

        val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
        val resourceRoots = listOf(
          ResourceRoot(
            resourcePath = resourcePath1,
          ),
          ResourceRoot(
            resourcePath = resourcePath2,
          ),
        )

        val libraries = listOf(
          Library(
            displayName = "lib1",
            sourceJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/"),
            classJars = listOf("jar:///lib1/1.0.0/lib1.jar!/"),
          ),
          Library(
            displayName = "lib2",
            sourceJars = listOf("jar:///lib2/1.0.0/lib2-2.0.0-sources.jar!/"),
            classJars = listOf("jar:///lib2/1.0.0/lib2-2.0.0.jar!/"),
          ),
        )
        val compilerOutput = Path("compiler/output")

        val javaModule = JavaModule(
          genericModuleInfo = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = sourceRoots,
          resourceRoots = resourceRoots,
          moduleLevelLibraries = libraries,
          compilerOutput = compilerOutput,
          jvmJdkName = "test-proj-11",
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = BspEntitySource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module2"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("test-proj-11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "JAVA_MODULE"
            javaSettings = JavaModuleSettingsEntity(
              inheritedCompilerOutput = false,
              excludeOutput = true,
              entitySource = BspEntitySource,
            ) {
              this.compilerOutput = Path("compiler/output").toVirtualFileUrl(virtualFileUrlManager)
            }
          },
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            rootType = "java-source",
          ) {
            javaSourceRoots = listOf(
              JavaSourceRootPropertiesEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                generated = false,
                packagePrefix = sourcePackagePrefix1,
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualSourceDir2 = sourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            rootType = "java-source",
          ) {
            javaSourceRoots = listOf(
              JavaSourceRootPropertiesEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                generated = false,
                packagePrefix = sourcePackagePrefix2,
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            rootType = "java-resource",
          ) {
            javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            rootType = "java-resource",
          ) {
            javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity1,
          expectedJavaSourceRootEntity2,
          expectedJavaResourceRootEntity1,
          expectedJavaResourceRootEntity2,
        )
      }
    }

    @Test
    fun `should add multiple java module with sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module1 = GenericModuleInfo(
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
              isProjectLevelLibrary = false,
            ),
            LibraryDependency(
              libraryName = "lib2",
              isProjectLevelLibrary = false,
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
          ),
          ResourceRoot(
            resourcePath = resourcePath12,
          ),
        )

        val libraries1 = listOf(
          Library(
            displayName = "lib1",
            sourceJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/"),
            classJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0.jar!/"),
          ),
          Library(
            displayName = "lib2",
            sourceJars = listOf("jar:///lib2/2.0.0/lib2-2.0.0-sources.jar!/"),
            classJars = listOf("jar:///lib2/1.0.0/lib2-2.0.0.jar!/"),
          ),
        )
        val compilerOutput1 = Path("compiler/output1")

        val javaModule1 = JavaModule(
          genericModuleInfo = module1,
          sourceRoots = sourceRoots1,
          resourceRoots = resourceRoots1,
          moduleLevelLibraries = libraries1,
          baseDirContentRoot = baseDirContentRoot1,
          compilerOutput = compilerOutput1,
          jvmJdkName = "test-proj-11",
          kotlinAddendum = null,
        )

        val module2 = GenericModuleInfo(
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
              isProjectLevelLibrary = false,
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
          ),
        )

        val libraries2 = listOf(
          Library(
            displayName = "lib1",
            sourceJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/"),
            classJars = listOf("jar:///lib1/1.0.0/lib1-1.0.0.jar!/"),
          ),
        )
        val compilerOutput2 = Path("compiler/output2")

        val javaModule2 = JavaModule(
          genericModuleInfo = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = sourceRoots2,
          resourceRoots = resourceRoots2,
          moduleLevelLibraries = libraries2,
          compilerOutput = compilerOutput2,
          jvmJdkName = "test-proj-11",
          kotlinAddendum = null,
        )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = BspEntitySource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module2"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("test-proj-11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "JAVA_MODULE"
            javaSettings = JavaModuleSettingsEntity(
              inheritedCompilerOutput = false,
              excludeOutput = true,
              entitySource = BspEntitySource,
            ) {
              this.compilerOutput = Path("compiler/output1").toVirtualFileUrl(virtualFileUrlManager)
            }
          },
        )

        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = BspEntitySource,
            dependencies = listOf(
              ModuleDependencyItem.Exportable.ModuleDependency(
                module = ModuleId("module3"),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
                productionOnTest = true,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib1",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module2")),
                ),
                exported = true,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("test-proj-11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "JAVA_MODULE"
            javaSettings = JavaModuleSettingsEntity(
              inheritedCompilerOutput = false,
              excludeOutput = true,
              entitySource = BspEntitySource,
            ) {
              this.compilerOutput = Path("compiler/output2").toVirtualFileUrl(virtualFileUrlManager)
            }
          },
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            rootType = "java-source",
          ) {
            javaSourceRoots = listOf(
              JavaSourceRootPropertiesEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                generated = false,
                packagePrefix = sourcePackagePrefix11,
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir12 = sourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            rootType = "java-source",
          ) {
            javaSourceRoots = listOf(
              JavaSourceRootPropertiesEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                generated = false,
                packagePrefix = sourcePackagePrefix12,
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir21 = sourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            rootType = "java-test",
          ) {
            javaSourceRoots = listOf(
              JavaSourceRootPropertiesEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                generated = false,
                packagePrefix = sourcePackagePrefix21,
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            rootType = "java-resource",
          ) {
            javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            rootType = "java-resource",
          ) {
            javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            rootType = "java-resource",
          ) {
            javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                generated = false,
                relativeOutputPath = "",
              ),
            )
          },
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity11,
          expectedJavaSourceRootEntity12,
          expectedJavaSourceRootEntity21,
          expectedJavaResourceRootEntity11,
          expectedJavaResourceRootEntity12,
          expectedJavaResourceRootEntity21,
        )
      }
    }
  }

  @Nested
  @DisplayName("javaModuleWithoutSourcesUpdater.addEntity(entityToAdd) tests")
  inner class JavaModuleWithoutSourcesUpdaterTest {
    @Test
    fun `should add one java module without sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithoutSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module = GenericModuleInfo(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
          languageIds = listOf("java"),
        )

        val baseDirContentRootPath = URI.create("file:///root/dir/").toPath()
        val baseDirContentRoot = ContentRoot(
          path = baseDirContentRootPath,
        )

        val javaModule = JavaModule(
          genericModuleInfo = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          moduleLevelLibraries = emptyList(),
          compilerOutput = null,
          jvmJdkName = null,
          kotlinAddendum = null,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = BspEntitySource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          },
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)
      }
    }

    @Test
    fun `should add multiple java module without sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithoutSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module1 = GenericModuleInfo(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
          languageIds = listOf("java"),
        )

        val baseDirContentRootPath1 = URI.create("file:///root/dir1/").toPath()
        val baseDirContentRoot1 = ContentRoot(
          path = baseDirContentRootPath1,
        )

        val javaModule1 = JavaModule(
          genericModuleInfo = module1,
          baseDirContentRoot = baseDirContentRoot1,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          moduleLevelLibraries = emptyList(),
          compilerOutput = null,
          jvmJdkName = null,
          kotlinAddendum = null,
        )

        val module2 = GenericModuleInfo(
          name = "module2",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
          languageIds = listOf("java"),
        )

        val baseDirContentRootPath2 = URI.create("file:///root/dir2/").toPath()
        val baseDirContentRoot2 = ContentRoot(
          path = baseDirContentRootPath2,
        )

        val javaModule2 = JavaModule(
          genericModuleInfo = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          moduleLevelLibraries = emptyList(),
          compilerOutput = null,
          jvmJdkName = null,
          kotlinAddendum = null,
        )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = BspEntitySource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          },
        )
        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = BspEntitySource,
            dependencies = emptyList(),
          ) {
            type = "JAVA_MODULE"
          },
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
      }
    }
  }

  private fun runTestForUpdaters(
    updaters: List<KClass<out WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) =
    updaters
      .map { it.primaryConstructor!! }
      .forEach { runTest(it, test) }

  private fun runTest(
    updaterConstructor: KFunction<WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) {
    beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)

    if (updaterConstructor.parameters.size == 1)
      test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
    else if (updaterConstructor.parameters.size == 2)
      test(updaterConstructor.call(workspaceModelEntityUpdaterConfig, projectBasePath))
  }
}

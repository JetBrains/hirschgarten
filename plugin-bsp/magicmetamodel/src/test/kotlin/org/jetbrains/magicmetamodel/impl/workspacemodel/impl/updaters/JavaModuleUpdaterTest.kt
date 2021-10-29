@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.workspace.model.matchers.entries.ExpectedContentRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaResourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedJavaSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
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
        val module = Module(
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
            ),
            LibraryDependency(
              libraryName = "lib2",
            ),
          ),
        )

        val baseDirContentRoot = ContentRoot(
          url = URI.create("file:///root/dir/example/").toPath()
        )

        val sourceDir1 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePackagePrefix1 = "example.package.one"
        val sourceDir2 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourcePackagePrefix2 = "example.package.two"
        val sourceRoots = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir1,
            generated = false,
            packagePrefix = sourcePackagePrefix1,
          ),
          JavaSourceRoot(
            sourceDir = sourceDir2,
            generated = false,
            packagePrefix = sourcePackagePrefix2,
          ),
        )

        val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()
        val resourceRoots = listOf(
          JavaResourceRoot(
            resourcePath = resourcePath1,
          ),
          JavaResourceRoot(
            resourcePath = resourcePath2,
          ),
        )

        val libraries = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1.jar!/",
          ),
          Library(
            displayName = "lib2",
            sourcesJar = "jar:///lib2/1.0.0/lib2-2.0.0-sources.jar!/",
            classesJar = "jar:///lib2/1.0.0/lib2-2.0.0.jar!/",
          ),
        )

        val javaModule = JavaModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = sourceRoots,
          resourceRoots = resourceRoots,
          libraries = libraries,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            type = "JAVA_MODULE",
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
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          )
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity1 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualSourceDir1, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir1, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix1),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity2 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualSourceDir2, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir2, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix2),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity1,
          expectedJavaSourceRootEntity2
        )

        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity1 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualResourceUrl1, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl1, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity2 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualResourceUrl2, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl2, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaResourceRootEntity1,
          expectedJavaResourceRootEntity2
        )
      }
    }

    @Test
    fun `should add multiple java module with sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
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
            ),
            LibraryDependency(
              libraryName = "lib2",
            ),
          ),
        )

        val baseDirContentRoot1 = ContentRoot(
          url = URI.create("file:///root/dir/example/").toPath()
        )

        val sourceDir11 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePackagePrefix11 = "example.package.one"
        val sourceDir12 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourcePackagePrefix12 = "example.package.two"
        val sourceRoots1 = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir11,
            generated = false,
            packagePrefix = sourcePackagePrefix11,
          ),
          JavaSourceRoot(
            sourceDir = sourceDir12,
            generated = false,
            packagePrefix = sourcePackagePrefix12,
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

        val libraries1 = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1-1.0.0.jar!/",
          ),
          Library(
            displayName = "lib2",
            sourcesJar = "jar:///lib2/2.0.0/lib2-2.0.0-sources.jar!/",
            classesJar = "jar:///lib2/1.0.0/lib2-2.0.0.jar!/",
          ),
        )

        val javaModule1 = JavaModule(
          module = module1,
          sourceRoots = sourceRoots1,
          resourceRoots = resourceRoots1,
          libraries = libraries1,
          baseDirContentRoot = baseDirContentRoot1,
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
            ),
          ),
        )

        val baseDirContentRoot2 = ContentRoot(
          url = URI.create("file:///another/root/dir/example/").toPath()
        )

        val sourceDir21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
        val sourcePackagePrefix21 = "another.example.package"
        val sourceRoots2 = listOf(
          JavaSourceRoot(
            sourceDir = sourceDir21,
            generated = false,
            packagePrefix = sourcePackagePrefix21,
          ),
        )

        val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
        val resourceRoots2 = listOf(
          JavaResourceRoot(
            resourcePath = resourcePath21,
          ),
        )

        val libraries2 = listOf(
          Library(
            displayName = "lib1",
            sourcesJar = "jar:///lib1/1.0.0/lib1-1.0.0-sources.jar!/",
            classesJar = "jar:///lib1/1.0.0/lib1-1.0.0.jar!/",
          ),
        )

        val javaModule2 = JavaModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = sourceRoots2,
          resourceRoots = resourceRoots2,
          libraries = libraries2,
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
            type = "JAVA_MODULE",
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
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.Exportable.LibraryDependency(
                library = LibraryId(
                  name = "lib2",
                  tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
                ),
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          )
        )

        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            type = "JAVA_MODULE",
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
                exported = false,
                scope = ModuleDependencyItem.DependencyScope.COMPILE,
              ),
              ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
              ModuleDependencyItem.ModuleSourceDependency,
            )
          )
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourceDir11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity11 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualSourceDir11, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir11, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix11),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir12 = sourceDir12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity12 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualSourceDir12, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir12, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix12),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir21 = sourceDir21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity21 = ExpectedJavaSourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualSourceDir21, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir21, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix21),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaSourceRootEntity11,
          expectedJavaSourceRootEntity12,
          expectedJavaSourceRootEntity21
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity11 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualResourceUrl11, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl11, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity12 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualResourceUrl12, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl12, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntity21 = ExpectedJavaResourceRootEntity(
          contentRootEntity = ContentRootEntity(virtualResourceUrl21, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl21, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedJavaResourceRootEntity11,
          expectedJavaResourceRootEntity12,
          expectedJavaResourceRootEntity21
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
        val module = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath = URI.create("file:///root/dir/").toPath()
        val baseDirContentRoot = ContentRoot(
          url = baseDirContentRootPath,
        )

        val javaModule = JavaModule(
          module = module,
          baseDirContentRoot = baseDirContentRoot,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            type = "JAVA_MODULE",
            dependencies = emptyList(),
          )
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualBaseDirContentRootPath = baseDirContentRootPath.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath, emptyList(), emptyList()),
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedContentRootEntity)
      }
    }

    @Test
    fun `should add multiple java module without sources to the workspace model`() {
      runTestForUpdaters(listOf(JavaModuleWithoutSourcesUpdater::class, JavaModuleUpdater::class)) { updater ->
        // given
        val module1 = Module(
          name = "module1",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath1 = URI.create("file:///root/dir1/").toPath()
        val baseDirContentRoot1 = ContentRoot(
          url = baseDirContentRootPath1,
        )

        val javaModule1 = JavaModule(
          module = module1,
          baseDirContentRoot = baseDirContentRoot1,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
        )

        val module2 = Module(
          name = "module2",
          type = "JAVA_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val baseDirContentRootPath2 = URI.create("file:///root/dir2/").toPath()
        val baseDirContentRoot2 = ContentRoot(
          url = baseDirContentRootPath2,
        )

        val javaModule2 = JavaModule(
          module = module2,
          baseDirContentRoot = baseDirContentRoot2,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
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
            type = "JAVA_MODULE",
            dependencies = emptyList(),
          )
        )
        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            type = "JAVA_MODULE",
            dependencies = emptyList(),
          )
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualBaseDirContentRootPath1 = baseDirContentRootPath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity1 = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath1, emptyList(), emptyList()),
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )

        val virtualBaseDirContentRootPath2 = baseDirContentRootPath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntity2 = ExpectedContentRootEntity(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath2, emptyList(), emptyList()),
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedContentRootEntity1,
          expectedContentRootEntity2
        )
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
      WorkspaceModelEntityUpdaterConfig(workspaceModel, virtualFileUrlManager, projectConfigSource)

    test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
  }
}

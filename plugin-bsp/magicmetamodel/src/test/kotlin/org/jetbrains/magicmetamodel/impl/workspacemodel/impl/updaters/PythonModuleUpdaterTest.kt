@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.workspacemodel.entities.BspEntitySource
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

internal class PythonModuleUpdaterTest : WorkspaceModelBaseTest() {
  @Nested
  @DisplayName("pythonModuleWithSourcesUpdater.addEntity(entityToAdd) tests")
  inner class PythonModuleWithSourcesUpdaterTest {
    @Test
    fun `should add one python module with sources to the workspace model`() {
      runTestForUpdaters(isPythonSupportEnabled = true, listOf(PythonModuleWithSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module = GenericModuleInfo(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(),
        )

        val sourcePath1 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePath2 = URI.create("file:///root/dir/example/package/two").toPath()

        val sourceRoots = listOf(
          GenericSourceRoot(
            sourcePath = sourcePath1,
            rootType = "python-source",
          ),
          GenericSourceRoot(
            sourcePath = sourcePath2,
            rootType = "python-source",
          ),
        )

        val resourcePath1 = URI.create("file:///root/dir/example/resource/File1.txt").toPath()
        val resourcePath2 = URI.create("file:///root/dir/example/resource/File2.txt").toPath()

        val resourceRoots = listOf(
          ResourceRoot(
            resourcePath = resourcePath1,
            rootType = "",
          ),
          ResourceRoot(
            resourcePath = resourcePath2,
            rootType = "",
          ),
        )

        val sdkInfo = PythonSdkInfo(version = "3", originalName = "fake-interpreter-name")

        val pythonModule = PythonModule(
          module = module,
          sourceRoots = sourceRoots,
          resourceRoots = resourceRoots,
          libraries = listOf(),
          sdkInfo = sdkInfo,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(pythonModule)
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
              ModuleDependencyItem.ModuleSourceDependency,
              ModuleDependencyItem.SdkDependency(SdkId("fake-interpreter-name-3", "PythonSDK")),
            ),
          ) {
            type = "PYTHON_MODULE"
          },
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity

        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir1,
            rootType = "python-source",
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        val virtualSourceDir2 = sourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualSourceDir2,
            rootType = "python-source",
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedResourceRootEntity1 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl1,
            rootType = "python-resource",
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedResourceRootEntity2 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity.moduleEntity.entitySource,
            url = virtualResourceUrl2,
            rootType = "python-resource",
          ) {
          },
          parentModuleEntity = expectedModuleEntity.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedPythonSourceRootEntity1,
          expectedPythonSourceRootEntity2,
          expectedResourceRootEntity1,
          expectedResourceRootEntity2,
        )
      }
    }

    @Test
    fun `should add multiple python modules with sources to the workspace model`() {
      runTestForUpdaters(isPythonSupportEnabled = true, listOf(PythonModuleWithSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module1 = GenericModuleInfo(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(),
        )

        val sourcePath11 = URI.create("file:///root/dir/example/package/one").toPath()
        val sourcePath12 = URI.create("file:///root/dir/example/package/two").toPath()
        val sourceRoots1 = listOf(
          GenericSourceRoot(
            sourcePath = sourcePath11,
            rootType = "python-source",
          ),
          GenericSourceRoot(
            sourcePath = sourcePath12,
            rootType = "python-source",
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

        val sdkInfo1 = PythonSdkInfo(version = "3", originalName = "fake-interpreter-name")

        val pythonModule1 = PythonModule(
          module = module1,
          sourceRoots = sourceRoots1,
          resourceRoots = resourceRoots1,
          libraries = listOf(),
          sdkInfo = sdkInfo1,
        )

        val module2 = GenericModuleInfo(
          name = "module2",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module3",
            ),
          ),
          librariesDependencies = listOf(),
        )

        val sourcePath21 = URI.create("file:///another/root/dir/another/example/package/").toPath()
        val sourceRoots2 = listOf(
          GenericSourceRoot(
            sourcePath = sourcePath21,
            rootType = "python-test",
          ),
        )

        val resourcePath21 = URI.create("file:///another/root/dir/another/example/resource/File1.txt").toPath()
        val resourceRoots2 = listOf(
          ResourceRoot(
            resourcePath = resourcePath21,
            rootType = "",
          ),
        )

        val sdkInfo2 = PythonSdkInfo(version = "3", originalName = "fake-interpreter-name")

        val pythonModule2 = PythonModule(
          module = module2,
          sourceRoots = sourceRoots2,
          resourceRoots = resourceRoots2,
          libraries = listOf(),
          sdkInfo = sdkInfo2,
        )

        val pythonModules = listOf(pythonModule1, pythonModule2)

        // when
        val returnedModuleEntries = runTestWriteAction {
          updater.addEntries(pythonModules)
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
              ModuleDependencyItem.SdkDependency(SdkId("fake-interpreter-name-3", "PythonSDK")),
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "PYTHON_MODULE"
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
              ModuleDependencyItem.SdkDependency(SdkId("fake-interpreter-name-3", "PythonSDK")),
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "PYTHON_MODULE"
          },
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir11,
            rootType = "python-source",
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir12 = sourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualSourceDir12,
            rootType = "python-source",
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualSourceDir21 = sourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedPythonSourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualSourceDir21,
            rootType = "python-test",
          ) {},
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedResourceRootEntity11 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl11,
            rootType = "python-resource",
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedResourceRootEntity12 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity1.moduleEntity.entitySource,
            url = virtualResourceUrl12,
            rootType = "python-resource",
          ) {},
          parentModuleEntity = expectedModuleEntity1.moduleEntity,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedResourceRootEntity21 = ExpectedSourceRootEntity(
          contentRootEntity = ContentRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            excludedPatterns = emptyList(),
          ),
          sourceRootEntity = SourceRootEntity(
            entitySource = expectedModuleEntity2.moduleEntity.entitySource,
            url = virtualResourceUrl21,
            rootType = "python-resource",
          ) {},
          parentModuleEntity = expectedModuleEntity2.moduleEntity,
        )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder listOf(
          expectedPythonSourceRootEntity11,
          expectedPythonSourceRootEntity12,
          expectedPythonSourceRootEntity21,
          expectedResourceRootEntity11,
          expectedResourceRootEntity12,
          expectedResourceRootEntity21,
        )
      }
    }

    @Test
    fun `should not add the sdk dependency when python support is not enabled`() {
      runTestForUpdaters(isPythonSupportEnabled = false, listOf(PythonModuleWithSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module = GenericModuleInfo(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = listOf(
            ModuleDependency(
              moduleName = "module2",
            ),
          ),
          librariesDependencies = listOf(),
        )

        val sourceRoots = listOf(
          GenericSourceRoot(
            sourcePath = URI.create("file:///root/dir/example/package/one").toPath(),
            rootType = "python-source",
          ),
        )

        val sdkInfo = PythonSdkInfo(version = "3", originalName = "fake-interpreter-name")

        val pythonModule = PythonModule(
          module = module,
          sourceRoots = sourceRoots,
          resourceRoots = emptyList(),
          libraries = listOf(),
          sdkInfo = sdkInfo,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(pythonModule)
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
              ModuleDependencyItem.ModuleSourceDependency,
            ),
          ) {
            type = "PYTHON_MODULE"
          },
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity

        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)
      }
    }
  }

  @Nested
  @DisplayName("pythonModuleWithoutSourcesUpdater.addEntity(entityToAdd) tests")
  inner class PythonModuleWithoutSourcesUpdaterTest {
    @Test
    fun `should add one python module without sources to the workspace model`() {
      runTestForUpdaters(isPythonSupportEnabled = true, listOf(PythonModuleWithoutSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module = GenericModuleInfo(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val pythonModule = PythonModule(
          module = module,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
        )

        // when
        val returnedModuleEntity = runTestWriteAction {
          updater.addEntity(pythonModule)
        }

        // then
        val expectedModuleEntity = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module1",
            entitySource = BspEntitySource,
            dependencies = emptyList(),
          ) {
            type = "PYTHON_MODULE"
          },
        )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)
      }
    }

    @Test
    fun `should add multiple python modules without sources to the workspace model`() {
      runTestForUpdaters(isPythonSupportEnabled = true, listOf(PythonModuleWithoutSourcesUpdater::class, PythonModuleUpdater::class)) { updater ->
        // given
        val module1 = GenericModuleInfo(
          name = "module1",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val pythonModule1 = PythonModule(
          module = module1,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
        )

        val module2 = GenericModuleInfo(
          name = "module2",
          type = "PYTHON_MODULE",
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        )

        val pythonModule2 = PythonModule(
          module = module2,
          sourceRoots = emptyList(),
          resourceRoots = emptyList(),
          libraries = emptyList(),
          sdkInfo = null,
        )

        val javaModules = listOf(pythonModule1, pythonModule2)

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
            type = "PYTHON_MODULE"
          },
        )
        val expectedModuleEntity2 = ExpectedModuleEntity(
          moduleEntity = ModuleEntity(
            name = "module2",
            entitySource = BspEntitySource,
            dependencies = emptyList(),
          ) {
            type = "PYTHON_MODULE"
          },
        )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
      }
    }
  }

  private fun runTestForUpdaters(
    isPythonSupportEnabled: Boolean = true,
    updaters: List<KClass<out WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>) -> Unit,
  ) =
    updaters
      .map { it.primaryConstructor!! }
      .forEach { runTest(it, test, isPythonSupportEnabled) }

  private fun runTest(
    updaterConstructor: KFunction<WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity>) -> Unit,
    isPythonSupportEnabled: Boolean,
  ) {
    beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)

    if (updaterConstructor.parameters.size == 1)
      test(updaterConstructor.call(workspaceModelEntityUpdaterConfig))
    else if (updaterConstructor.parameters.size == 2)
      test(updaterConstructor.call(workspaceModelEntityUpdaterConfig, isPythonSupportEnabled))
  }
}

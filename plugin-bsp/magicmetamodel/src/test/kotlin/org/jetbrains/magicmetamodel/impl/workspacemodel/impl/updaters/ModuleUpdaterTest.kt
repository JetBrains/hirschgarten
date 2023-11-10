@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.workspacemodel.storage.BspEntitySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ModuleEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class ModuleUpdaterTest : WorkspaceModelBaseTest() {
  private val defaultDependencies = listOf(
    ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
    ModuleDependencyItem.ModuleSourceDependency,
  )

  private lateinit var moduleEntityUpdater: ModuleEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath)
    moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)
  }

  @Test
  fun `should add one module to the workspace model`() {
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
        ),
        LibraryDependency(
          libraryName = "lib2",
        ),
      ),
    )

    // when
    val returnedModuleEntity = runTestWriteAction {
      moduleEntityUpdater.addEntity(module)
    }

    // then
    val expectedModule = ExpectedModuleEntity(
      moduleEntity = ModuleEntity(
        name = "module1",
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
          ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
          ModuleDependencyItem.ModuleSourceDependency,
        ),
        entitySource = BspEntitySource,
      ) {
        type = "JAVA_MODULE"
      },
    )

    returnedModuleEntity shouldBeEqual expectedModule
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModule)
  }

  @Test
  fun `should add multiple modules to the workspace model`() {
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
        ),
        LibraryDependency(
          libraryName = "lib2",
        ),
      ),
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
        ),
      ),
    )

    val modules = listOf(module1, module2)

    // when
    val returnedModuleEntries = runTestWriteAction {
      moduleEntityUpdater.addEntries(modules)
    }

    // then
    val expectedModule1 = ExpectedModuleEntity(
      moduleEntity = ModuleEntity(
        name = "module1",
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
          ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
          ModuleDependencyItem.ModuleSourceDependency,
        ),
        entitySource = BspEntitySource,
      ) {
        type = "JAVA_MODULE"
      },
    )
    val expectedModule2 = ExpectedModuleEntity(
      moduleEntity = ModuleEntity(
        name = "module2",
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
          ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
          ModuleDependencyItem.ModuleSourceDependency,
        ),
        entitySource = BspEntitySource,
      ) {
        type = "JAVA_MODULE"
      },
    )

    val expectedModuleEntries = listOf(expectedModule1, expectedModule2)

    returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
  }
}

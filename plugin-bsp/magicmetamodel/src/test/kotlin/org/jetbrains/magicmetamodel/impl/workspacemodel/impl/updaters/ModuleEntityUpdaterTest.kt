@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleId
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ModuleEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class ModuleEntityUpdaterTest : WorkspaceModelBaseTest() {

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
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager)
    moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)
  }

  @Test
  fun `should add one module to the workspace model`() {
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
        ),
        entitySource = DoNotSaveInDotIdeaDirEntitySource,
      ) {
        type = "JAVA_MODULE"
      }
    )

    returnedModuleEntity shouldBeEqual expectedModule
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModule)
  }

  @Test
  fun `should add multiple modules to the workspace model`() {
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
        ),
        entitySource = DoNotSaveInDotIdeaDirEntitySource
      ) {
        type = "JAVA_MODULE"
      }
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
            exported = false,
            scope = ModuleDependencyItem.DependencyScope.COMPILE,
          ),
          ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
          ModuleDependencyItem.ModuleSourceDependency,
        ),
        entitySource = DoNotSaveInDotIdeaDirEntitySource,
      ) {
        type = "JAVA_MODULE"
      }
    )

    val expectedModuleEntries = listOf(expectedModule1, expectedModule2)

    returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
  }
}

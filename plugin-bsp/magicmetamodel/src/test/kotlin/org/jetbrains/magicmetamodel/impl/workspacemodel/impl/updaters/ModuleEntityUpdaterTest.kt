@file:Suppress("LongMethod")
package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private data class ExpectedModuleEntityDetails(
  val moduleEntity: ModuleEntity,
)

@DisplayName("ModuleEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class ModuleEntityUpdaterTest : WorkspaceModelEntityWithoutParentModuleUpdaterBaseTest() {

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
      )
    )

    val defaultDependencies = listOf(
      ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
      ModuleDependencyItem.ModuleSourceDependency,
    )

    // when
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)

    lateinit var returnedModuleEntity: ModuleEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedModuleEntity = moduleEntityUpdater.addEntity(module)
    }

    // then
    val expectedModuleDetails = ExpectedModuleEntityDetails(
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

    validateModuleEntity(returnedModuleEntity, expectedModuleDetails)

    workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedModuleDetails), this::validateModuleEntity
    )
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
      )
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
      )
    )

    val defaultDependencies = listOf(
      ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
      ModuleDependencyItem.ModuleSourceDependency,
    )

    val modules = listOf(module1, module2)

    // when
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)

    lateinit var returnedModuleEntries: List<ModuleEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedModuleEntries = moduleEntityUpdater.addEntries(modules)
    }

    // then
    val expectedModule1Details = ExpectedModuleEntityDetails(
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
    val expectedModule2Details = ExpectedModuleEntityDetails(
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

    returnedModuleEntries shouldContainExactlyInAnyOrder Pair(
      listOf(expectedModule1Details, expectedModule2Details), this::validateModuleEntity
    )
    workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedModule1Details, expectedModule2Details), this::validateModuleEntity
    )
  }

  private fun validateModuleEntity(actual: ModuleEntity, expected: ExpectedModuleEntityDetails) {
    actual.name shouldBe expected.moduleEntity.name
    actual.type shouldBe expected.moduleEntity.type
    actual.dependencies shouldContainExactlyInAnyOrder expected.moduleEntity.dependencies
  }
}

@file:Suppress("LongMethod")

package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.workspacemodel.entities.BspEntitySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ModuleEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class ModuleUpdaterTest : WorkspaceModelBaseTest() {
  private val defaultDependencies = listOf(
    SdkDependency(SdkId("11", "JavaSDK")),
    ModuleSourceDependency,
  )

  private lateinit var moduleEntityUpdater: ModuleEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
    moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)
  }

  @Test
  fun `should add one module to the workspace model`() {
    // given
    val module = GenericModuleInfo(
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

    // when
    val returnedModuleEntity = runTestWriteAction {
      moduleEntityUpdater.addEntity(module)
    }

    // then
    val expectedModule = ExpectedModuleEntity(
      moduleEntity = ModuleEntity(
        name = "module1",
        dependencies = listOf(
          ModuleDependency(
            module = ModuleId("module2"),
            exported = true,
            scope = DependencyScope.COMPILE,
            productionOnTest = true,
          ),
          ModuleDependency(
            module = ModuleId("module3"),
            exported = true,
            scope = DependencyScope.COMPILE,
            productionOnTest = true,
          ),
          LibraryDependency(
            library = LibraryId(
              name = "lib1",
              tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
            ),
            exported = true,
            scope = DependencyScope.COMPILE,
          ),
          LibraryDependency(
            library = LibraryId(
              name = "lib2",
              tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
            ),
            exported = true,
            scope = DependencyScope.COMPILE,
          ),
          SdkDependency(SdkId("11", "JavaSDK")),
          ModuleSourceDependency,
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
          ModuleDependency(
            module = ModuleId("module2"),
            exported = true,
            scope = DependencyScope.COMPILE,
            productionOnTest = true,
          ),
          ModuleDependency(
            module = ModuleId("module3"),
            exported = true,
            scope = DependencyScope.COMPILE,
            productionOnTest = true,
          ),
          LibraryDependency(
            library = LibraryId(
              name = "lib1",
              tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
            ),
            exported = true,
            scope = DependencyScope.COMPILE,
          ),
          LibraryDependency(
            library = LibraryId(
              name = "lib2",
              tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module1")),
            ),
            exported = true,
            scope = DependencyScope.COMPILE,
          ),
          SdkDependency(SdkId("11", "JavaSDK")),
          ModuleSourceDependency,
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
          ModuleDependency(
            module = ModuleId("module3"),
            exported = true,
            scope = DependencyScope.COMPILE,
            productionOnTest = true,
          ),
          LibraryDependency(
            library = LibraryId(
              name = "lib1",
              tableId = LibraryTableId.ModuleLibraryTableId(ModuleId("module2")),
            ),
            exported = true,
            scope = DependencyScope.COMPILE,
          ),
          SdkDependency(SdkId("11", "JavaSDK")),
          ModuleSourceDependency,
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

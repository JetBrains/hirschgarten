@file:Suppress("LongMethod")

package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

val testLibraries: List<Library> =
  listOf(
    Library(
      displayName = "lib1",
      iJars = emptyList(),
      sourceJars = emptyList(),
      classJars = emptyList(),
      mavenCoordinates = null,
      isLowPriority = false,
    ),
    Library(
      displayName = "lib2",
      iJars = emptyList(),
      sourceJars = emptyList(),
      classJars = emptyList(),
      mavenCoordinates = null,
      isLowPriority = false,
    ),
  )
val testLibrariesByName: Map<String, Library> =
  testLibraries.associateBy { it.displayName }

@DisplayName("ModuleEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class ModuleUpdaterTest : WorkspaceModelBaseTest() {
  private val defaultDependencies =
    listOf(
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

    moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies, testLibrariesByName)
  }

  @Test
  fun `should add one module to the workspace model`() {
    // given
    val module =
      GenericModuleInfo(
        name = "module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2",
            "lib1",
            "module3",
            "lib2",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    // when
    val returnedModuleEntity =
      runTestWriteAction {
        moduleEntityUpdater.addEntity(module)
      }

    // then
    val expectedModule =
      ExpectedModuleEntity(
        moduleEntity =
          ModuleEntity(
            name = "module1",
            dependencies =
              listOf(
                ModuleDependency(
                  module = ModuleId("module2"),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
                ),
                LibraryDependency(
                  library =
                    LibraryId(
                      name = "lib1",
                      tableId = LibraryTableId.ProjectLibraryTableId,
                    ),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                ModuleDependency(
                  module = ModuleId("module3"),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
                ),
                LibraryDependency(
                  library =
                    LibraryId(
                      name = "lib2",
                      tableId = LibraryTableId.ProjectLibraryTableId,
                    ),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                SdkDependency(SdkId("11", "JavaSDK")),
                ModuleSourceDependency,
              ),
            entitySource = BazelProjectEntitySource,
          ) {
            type = ModuleTypeId("JAVA_MODULE")
          },
      )

    returnedModuleEntity shouldBeEqual expectedModule
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModule)
  }

  @Test
  fun `should add multiple modules to the workspace model`() {
    // given
    val module1 =
      GenericModuleInfo(
        name = "module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module2",
            "module3",
            "lib1",
            "lib2",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val module2 =
      GenericModuleInfo(
        name = "module2",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            "module3",
            "lib1",
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
      )

    val modules = listOf(module1, module2)

    // when
    val returnedModuleEntries =
      runTestWriteAction {
        moduleEntityUpdater.addEntities(modules)
      }

    // then
    val expectedModule1 =
      ExpectedModuleEntity(
        moduleEntity =
          ModuleEntity(
            name = "module1",
            dependencies =
              listOf(
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
                  library =
                    LibraryId(
                      name = "lib1",
                      tableId = LibraryTableId.ProjectLibraryTableId,
                    ),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                LibraryDependency(
                  library =
                    LibraryId(
                      name = "lib2",
                      tableId = LibraryTableId.ProjectLibraryTableId,
                    ),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                SdkDependency(SdkId("11", "JavaSDK")),
                ModuleSourceDependency,
              ),
            entitySource = BazelProjectEntitySource,
          ) {
            type = ModuleTypeId("JAVA_MODULE")
          },
      )
    val expectedModule2 =
      ExpectedModuleEntity(
        moduleEntity =
          ModuleEntity(
            name = "module2",
            dependencies =
              listOf(
                ModuleDependency(
                  module = ModuleId("module3"),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
                ),
                LibraryDependency(
                  library =
                    LibraryId(
                      name = "lib1",
                      tableId = LibraryTableId.ProjectLibraryTableId,
                    ),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                SdkDependency(SdkId("11", "JavaSDK")),
                ModuleSourceDependency,
              ),
            entitySource = BazelProjectEntitySource,
          ) {
            type = ModuleTypeId("JAVA_MODULE")
          },
      )

    val expectedModuleEntries = listOf(expectedModule1, expectedModule2)

    returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
    loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
  }
}

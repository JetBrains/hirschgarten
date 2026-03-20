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
import org.jetbrains.bazel.magicmetamodel.LIBRARY_MODULE_PREFIX
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.createJavaModule
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.Dependency
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
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

  private val testModules: Map<String, Module> =
    mapOf(
      "module1" to createJavaModule("module1"),
      "module2" to createJavaModule("module2"),
      "module3" to createJavaModule("module3"),
    )

  private lateinit var moduleEntityUpdater: ModuleEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)

    moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies, testModules, testLibrariesByName)
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
            Dependency("module2"),
            Dependency("lib1"),
            Dependency("module3"),
            Dependency("lib2"),
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
                ModuleDependency(
                  module = ModuleId(name = LibraryGraph.addLibraryModulePrefix("lib1")),
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
                ModuleDependency(
                  module = ModuleId(name = LibraryGraph.addLibraryModulePrefix("lib2")),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
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
            Dependency("module2"),
            Dependency("module3"),
            Dependency("lib1"),
            Dependency("lib2"),
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
            Dependency("module3"),
            Dependency("lib1"),
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
                ModuleDependency(
                  module = ModuleId(LibraryGraph.addLibraryModulePrefix("lib1")),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
                ),
                ModuleDependency(
                  module = ModuleId(LibraryGraph.addLibraryModulePrefix("lib2")),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
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
                ModuleDependency(
                  module = ModuleId(LibraryGraph.addLibraryModulePrefix("lib1")),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
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

  @Test
  fun `should filter out dependencies not present in modules or libraries`() {
    // given
    val module =
      GenericModuleInfo(
        name = "module1",
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Dependency("module2"),
            Dependency("unknown_module"),
            Dependency("lib1"),
            Dependency("unknown_lib"),
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
                ModuleDependency(
                  module = ModuleId(LibraryGraph.addLibraryModulePrefix("lib1")),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
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
  fun `should resolve library module dependencies via modules map`() {
    // given
    val libModuleName1 = "${LIBRARY_MODULE_PREFIX}lib1"
    val libModuleName2 = "${LIBRARY_MODULE_PREFIX}lib2"
    val modulesWithLibraryModules: Map<String, Module> = testModules + mapOf(
      libModuleName1 to createJavaModule(libModuleName1),
      libModuleName2 to createJavaModule(libModuleName2),
    )

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project)
    val updater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies, modulesWithLibraryModules, testLibrariesByName)

    val libraryModule =
      GenericModuleInfo(
        name = libModuleName1,
        type = ModuleTypeId("JAVA_MODULE"),
        dependencies =
          listOf(
            Dependency("lib1", isRuntimeOnly = false, exported = true),  // self-dep: library name without prefix
            Dependency(libModuleName2, isRuntimeOnly = false, exported = true),  // inter-library dep: prefixed name
          ),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        isLibraryModule = true,
      )

    // when
    val returnedModuleEntity =
      runTestWriteAction {
        updater.addEntity(libraryModule)
      }

    // then
    val expectedModule =
      ExpectedModuleEntity(
        moduleEntity =
          ModuleEntity(
            name = libModuleName1,
            dependencies =
              listOf(
                // self-dep becomes a LibraryDependency (handled by library branch since isLibraryModule=true)
                LibraryDependency(
                  library = LibraryId("lib1", LibraryTableId.ProjectLibraryTableId),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                ),
                // inter-library dep becomes a ModuleDependency (resolved via modules map)
                ModuleDependency(
                  module = ModuleId(libModuleName2),
                  exported = true,
                  scope = DependencyScope.COMPILE,
                  productionOnTest = true,
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
}

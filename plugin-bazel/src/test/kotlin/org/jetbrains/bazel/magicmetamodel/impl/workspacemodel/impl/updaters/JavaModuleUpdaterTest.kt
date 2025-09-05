@file:Suppress("LongMethod")

package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
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
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ResourceRoot
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

// TODO add libraries tests
internal class JavaModuleUpdaterTest : WorkspaceModelBaseTest() {
  @Nested
  @DisplayName("javaModuleWithSourcesUpdater.addEntity(entityToAdd) tests")
  inner class JavaModuleWithSourcesUpdaterTest {
    @Test
    fun `should add one java module with sources to the workspace model`() {
      runTestForUpdaters(
        listOf(
          { JavaModuleWithSourcesUpdater(it, it.projectBasePath, emptyList(), testLibrariesByName) },
          { JavaModuleUpdater(it, it.projectBasePath, emptyList(), testLibraries) },
        ),
      ) { updater ->
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

        val baseDirContentRoot =
          ContentRoot(
            path = Path("/root/dir/example/"),
          )

        val sourcePath1 = Path("/root/dir/example/package/one")
        val sourcePackagePrefix1 = "example.package.one"
        val sourcePath2 = Path("/root/dir/example/package/two")
        val sourcePackagePrefix2 = "example.package.two"
        val sourceRoots =
          listOf(
            JavaSourceRoot(
              sourcePath = sourcePath1,
              generated = false,
              packagePrefix = sourcePackagePrefix1,
              rootType = SourceRootTypeId("java-source"),
            ),
            JavaSourceRoot(
              sourcePath = sourcePath2,
              generated = false,
              packagePrefix = sourcePackagePrefix2,
              rootType = SourceRootTypeId("java-source"),
            ),
          )

        val resourcePath1 = Path("/root/dir/example/resource/File1.txt")
        val resourcePath2 = Path("/root/dir/example/resource/File2.txt")
        val resourceRoots =
          listOf(
            ResourceRoot(
              resourcePath = resourcePath1,
              rootType = SourceRootTypeId("java-resource"),
            ),
            ResourceRoot(
              resourcePath = resourcePath2,
              rootType = SourceRootTypeId("java-resource"),
            ),
          )

        val javaModule =
          JavaModule(
            genericModuleInfo = module,
            baseDirContentRoot = baseDirContentRoot,
            sourceRoots = sourceRoots,
            resourceRoots = resourceRoots,
            jvmJdkName = "test-proj-11",
          )

        // when
        val returnedModuleEntity =
          runTestWriteAction {
            updater.addEntity(javaModule)!!
          }

        // then
        val expectedModuleEntity =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module1",
                entitySource = BazelProjectEntitySource,
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
                    SdkDependency(SdkId("test-proj-11", "JavaSDK")),
                    ModuleSourceDependency,
                  ),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
                javaSettings =
                  JavaModuleSettingsEntity(
                    inheritedCompilerOutput = false,
                    excludeOutput = true,
                    entitySource = BazelProjectEntitySource,
                  ) {
                    this.compilerOutput = Path("compiler/output").toVirtualFileUrl(virtualFileUrlManager)
                  }
              },
          )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)

        val virtualSourceDir1 = sourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity1 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualSourceDir1,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualSourceDir1,
                rootTypeId = SourceRootTypeId("java-source"),
              ) {
                javaSourceRoots =
                  listOf(
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
        val expectedJavaSourceRootEntity2 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualSourceDir2,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualSourceDir2,
                rootTypeId = SourceRootTypeId("java-source"),
              ) {
                javaSourceRoots =
                  listOf(
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
        val expectedJavaResourceRootEntity1 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualResourceUrl1,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualResourceUrl1,
                rootTypeId = SourceRootTypeId("java-resource"),
              ) {
                javaResourceRoots =
                  listOf(
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
        val expectedJavaResourceRootEntity2 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualResourceUrl2,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity.moduleEntity.entitySource,
                url = virtualResourceUrl2,
                rootTypeId = SourceRootTypeId("java-resource"),
              ) {
                javaResourceRoots =
                  listOf(
                    JavaResourceRootPropertiesEntity(
                      entitySource = expectedModuleEntity.moduleEntity.entitySource,
                      generated = false,
                      relativeOutputPath = "",
                    ),
                  )
              },
            parentModuleEntity = expectedModuleEntity.moduleEntity,
          )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder
          listOf(
            expectedJavaSourceRootEntity1,
            expectedJavaSourceRootEntity2,
            expectedJavaResourceRootEntity1,
            expectedJavaResourceRootEntity2,
          )
      }
    }

    @Test
    fun `should add multiple java module with sources to the workspace model`() {
      runTestForUpdaters(
        listOf(
          { JavaModuleWithSourcesUpdater(it, it.projectBasePath, emptyList(), testLibrariesByName) },
          { JavaModuleUpdater(it, it.projectBasePath, emptyList(), testLibraries) },
        ),
      ) { updater ->
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

        val baseDirContentRoot1 =
          ContentRoot(
            path = Path("/root/dir/example/"),
          )

        val sourcePath11 = Path("/root/dir/example/package/one")
        val sourcePackagePrefix11 = "example.package.one"
        val sourcePath12 = Path("/root/dir/example/package/two")
        val sourcePackagePrefix12 = "example.package.two"
        val sourceRoots1 =
          listOf(
            JavaSourceRoot(
              sourcePath = sourcePath11,
              generated = false,
              packagePrefix = sourcePackagePrefix11,
              rootType = SourceRootTypeId("java-source"),
            ),
            JavaSourceRoot(
              sourcePath = sourcePath12,
              generated = false,
              packagePrefix = sourcePackagePrefix12,
              rootType = SourceRootTypeId("java-source"),
            ),
          )

        val resourcePath11 = Path("/root/dir/example/resource/File1.txt")
        val resourcePath12 = Path("/root/dir/example/resource/File2.txt")
        val resourceRoots1 =
          listOf(
            ResourceRoot(
              resourcePath = resourcePath11,
              rootType = SourceRootTypeId("java-resource"),
            ),
            ResourceRoot(
              resourcePath = resourcePath12,
              rootType = SourceRootTypeId("java-resource"),
            ),
          )

        val javaModule1 =
          JavaModule(
            genericModuleInfo = module1,
            sourceRoots = sourceRoots1,
            resourceRoots = resourceRoots1,
            baseDirContentRoot = baseDirContentRoot1,
            jvmJdkName = "test-proj-11",
            kotlinAddendum = null,
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

        val baseDirContentRoot2 =
          ContentRoot(
            path = Path("/another/root/dir/example/"),
          )

        val sourcePath21 = Path("/another/root/dir/another/example/package/")
        val sourcePackagePrefix21 = "another.example.package"
        val sourceRoots2 =
          listOf(
            JavaSourceRoot(
              sourcePath = sourcePath21,
              generated = false,
              packagePrefix = sourcePackagePrefix21,
              rootType = SourceRootTypeId("java-test"),
            ),
          )

        val resourcePath21 = Path("/another/root/dir/another/example/resource/File1.txt")
        val resourceRoots2 =
          listOf(
            ResourceRoot(
              resourcePath = resourcePath21,
              rootType = SourceRootTypeId("java-test-resource"),
            ),
          )

        val javaModule2 =
          JavaModule(
            genericModuleInfo = module2,
            baseDirContentRoot = baseDirContentRoot2,
            sourceRoots = sourceRoots2,
            resourceRoots = resourceRoots2,
            jvmJdkName = "test-proj-11",
            kotlinAddendum = null,
          )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries =
          runTestWriteAction {
            updater.addEntities(javaModules)
          }

        // then
        val expectedModuleEntity1 =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module1",
                entitySource = BazelProjectEntitySource,
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
                    SdkDependency(SdkId("test-proj-11", "JavaSDK")),
                    ModuleSourceDependency,
                  ),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
                javaSettings =
                  JavaModuleSettingsEntity(
                    inheritedCompilerOutput = false,
                    excludeOutput = true,
                    entitySource = BazelProjectEntitySource,
                  ) {
                    this.compilerOutput = Path("compiler/output1").toVirtualFileUrl(virtualFileUrlManager)
                  }
              },
          )

        val expectedModuleEntity2 =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module2",
                entitySource = BazelProjectEntitySource,
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
                    SdkDependency(SdkId("test-proj-11", "JavaSDK")),
                    ModuleSourceDependency,
                  ),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
                javaSettings =
                  JavaModuleSettingsEntity(
                    inheritedCompilerOutput = false,
                    excludeOutput = true,
                    entitySource = BazelProjectEntitySource,
                  ) {
                    this.compilerOutput = Path("compiler/output2").toVirtualFileUrl(virtualFileUrlManager)
                  }
              },
          )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries

        val virtualSourceDir11 = sourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntity11 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualSourceDir11,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualSourceDir11,
                rootTypeId = SourceRootTypeId("java-source"),
              ) {
                javaSourceRoots =
                  listOf(
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
        val expectedJavaSourceRootEntity12 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualSourceDir12,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualSourceDir12,
                rootTypeId = SourceRootTypeId("java-source"),
              ) {
                javaSourceRoots =
                  listOf(
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
        val expectedJavaSourceRootEntity21 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                url = virtualSourceDir21,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                url = virtualSourceDir21,
                rootTypeId = SourceRootTypeId("java-test"),
              ) {
                javaSourceRoots =
                  listOf(
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
        val expectedJavaResourceRootEntity11 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualResourceUrl11,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualResourceUrl11,
                rootTypeId = SourceRootTypeId("java-resource"),
              ) {
                javaResourceRoots =
                  listOf(
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
        val expectedJavaResourceRootEntity12 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualResourceUrl12,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity1.moduleEntity.entitySource,
                url = virtualResourceUrl12,
                rootTypeId = SourceRootTypeId("java-resource"),
              ) {
                javaResourceRoots =
                  listOf(
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
        val expectedJavaResourceRootEntity21 =
          ExpectedSourceRootEntity(
            contentRootEntity =
              ContentRootEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                url = virtualResourceUrl21,
                excludedPatterns = emptyList(),
              ),
            sourceRootEntity =
              SourceRootEntity(
                entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                url = virtualResourceUrl21,
                rootTypeId = SourceRootTypeId("java-test-resource"),
              ) {
                javaResourceRoots =
                  listOf(
                    JavaResourceRootPropertiesEntity(
                      entitySource = expectedModuleEntity2.moduleEntity.entitySource,
                      generated = false,
                      relativeOutputPath = "",
                    ),
                  )
              },
            parentModuleEntity = expectedModuleEntity2.moduleEntity,
          )

        loadedEntries(SourceRootEntity::class.java) shouldContainExactlyInAnyOrder
          listOf(
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
      runTestForUpdaters(
        listOf(
          { JavaModuleWithoutSourcesUpdater(it) },
          { JavaModuleUpdater(it, it.projectBasePath) },
        ),
      ) { updater ->
        // given
        val module =
          GenericModuleInfo(
            name = "module1",
            type = ModuleTypeId("JAVA_MODULE"),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
          )

        val baseDirContentRootPath = Path("/root/dir/")
        val baseDirContentRoot =
          ContentRoot(
            path = baseDirContentRootPath,
          )

        val javaModule =
          JavaModule(
            genericModuleInfo = module,
            baseDirContentRoot = baseDirContentRoot,
            sourceRoots = emptyList(),
            resourceRoots = emptyList(),
            jvmJdkName = null,
            kotlinAddendum = null,
          )

        // when
        val returnedModuleEntity =
          runTestWriteAction {
            updater.addEntity(javaModule)!!
          }

        // then
        val expectedModuleEntity =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module1",
                entitySource = BazelProjectEntitySource,
                dependencies = emptyList(),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
              },
          )

        returnedModuleEntity shouldBeEqual expectedModuleEntity
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)
      }
    }

    @Test
    fun `should add multiple java module without sources to the workspace model`() {
      runTestForUpdaters(
        listOf(
          { JavaModuleWithoutSourcesUpdater(it) },
          { JavaModuleUpdater(it, it.projectBasePath) },
        ),
      ) { updater ->
        // given
        val module1 =
          GenericModuleInfo(
            name = "module1",
            type = ModuleTypeId("JAVA_MODULE"),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
          )

        val baseDirContentRootPath1 = Path("/root/dir1/")
        val baseDirContentRoot1 =
          ContentRoot(
            path = baseDirContentRootPath1,
          )

        val javaModule1 =
          JavaModule(
            genericModuleInfo = module1,
            baseDirContentRoot = baseDirContentRoot1,
            sourceRoots = emptyList(),
            resourceRoots = emptyList(),
            jvmJdkName = null,
            kotlinAddendum = null,
          )

        val module2 =
          GenericModuleInfo(
            name = "module2",
            type = ModuleTypeId("JAVA_MODULE"),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
          )

        val baseDirContentRootPath2 = Path("/root/dir2/")
        val baseDirContentRoot2 =
          ContentRoot(
            path = baseDirContentRootPath2,
          )

        val javaModule2 =
          JavaModule(
            genericModuleInfo = module2,
            baseDirContentRoot = baseDirContentRoot2,
            sourceRoots = emptyList(),
            resourceRoots = emptyList(),
            jvmJdkName = null,
            kotlinAddendum = null,
          )

        val javaModules = listOf(javaModule1, javaModule2)

        // when
        val returnedModuleEntries =
          runTestWriteAction {
            updater.addEntities(javaModules)
          }

        // then
        val expectedModuleEntity1 =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module1",
                entitySource = BazelProjectEntitySource,
                dependencies = emptyList(),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
              },
          )
        val expectedModuleEntity2 =
          ExpectedModuleEntity(
            moduleEntity =
              ModuleEntity(
                name = "module2",
                entitySource = BazelProjectEntitySource,
                dependencies = emptyList(),
              ) {
                type = ModuleTypeId("JAVA_MODULE")
              },
          )

        val expectedModuleEntries = listOf(expectedModuleEntity1, expectedModuleEntity2)

        returnedModuleEntries shouldContainExactlyInAnyOrder expectedModuleEntries
        loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder expectedModuleEntries
      }
    }
  }

  private fun runTestForUpdaters(
    updaters: List<(WorkspaceModelEntityUpdaterConfig) -> WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) = updaters
    .forEach {
      beforeEach()
      val updater = it(WorkspaceModelEntityUpdaterConfig(workspaceEntityStorageBuilder, virtualFileUrlManager, projectBasePath, project))
      test(updater)
    }
}

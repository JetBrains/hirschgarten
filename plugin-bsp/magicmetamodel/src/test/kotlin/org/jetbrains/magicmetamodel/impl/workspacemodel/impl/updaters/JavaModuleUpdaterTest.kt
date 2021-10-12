@file:Suppress("LongMethod")
package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal class JavaModuleUpdaterTest : WorkspaceModelEntityWithoutParentModuleUpdaterBaseTest() {

  private data class ExpectedJavaSourceRootEntityDetails(
    val contentRootEntity: ContentRootEntity,
    val sourceRootEntity: SourceRootEntity,
    val javaSourceRootEntity: JavaSourceRootEntity,
    val module: ModuleEntity,
  )

  private data class ExpectedJavaResourceRootEntityDetails(
    val contentRootEntity: ContentRootEntity,
    val sourceRootEntity: SourceRootEntity,
    val javaResourceRootEntity: JavaResourceRootEntity,
    val module: ModuleEntity,
  )

  private data class ExpectedContentRootEntityDetails(
    val contentRootEntity: ContentRootEntity,
    val module: ModuleEntity,
  )

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
            jar = "jar:file:///lib1/1.0.0/lib1-sources.jar!/"
          ),
          Library(
            displayName = "lib2",
            jar = "jar:file:///lib2/1.0.0/lib2-sources.jar!/"
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
        lateinit var returnedModuleEntity: ModuleEntity

        WriteCommandAction.runWriteCommandAction(project) {
          returnedModuleEntity = updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ModuleEntity(
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

        validateModuleEntity(returnedModuleEntity, expectedModuleEntity)

        workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntityDetails1 = ExpectedJavaSourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualSourceDir1, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir1, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix1),
          module = expectedModuleEntity,
        )
        val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntityDetails2 = ExpectedJavaSourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualSourceDir2, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir2, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix2),
          module = expectedModuleEntity,
        )

        workspaceModelLoadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedJavaSourceRootEntityDetails1, expectedJavaSourceRootEntityDetails2),
          this@JavaModuleUpdaterTest::validateJavaSourceRootEntity
        )

        val virtualResourceUrl1 = resourcePath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntityDetails1 = ExpectedJavaResourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualResourceUrl1, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl1, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          module = expectedModuleEntity,
        )
        val virtualResourceUrl2 = resourcePath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntityDetails2 = ExpectedJavaResourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualResourceUrl2, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl2, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          module = expectedModuleEntity,
        )

        workspaceModelLoadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedJavaResourceRootEntityDetails1, expectedJavaResourceRootEntityDetails2),
          this@JavaModuleUpdaterTest::validateJavaResourceRootEntity
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
            jar = "jar:file:///lib1/1.0.0/lib1-sources.jar!/"
          ),
          Library(
            displayName = "lib2",
            jar = "jar:file:///lib2/1.0.0/lib2-sources.jar!/"
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
            jar = "jar:file:///lib1/1.0.0/lib1-sources.jar!/"
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
        lateinit var returnedModuleEntries: List<ModuleEntity>

        WriteCommandAction.runWriteCommandAction(project) {
          returnedModuleEntries = updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ModuleEntity(
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

        val expectedModuleEntity2 = ModuleEntity(
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

        returnedModuleEntries shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity1, expectedModuleEntity2), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity1, expectedModuleEntity2), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        val virtualSourceDir11 = sourceDir11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntityDetails11 = ExpectedJavaSourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualSourceDir11, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir11, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix11),
          module = expectedModuleEntity1,
        )
        val virtualSourceDir12 = sourceDir12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntityDetails12 = ExpectedJavaSourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualSourceDir12, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir12, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix12),
          module = expectedModuleEntity1,
        )
        val virtualSourceDir21 = sourceDir21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaSourceRootEntityDetails21 = ExpectedJavaSourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualSourceDir21, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualSourceDir21, "java-source"),
          javaSourceRootEntity = JavaSourceRootEntity(false, sourcePackagePrefix21),
          module = expectedModuleEntity2,
        )

        workspaceModelLoadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(
            expectedJavaSourceRootEntityDetails11,
            expectedJavaSourceRootEntityDetails12,
            expectedJavaSourceRootEntityDetails21
          ),
          this@JavaModuleUpdaterTest::validateJavaSourceRootEntity
        )

        val virtualResourceUrl11 = resourcePath11.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntityDetails11 = ExpectedJavaResourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualResourceUrl11, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl11, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          module = expectedModuleEntity1,
        )
        val virtualResourceUrl12 = resourcePath12.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntityDetails12 = ExpectedJavaResourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualResourceUrl12, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl12, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          module = expectedModuleEntity1,
        )
        val virtualResourceUrl21 = resourcePath21.toVirtualFileUrl(virtualFileUrlManager)
        val expectedJavaResourceRootEntityDetails21 = ExpectedJavaResourceRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualResourceUrl21, emptyList(), emptyList()),
          sourceRootEntity = SourceRootEntity(virtualResourceUrl21, "java-resource"),
          javaResourceRootEntity = JavaResourceRootEntity(false, ""),
          module = expectedModuleEntity2,
        )

        workspaceModelLoadedEntries(JavaResourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(
            expectedJavaResourceRootEntityDetails11,
            expectedJavaResourceRootEntityDetails12,
            expectedJavaResourceRootEntityDetails21
          ),
          this@JavaModuleUpdaterTest::validateJavaResourceRootEntity
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
        lateinit var returnedModuleEntity: ModuleEntity

        WriteCommandAction.runWriteCommandAction(project) {
          returnedModuleEntity = updater.addEntity(javaModule)
        }

        // then
        val expectedModuleEntity = ModuleEntity(
          name = "module1",
          type = "JAVA_MODULE",
          dependencies = emptyList(),
        )

        validateModuleEntity(returnedModuleEntity, expectedModuleEntity)

        workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        val virtualBaseDirContentRootPath = baseDirContentRootPath.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntityDetails = ExpectedContentRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath, emptyList(), emptyList()),
          module = expectedModuleEntity,
        )

        workspaceModelLoadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedContentRootEntityDetails), this@JavaModuleUpdaterTest::validateContentRootEntity
        )
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
        lateinit var returnedModuleEntries: List<ModuleEntity>

        WriteCommandAction.runWriteCommandAction(project) {
          returnedModuleEntries = updater.addEntries(javaModules)
        }

        // then
        val expectedModuleEntity1 = ModuleEntity(
          name = "module1",
          type = "JAVA_MODULE",
          dependencies = emptyList(),
        )
        val expectedModuleEntity2 = ModuleEntity(
          name = "module2",
          type = "JAVA_MODULE",
          dependencies = emptyList(),
        )

        returnedModuleEntries shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity1, expectedModuleEntity2), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        workspaceModelLoadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedModuleEntity1, expectedModuleEntity2), this@JavaModuleUpdaterTest::validateModuleEntity
        )

        val virtualBaseDirContentRootPath1 = baseDirContentRootPath1.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntityDetails1 = ExpectedContentRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath1, emptyList(), emptyList()),
          module = expectedModuleEntity1,
        )

        val virtualBaseDirContentRootPath2 = baseDirContentRootPath2.toVirtualFileUrl(virtualFileUrlManager)
        val expectedContentRootEntityDetails2 = ExpectedContentRootEntityDetails(
          contentRootEntity = ContentRootEntity(virtualBaseDirContentRootPath2, emptyList(), emptyList()),
          module = expectedModuleEntity2,
        )

        workspaceModelLoadedEntries(ContentRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
          listOf(expectedContentRootEntityDetails1, expectedContentRootEntityDetails2),
          this@JavaModuleUpdaterTest::validateContentRootEntity
        )
      }
    }
  }

  private fun runTestForUpdaters(
    updaters: List<KClass<out WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>>>,
    test: (WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity>) -> Unit,
  ) = updaters
    .map { it.primaryConstructor!! }
    .forEach {
      beforeEach()
      test(it.call(workspaceModelEntityUpdaterConfig))
    }

  private fun validateJavaSourceRootEntity(
    actual: JavaSourceRootEntity,
    expected: ExpectedJavaSourceRootEntityDetails
  ) {
    actual.generated shouldBe expected.javaSourceRootEntity.generated
    actual.packagePrefix shouldBe expected.javaSourceRootEntity.packagePrefix

    val actualSourceRoot = actual.sourceRoot
    actualSourceRoot.url shouldBe expected.sourceRootEntity.url
    actualSourceRoot.rootType shouldBe expected.sourceRootEntity.rootType

    val actualContentRoot = actualSourceRoot.contentRoot
    actualContentRoot.url shouldBe expected.contentRootEntity.url
    actualContentRoot.excludedUrls shouldBe expected.contentRootEntity.excludedUrls
    actualContentRoot.excludedPatterns shouldBe expected.contentRootEntity.excludedPatterns

    val actualModuleEntity = actualContentRoot.module
    validateModuleEntity(actualModuleEntity, expected.module)
  }

  private fun validateJavaResourceRootEntity(
    actual: JavaResourceRootEntity,
    expected: ExpectedJavaResourceRootEntityDetails
  ) {
    actual.generated shouldBe expected.javaResourceRootEntity.generated
    actual.relativeOutputPath shouldBe expected.javaResourceRootEntity.relativeOutputPath

    val actualSourceRoot = actual.sourceRoot
    actualSourceRoot.url shouldBe expected.sourceRootEntity.url
    actualSourceRoot.rootType shouldBe expected.sourceRootEntity.rootType

    val actualContentRoot = actualSourceRoot.contentRoot
    actualContentRoot.url shouldBe expected.contentRootEntity.url
    actualContentRoot.excludedUrls shouldBe expected.contentRootEntity.excludedUrls
    actualContentRoot.excludedPatterns shouldBe expected.contentRootEntity.excludedPatterns

    val actualModuleEntity = actualContentRoot.module
    validateModuleEntity(actualModuleEntity, expected.module)
  }

  private fun validateContentRootEntity(
    actual: ContentRootEntity,
    expected: ExpectedContentRootEntityDetails,
  ) {
    actual.url shouldBe expected.contentRootEntity.url
    actual.excludedUrls shouldContainExactlyInAnyOrder expected.contentRootEntity.excludedUrls
    actual.excludedPatterns shouldContainExactlyInAnyOrder expected.contentRootEntity.excludedPatterns

    validateModuleEntity(actual.module, expected.module)
  }

  private fun validateModuleEntity(actual: ModuleEntity, expected: ModuleEntity) {
    actual.name shouldBe expected.name
    actual.type shouldBe expected.type
    actual.dependencies shouldContainExactlyInAnyOrder expected.dependencies
  }
}

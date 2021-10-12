package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

private data class ExpectedJavaSourceRootEntityDetails(
  val contentRootEntity: ContentRootEntity,
  val sourceRootEntity: SourceRootEntity,
  val javaSourceRootEntity: JavaSourceRootEntity,
)

@DisplayName("javaSourceEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class JavaSourceEntityUpdaterTest : WorkspaceModelEntityWithParentModuleUpdaterBaseTest() {

  @Test
  fun `should add one java source root to the workspace model`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val generated = false
    val packagePrefix = "example.package"

    val javaSourceRoot = JavaSourceRoot(sourceDir, generated, packagePrefix)

    // when
    val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedJavaSourceRootEntity: JavaSourceRootEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedJavaSourceRootEntity = javaSourceEntityUpdater.addEntity(javaSourceRoot, parentModuleEntity)
    }

    // then
    val virtualSourceDir = sourceDir.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntityDetails = ExpectedJavaSourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualSourceDir, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualSourceDir, "java-source"),
      javaSourceRootEntity = JavaSourceRootEntity(generated, packagePrefix),
    )

    validateJavaSourceRootEntity(returnedJavaSourceRootEntity, expectedJavaSourceRootEntityDetails)

    workspaceModelLoadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedJavaSourceRootEntityDetails), this::validateJavaSourceRootEntity
    )
  }

  @Test
  fun `should add multiple java source roots to the workspace model`() {
    // given
    val sourceDir1 = URI.create("file:///root/dir/example/package/").toPath()
    val generated1 = false
    val packagePrefix1 = "example.package"

    val javaSourceRoot1 = JavaSourceRoot(sourceDir1, generated1, packagePrefix1)

    val sourceDir2 = URI.create("file:///another/root/dir/another/example/package/").toPath()
    val generated2 = true
    val packagePrefix2 = "another.example.package"

    val javaSourceRoot2 = JavaSourceRoot(sourceDir2, generated2, packagePrefix2)

    val javaSourceRoots = listOf(javaSourceRoot1, javaSourceRoot2)

    // when
    val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedJavaSourceRootEntries: Collection<JavaSourceRootEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedJavaSourceRootEntries = javaSourceEntityUpdater.addEntries(javaSourceRoots, parentModuleEntity)
    }

    // then
    val virtualSourceDir1 = sourceDir1.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntityDetails1 = ExpectedJavaSourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualSourceDir1, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualSourceDir1, "java-source"),
      javaSourceRootEntity = JavaSourceRootEntity(generated1, packagePrefix1),
    )

    val virtualSourceDir2 = sourceDir2.toVirtualFileUrl(virtualFileUrlManager)
    val expectedJavaSourceRootEntityDetails2 = ExpectedJavaSourceRootEntityDetails(
      contentRootEntity = ContentRootEntity(virtualSourceDir2, emptyList(), emptyList()),
      sourceRootEntity = SourceRootEntity(virtualSourceDir2, "java-source"),
      javaSourceRootEntity = JavaSourceRootEntity(generated2, packagePrefix2),
    )

    returnedJavaSourceRootEntries shouldContainExactlyInAnyOrder Pair(
      listOf(expectedJavaSourceRootEntityDetails1, expectedJavaSourceRootEntityDetails2),
      this::validateJavaSourceRootEntity
    )

    workspaceModelLoadedEntries(JavaSourceRootEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedJavaSourceRootEntityDetails1, expectedJavaSourceRootEntityDetails2),
      this::validateJavaSourceRootEntity
    )
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
    actualModuleEntity shouldBe parentModuleEntity
  }
}

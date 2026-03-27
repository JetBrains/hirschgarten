package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class SourceRootsOverlapTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testIndexing() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/source_roots_overlap")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      val moduleEntity = WorkspaceModel.getInstance(fixture.project)
        .currentSnapshot
        .entities(ModuleEntity::class.java)
        .single()
      moduleEntity.contentRoots.shouldHaveSize(2)
      moduleEntity.shouldHaveSingleContentRoot("java-source", "module/src/main/java")
      moduleEntity.shouldHaveSingleContentRoot("java-resource", "module/src/main/java/com/example/data.html")
    }
  }

  private fun ModuleEntity.shouldHaveSingleContentRoot(rootTypeName: String, rootFile: String) {
    val expectedFileUrl = fixture.findFileInTempDir(rootFile)
      .toVirtualFileUrl(WorkspaceModel.getInstance(fixture.project).getVirtualFileUrlManager())
    val contentRoot = contentRoots.find { it.url == expectedFileUrl } ?: error("Content root with url $expectedFileUrl not found")
    contentRoot.sourceRoots.shouldHaveSingleElement { it.url == expectedFileUrl && it.rootTypeId.name == rootTypeName }
  }
}

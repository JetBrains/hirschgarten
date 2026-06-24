package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class CustomAspectMergeTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testTwoAspectsOnOneTargetProduceDistinctFullKeyLibraries(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.copyBazelTestProject("redcodes/custom_aspect_merge")
    fixture.setBazelVersion("9.1.0")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("app/App.java")

      val snapshot = WorkspaceModel.getInstance(fixture.project).currentSnapshot
      val shadows = snapshot.entities(LibraryEntity::class.java).toList()
        .filter { it.name.startsWith("my_schema-") }

      shadows shouldHaveSize 2

      val appModule = snapshot.entities(ModuleEntity::class.java).single { it.name == "app.app" }
      appModule.bazelModuleExtension?.strictDependencies?.labels.orEmpty() shouldContain "@//:my_schema"
    }
  }
}

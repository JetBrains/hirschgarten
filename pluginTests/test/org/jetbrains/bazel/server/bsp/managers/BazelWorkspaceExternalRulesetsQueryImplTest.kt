package org.jetbrains.bazel.server.bsp.managers

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
internal class BazelWorkspaceExternalRulesetsQueryImplTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testCustomAspectGeneratedClassResolves() = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.enableGoHighlighting()
    fixture.copyBazelTestProject("redcodes/go_workspace_git_repository")
    fixture.setBazelVersion("7.7.1")
    fixture.performBazelSync(buildProject = false)

    val project = fixture.project
    val workspaceIndex = WorkspaceFileIndex.getInstance(project)
    readAction { workspaceIndex.isIndexable(project.rootDir.findFileByRelativePath("main.go")!!) shouldBe true }

    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("main.go")
    }
  }
}

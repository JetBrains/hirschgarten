package org.jetbrains.bazel.server.bsp.managers

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.common.timeoutRunBlocking
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
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
internal class BazelWorkspaceExternalRulesetsQueryImplTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_workspace_git_repository",
    bazelVersion = "7.7.1",
    configure = { it.enableGoHighlighting() },
  )

  @Test
  @DisabledOnOs(OS.WINDOWS) // bazel failed to get go rules
  fun testCustomAspectGeneratedClassResolves() = timeoutRunBlocking(timeout = 5.minutes) {
    val project = fixture.project
    val workspaceIndex = WorkspaceFileIndex.getInstance(project)
    readAction { workspaceIndex.isIndexable(project.rootDir.findFileByRelativePath("main.go")!!) shouldBe true }

    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("main.go")
    }
  }
}

package org.jetbrains.bazel.projectAware

import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.util.Disposer
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test

internal class BazelWorkspaceTest : WorkspaceModelBaseTest() {
  @Test
  fun `initialize registers and activates the project aware`() {
    val workspace = BazelWorkspace(project)
    Disposer.register(disposable, workspace)

    runBlocking { workspace.initialize() }

    activatedProjectIds() shouldContain projectAwareId()
  }

  @Test
  fun `disposing the workspace removes the registration`() {
    val workspace = BazelWorkspace(project)
    runBlocking { workspace.initialize() }

    Disposer.dispose(workspace)

    activatedProjectIds() shouldNotContain projectAwareId()
  }

  @Test
  fun `initialize after disposal registers nothing and does not throw`() {
    val workspace = BazelWorkspace(project)
    Disposer.dispose(workspace)

    shouldNotThrowAny {
      runBlocking { workspace.initialize() }
    }

    activatedProjectIds() shouldNotContain projectAwareId()
  }

  private fun activatedProjectIds(): Set<ExternalSystemProjectId> = AutoImportProjectTracker.getInstance(project).getActivatedProjects()

  private fun projectAwareId(): ExternalSystemProjectId = BazelProjectAware(project).projectId
}

package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class IdeModuleNameTagTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testIdeModuleNameTagRenamesImportedModule(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.copyBazelTestProject("redcodes/ide_module_name")
    fixture.performBazelSync(buildProject = false)

    val moduleNames = withContext(Dispatchers.EDT) {
      WorkspaceModel.getInstance(fixture.project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .map { it.name }
        .toList()
    }

    moduleNames shouldContainAll listOf("custom.module.name", "untagged")
    moduleNames shouldNotContain "tagged"
  }
}

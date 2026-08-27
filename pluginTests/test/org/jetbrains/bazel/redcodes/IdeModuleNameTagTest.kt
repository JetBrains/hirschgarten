package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class IdeModuleNameTagTest {

  private val project by bazelProjectFixture("redcodes/ide_module_name")

  @Test
  fun testIdeModuleNameTagRenamesImportedModule(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val moduleNames = withContext(Dispatchers.EDT) {
      WorkspaceModel.getInstance(project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .map { it.name }
        .toList()
    }

    moduleNames shouldContainAll listOf("custom.module.name", "untagged")
    moduleNames shouldNotContain "tagged"
  }
}

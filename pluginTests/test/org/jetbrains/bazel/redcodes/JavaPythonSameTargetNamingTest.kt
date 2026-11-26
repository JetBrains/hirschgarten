package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class JavaPythonSameTargetNamingTest {

  private val project by bazelProjectFixture(
    "redcodes/java_python_same_target",
    bazelVersion = "9.1.0",
  )

  @Test
  fun testTargetWithJavaAndPythonProvidersGetsOneModulePerImporter(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    withContext(Dispatchers.EDT) {
      val modules = WorkspaceModel.getInstance(project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .filter { it.name.startsWith("mixed.shared") }
        .associate { it.name to it.type }

      modules shouldBe mapOf(
        "mixed.shared-jvm" to ModuleTypeId("JAVA_MODULE"),
        "mixed.shared-python" to ModuleTypeId("PYTHON_MODULE"),
      )
    }
  }
}

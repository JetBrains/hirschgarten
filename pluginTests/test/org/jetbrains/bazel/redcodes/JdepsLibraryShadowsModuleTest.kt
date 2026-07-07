package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class JdepsLibraryShadowsModuleTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `jar reached only through an out-of-scope re-exporting library resolves to source`(): Unit =
    timeoutRunBlocking(timeout = 5.minutes) {
      fixture.copyBazelTestProject("redcodes/jdeps_library_shadows_module")
      fixture.setProjectView(projectview = ".managed.bazelproject")
      fixture.setBazelVersion("9.1.0")
      fixture.performBazelSync(buildProject = true)

      fixture.assertResolvesToSource(
        usage = "A.value",
        expectedSource = "liba/A.java",
        producerModule = "liba.a",
      )
    }

  @Test
  fun `jar exported through a custom code-gen aspect resolves to source`(): Unit =
    timeoutRunBlocking(timeout = 5.minutes) {
      fixture.copyBazelTestProject("redcodes/custom_aspect_runtime_shadow")
      fixture.setBazelVersion("9.1.0")
      fixture.performBazelSync(buildProject = true)

      fixture.assertResolvesToSource(
        usage = "Message.text",
        expectedSource = "rt/Message.java",
        producerModule = "rt.rt",
      )
    }
}

private suspend fun BazelSyncCodeInsightTestFixture.assertResolvesToSource(
  usage: String,
  expectedSource: String,
  producerModule: String,
) {
  withContext(Dispatchers.EDT) {
    checkHighlighting("app/App.java")

    val psiFile = configureFromTempProjectFile("app/App.java")
    val offset = psiFile.text.indexOf(usage)
    offset shouldBeGreaterThanOrEqual 0
    readAction {
      val resolved = psiFile.findReferenceAt(offset)?.resolve() ?: error("unresolved ref")
      val vf = resolved.containingFile?.virtualFile ?: error("no vf")
      // file should come from real sources, not compiled library
      (vf.fileSystem is JarFileSystem) shouldBe false
      vf.path shouldEndWith expectedSource
    }

    val snapshot = WorkspaceModel.getInstance(project).currentSnapshot
    val appModule = snapshot.entities(ModuleEntity::class.java).single { it.name == "app.app" }
    val moduleDeps = appModule.dependencies.filterIsInstance<ModuleDependency>().map { it.module.name }
    moduleDeps shouldContain producerModule
  }
}

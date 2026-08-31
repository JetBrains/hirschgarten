package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightTestFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class PartialSyncTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `partial sync adds the requested target and keeps the targets of the previous sync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
        fixture.checkHighlighting("base/Base.java")
        fixture.checkHighlighting("stale/Stale.java")
      }
    }

  @Test
  fun `partial sync without a previous sync imports the requested target only`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))

      fixture.moduleNames() shouldContainAll listOf("extra.extra", "base.base")
      fixture.moduleNames() shouldNotContain "stale.stale"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
      }
    }

  @Test
  fun `partial sync refreshes a target that changed after the previous sync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldContain "stale.stale"
      fixture.moduleNames() shouldNotContain "helper.helper"

      fixture.overwrite(
        "stale/BUILD.bazel",
        """
        load("@rules_java//java:java_library.bzl", "java_library")

        java_library(
            name = "stale",
            srcs = ["Stale.java"],
            visibility = ["//visibility:public"],
            deps = ["//helper:helper"],
        )
        """.trimIndent(),
      )
      fixture.overwrite(
        "stale/Stale.java",
        """
        package stale;

        import helper.Helper;

        public final class Stale {
          public static String describe() {
            return "stale(" + Helper.tag() + ")";
          }
        }
        """.trimIndent(),
      )

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//stale:stale")), build = false))

      fixture.moduleNames() shouldContainAll listOf("stale.stale", "helper.helper")
      fixture.moduleDependencies("stale.stale") shouldContain "helper.helper"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("stale/Stale.java")
      }
    }

  @Test
  fun `partial sync adds a target from a local bzlmod repository`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync_repo")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldContainAll listOf("base.base", "dep_repo.lib.dep_lib")
      fixture.moduleNames() shouldNotContain "extra.extra"
      fixture.moduleNames() shouldNotContain "ext_repo.lib.ext_lib"

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))

      // the merge adds the new repository and keeps the repository of the previous sync
      fixture.moduleNames() shouldContainAll
        listOf("base.base", "dep_repo.lib.dep_lib", "extra.extra", "ext_repo.lib.ext_lib")
      fixture.moduleDependencies("extra.extra") shouldContain "ext_repo.lib.ext_lib"
      fixture.moduleDependencies("base.base") shouldContain "dep_repo.lib.dep_lib"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
        fixture.checkHighlighting("base/Base.java")
      }
    }
}

private fun BazelSyncCodeInsightTestFixture.moduleNames(): List<String> =
  WorkspaceModel.getInstance(project).currentSnapshot
    .entities(ModuleEntity::class.java)
    .map { it.name }
    .toList()


private fun BazelSyncCodeInsightTestFixture.moduleDependencies(moduleName: String): List<String> =
  WorkspaceModel.getInstance(project).currentSnapshot
    .entities(ModuleEntity::class.java)
    .single { it.name == moduleName }
    .dependencies
    .filterIsInstance<ModuleDependency>()
    .map { it.module.name }


private suspend fun BazelSyncCodeInsightTestFixture.overwrite(path: String, text: String) {
  val file = tempDirFixture.getFile(path) ?: error("file not found $path")
  edtWriteAction { VfsUtil.saveText(file, text) }
}

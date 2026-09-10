package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
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
import kotlin.io.path.Path
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

  @Test
  fun `file sync adds the target that owns a changed source file`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"

      fixture.performFileSync("extra/Extra.java")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
        fixture.checkHighlighting("base/Base.java")
      }
    }

  @Test
  fun `file sync adds every rule target of a changed BUILD file`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldNotContain "extra.extra"

      fixture.performFileSync("extra/BUILD.bazel")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
      }
    }

  @Test
  fun `file sync of a directory adds every rule target beneath it`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldNotContain "extra.extra"
      fixture.moduleNames() shouldNotContain "extra.nested.nested"

      fixture.performFileSync("extra")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra", "extra.nested.nested")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
        fixture.checkHighlighting("extra/nested/Nested.java")
      }
    }

  @Test
  fun `file sync of a source file that no target uses keeps the previous sync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      // `Unused.java` belongs to the package `//base`, but no target lists it in `srcs`
      fixture.performFileSync("base/Unused.java")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("base/Base.java")
      }
    }

  @Test
  fun `file sync of a file outside every package keeps the previous sync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      // no directory above `docs` have a BUILD file, so the file belongs to no package
      fixture.performFileSync("docs/notes.md")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"
    }

  @Test
  fun `file sync refreshes a target whose source and BUILD file changed`(): Unit =
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

      fixture.performFileSync("stale/BUILD.bazel", "stale/Stale.java")

      fixture.moduleNames() shouldContainAll listOf("stale.stale", "helper.helper")
      fixture.moduleDependencies("stale.stale") shouldContain "helper.helper"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("stale/Stale.java")
      }
    }

  @Test
  fun `file sync of a starlark file adds every target of the BUILD files that load it`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      // only `//extra:BUILD.bazel` loads `//rules:java_rules.bzl`
      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"

      fixture.performFileSync("rules/java_rules.bzl")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
        fixture.checkHighlighting("base/Base.java")
      }
    }

  @Test
  fun `file sync of a changed starlark file and its BUILD file adds the target once`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldNotContain "extra.extra"

      fixture.overwrite(
        "rules/java_rules.bzl",
        """
        load("@rules_java//java:java_library.bzl", "java_library")

        def public_java_library(name, srcs, deps = []):
            java_library(
                name = name,
                srcs = srcs,
                deps = deps + ["//helper:helper"],
                visibility = ["//visibility:public"],
            )
        """.trimIndent(),
      )

      fixture.performFileSync("rules/java_rules.bzl", "extra/BUILD.bazel")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale", "extra.extra", "helper.helper")
      fixture.moduleDependencies("extra.extra") shouldContain "helper.helper"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("extra/Extra.java")
      }
    }

  @Test
  fun `file sync of a starlark file that no package holds does a full resync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))
      fixture.moduleNames() shouldContain "extra.extra"

      fixture.performFileSync("docs/orphan_rules.bzl")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"
    }

  @Test
  fun `file sync of a starlark file of a local bzlmod repository adds the targets of that repository`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync_repo")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.moduleNames() shouldContainAll listOf("base.base", "dep_repo.lib.dep_lib")
      fixture.moduleNames() shouldNotContain "dep_repo.util.dep_util"

      fixture.performFileSync("dep_repo/rules/java_rules.bzl")

      fixture.moduleNames() shouldContainAll listOf("base.base", "dep_repo.lib.dep_lib", "dep_repo.util.dep_util")

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("base/Base.java")
        fixture.checkHighlighting("dep_repo/util/DepUtil.java")
      }
    }

  @Test
  fun `file sync of MODULE dot bazel does a full resync and drops the extra target`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      // `//extra:extra` is not in the project view, so only a partial sync can add it
      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))
      fixture.moduleNames() shouldContain "extra.extra"

      fixture.performFileSync("MODULE.bazel")

      // the full resync discards the old state and imports the project view targets only
      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"

      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("base/Base.java")
      }
    }

  @Test
  fun `file sync of the project view file does a full resync`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      fixture.performBazelSync(ProjectSyncScope.Targets(patterns = listOf(Label.parse("//extra:extra")), build = false))
      fixture.moduleNames() shouldContain "extra.extra"

      fixture.performFileSync(".bazelproject")

      fixture.moduleNames() shouldContainAll listOf("base.base", "stale.stale")
      fixture.moduleNames() shouldNotContain "extra.extra"
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


private suspend fun BazelSyncCodeInsightTestFixture.performFileSync(vararg paths: String) {
  val projectRoot = Path(tempDirPath)
  performBazelSync(ProjectSyncScope.Files(files = paths.map { projectRoot.resolve(it) }, build = false))
}

private suspend fun BazelSyncCodeInsightTestFixture.overwrite(path: String, text: String) {
  val file = tempDirFixture.getFile(path) ?: error("file not found $path")
  edtWriteAction { VfsUtil.saveText(file, text) }
}

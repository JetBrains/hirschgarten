package org.jetbrains.bazel.tests.sync

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.base.assertFileKind
import org.jetbrains.bazel.base.assertSyncedTargets
import org.jetbrains.bazel.base.buildAndSync
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.refreshFile
import org.jetbrains.bazel.base.switchProjectView
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForBazelFileEventProcessorIdle
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.preCacheBazelisk
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.performanceImpl.FileKindCheck.INDEXABLE
import org.jetbrains.bazel.performanceImpl.FileKindCheck.IN_CONTENT
import org.jetbrains.bazel.performanceImpl.FileKindCheck.IN_TARGETS
import org.jetbrains.bazel.performanceImpl.FileKindCheck.NON_INDEXABLE
import org.jetbrains.bazel.performanceImpl.FileKindCheck.NOT_IN_TARGETS
import org.jetbrains.bazel.performanceImpl.FileKindCheck.OUTSIDE_CONTENT
import org.jetbrains.bazel.tests.combined.IdeStarterCombinedBaseTest
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val INDEX_SECTION_PROJECT = simpleBazelProject(
  path = "indexSectionTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
    preCacheBazelisk(context)
  },
)


class IndexSectionTest : IdeStarterCombinedBaseTest() {

  override fun createContext(): IDETestContext = createContext(
    projectName = "indexSection",
    case = IdeaBazelCases.withProject(INDEX_SECTION_PROJECT)
  )

  override fun IDETestContext.runIde(): BackgroundRun = runIdeWithDriver(runTimeout = timeout, pauseOnIndicators = 5.minutes)

  override fun Driver.syncBazelProject() = syncBazelProject(buildAndSync = true)

  @Test @Order(1)
  fun `patterns select the indexed files after sync`() {
    withDriver(bgRun) {
      ideFrame {
        step("Verify the target source is indexed") {
          execute { assertSyncedTargets("//lib:lib") }
          execute { assertFileKind("lib/Lib.java", IN_TARGETS, INDEXABLE) }
        }
        step("Verify the filename patterns index files outside the targets") {
          execute { assertFileKind("data/config.xml", NOT_IN_TARGETS, IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("data/data_fixture.txt", NOT_IN_TARGETS, IN_CONTENT, INDEXABLE) }
        }
        step("Verify the directory pattern indexes all files under the directory") {
          execute { assertFileKind("docs/guide.md", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("docs/nested/deep.txt", IN_CONTENT, INDEXABLE) }
        }
        step("Verify the built-in Bazel patterns still apply") {
          execute { assertFileKind("tools/defs.bzl", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("lib/BUILD.bazel", IN_CONTENT, INDEXABLE) }
        }
        step("Verify a file that no pattern matches is not indexed") {
          execute { assertFileKind("data/notes.txt", IN_CONTENT, NON_INDEXABLE) }
        }
        step("Verify an excluded directory stays excluded under a directory pattern") {
          execute { assertFileKind("docs/excluded/hidden.md", OUTSIDE_CONTENT) }
        }
      }
    }
  }

  @Test @Order(2)
  fun `new files follow the patterns without a resync`() {
    withDriver(bgRun) {
      ideFrame {
        step("Create new files on disk") {
          createProjectFile("data/created.xml", "<created/>\n")
          createProjectFile("data/created_fixture.txt", "created\n")
          createProjectFile("data/created.txt", "created\n")
          createProjectFile("docs/created.md", "# Created\n")
          execute {
            refreshFile("data")
            refreshFile("docs")
          }
          // BazelFileEventListener processes the events after a delay of 250 ms.
          wait(2.seconds)
          execute { waitForBazelFileEventProcessorIdle() }
        }
        step("Verify the new files that a pattern matches are indexed") {
          execute { assertFileKind("data/created.xml", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("data/created_fixture.txt", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("docs/created.md", IN_CONTENT, INDEXABLE) }
        }
        step("Verify a new file that no pattern matches is not indexed") {
          execute { assertFileKind("data/created.txt", IN_CONTENT, NON_INDEXABLE) }
        }
      }
    }
  }

  @Test @Order(3)
  fun `match-all pattern indexes all files in directories`() {
    withDriver(bgRun) {
      ideFrame {
        step("Switch to the index-everything view and resync") {
          execute { switchProjectView("index-everything.bazelproject") }
          execute {
            buildAndSync()
            waitForSmartMode()
          }
          waitForSyncSucceeded()
        }
        step("Verify the files that no pattern matched before are indexed") {
          execute { assertFileKind("data/notes.txt", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind("data/created.txt", IN_CONTENT, INDEXABLE) }
          execute { assertFileKind(".bazelversion", IN_CONTENT, INDEXABLE) }
        }
        step("Verify an excluded directory stays excluded") {
          execute { assertFileKind("docs/excluded/hidden.md", OUTSIDE_CONTENT) }
        }
      }
    }
  }

  private fun createProjectFile(relativePath: String, content: String) {
    (ctx.resolvedProjectHome / relativePath)
      .createParentDirectories()
      .writeText(content)
  }
}

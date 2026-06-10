package org.jetbrains.bazel.redcodes

import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.workspace.fileEvents.FileEventQueueController
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
internal class FileSystemRefreshTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `model is updated on added file`() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/file_system_refresh")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting(
        "module/src/com/example/Module.java",
        expected = ExpectedHighlightingData(
          MockDocument().apply {
            replaceText(
              """
                package com.example;

                class Module implements com.<error descr="Cannot resolve symbol 'core'">core</error>.IModule {
                }
               """.trimIndent(),
              1,
            )
          },
        ).also { it.init() },
      )

      // Create file and resync FS
      fixture.tempDirFixture.createFile(
        "/module/gen/com/core/IModule.java",
        """
          package com.core;

          public interface IModule {
             void foo();
          }
        """.trimIndent(),
      )
      PlatformTestUtil.flushAllPendingVFSUpdates()
      waitFor {
        FileEventQueueController.getInstance(fixture.project).isIdle()
      }

      // Should highlight with new file
      fixture.checkHighlighting(
        "module/src/com/example/Module.java",
        expected = ExpectedHighlightingData(
          MockDocument().apply {
            replaceText(
              """
                package com.example;

                <error descr="Class 'Module' must either be declared abstract or implement abstract method 'foo()' in 'IModule'">class Module implements com.core.IModule</error> {
                }
               """.trimIndent(),
              1,
            )
          },
        ).also { it.init() },
      )
    }
  }

  private suspend fun waitFor(condition: suspend () -> Boolean) {
    val start = System.currentTimeMillis()
    var success = false
    do {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
      if (condition()) {
        success = true
        break
      }
      delay(100.milliseconds)
    }
    while ((System.currentTimeMillis() - start) < 10.seconds.inWholeMilliseconds)
    if (!success)
      error("Timed out waiting for refresh")
  }
}

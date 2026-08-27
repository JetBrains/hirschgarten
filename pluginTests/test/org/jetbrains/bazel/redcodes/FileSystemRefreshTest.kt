package org.jetbrains.bazel.redcodes

import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.PlatformTestUtil
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessorResult
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class FileSystemRefreshTest {
  private val fixture by bazelSyncCodeInsightFixture("redcodes/file_system_refresh")

  @Test
  fun `model is updated on added file`() = runBlocking(Dispatchers.Default) {
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

      // Create file in stash and resync FS
      // Since stash is ignored, the file should not affect PSI
      val newFile = fixture.tempDirFixture.createFile(
        "/module/gen/stash/IModule.java",
        """
          package com.core;

          public interface IModule {
             void foo();
          }
        """.trimIndent(),
      )
      waitForFileEventsProcessor(
        VFileCreateEvent(
          this,
          newFile.parent,
          "IModule.java",
          false,
          null, null, null,
        )
      )

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

      // Move file out of stash. Now it should affect PSI
      WriteAction.runAndWait<Throwable>(
        {
          newFile.move(
            this@FileSystemRefreshTest,
            fixture.tempDirFixture.findOrCreateDir("/module/gen/com/core"),
          )
        },
      )
      waitForFileEventsProcessor(
        VFileMoveEvent(
          this,
          newFile,
          newFile.parent,
        )
      )

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

  @Test
  fun `model is not invalidated on added resource file`() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      // Create resource file and resync FS
      val newFile = fixture.tempDirFixture.createFile(
        "/module/resources/data/File.java",
        """
          class File {}
        """.trimIndent(),
      )
      val result = waitForFileEventsProcessor(
        VFileCreateEvent(
          this,
          newFile.parent,
          "File.java",
          false,
          null, null, null,
        )
      )

      result.isEmpty() shouldBe true
    }
  }

  private suspend fun waitForFileEventsProcessor(event: VFileEvent): BazelFileEventProcessorResult {
    PlatformTestUtil.flushAllPendingVFSUpdates()
    return BazelFileEventProcessor.getInstance(fixture.project)
      .enqueue(listOf(event))
      .await()

    //val start = System.currentTimeMillis()
    //var success = false
    //do {
    //  PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    //  if (BazelFileEventProcessor.getInstance(fixture.project).isIdle()) {
    //    success = true
    //    break
    //  }
    //  delay(100.milliseconds)
    //}
    //while ((System.currentTimeMillis() - start) < 10.seconds.inWholeMilliseconds)
    //if (!success)
    //  error("Timed out waiting for refresh")
  }
}

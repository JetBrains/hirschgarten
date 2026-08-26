package org.jetbrains.bazel.redcodes

import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.mock.MockDocument
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class GoStrictDepsTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.enableGoHighlighting()
    fixture.copyBazelTestProject("redcodes/go_strict_deps")
    fixture.performBazelSync()
    fixture.checkHighlighting(
      "a/a.go",
      expected = ExpectedHighlightingData(
        MockDocument().apply {
          replaceText(
            """
                package a

                import (
                  "github.com/example/b"
                  <error descr="Missing strict dependency: import of 'github.com/example/c'">"github.com/example/c"</error>
                  "<error descr="Cannot resolve directory 'github.com'">github.com</error>/<error descr="Cannot resolve file 'nonexistent'">nonexistent</error>"
                )
                
                func A() {
                  b.B()
                  c.C()
                }
               """.trimIndent(),
            1,
          )
        },
      ).also { it.init() },
    )
    // Delete the nonexistent import so that Gazelle succeeds
    withContext(Dispatchers.EDT) { fixture.performEditorAction(IdeActions.ACTION_EDITOR_DELETE_LINE) }
    val quickFix = fixture.getAllQuickFixes().first { it.text == "Run //:gazelle and resync project" }
    withContext(Dispatchers.EDT) {
      ShowIntentionActionsHandler.chooseActionAndInvoke(fixture.file, fixture.editor, quickFix, quickFix.getText())
    }
    waitForSyncToFinish(fixture.project)
    // All red code should be fixed
    fixture.checkHighlighting("a/a.go")
  }

  private suspend fun waitForSyncToFinish(project: Project) {
    val syncFinished = CompletableDeferred<Unit>()
    val connection = project.messageBus.connect()
    try {
      connection.subscribe(
        SyncStatusListener.TOPIC,
        object : SyncStatusListener {
          override fun syncStarted() {}
          override fun syncFinished(canceled: Boolean) {
            syncFinished.complete(Unit)
          }
        },
      )
      syncFinished.await()
    }
    finally {
      connection.disconnect()
    }
  }
}

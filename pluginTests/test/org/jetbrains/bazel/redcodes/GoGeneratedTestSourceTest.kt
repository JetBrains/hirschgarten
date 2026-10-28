package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class GoGeneratedTestSourceTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  @DisabledOnOs(OS.WINDOWS) // test use shell
  fun testGeneratedTestSourceResolves(): Unit = runBlocking(Dispatchers.Default) {
    fixture.enableGoHighlighting()
    fixture.copyBazelTestProject("redcodes/go_generated_test_source")
    fixture.performBazelSync(buildProject = true)
    fixture.checkHighlighting("greeter/greeter_internal_test.go")
    val definitionFile = withContext(Dispatchers.EDT) {
      val file = fixture.file
      val reference = file.findReferenceAt(file.text.indexOf("NewMockGreeter"))
        .shouldNotBeNull()
      reference.resolve()?.containingFile?.virtualFile.shouldNotBeNull()
    }
    definitionFile.path shouldContain "/intellij-hardlinks/"
    readAction { WorkspaceFileIndex.getInstance(fixture.project).isIndexable(definitionFile) }
      .shouldBeTrue()
  }
}

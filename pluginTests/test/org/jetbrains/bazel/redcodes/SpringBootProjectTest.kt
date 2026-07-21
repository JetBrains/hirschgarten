package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.spring.SpringInspectionsRegistry
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test

@BazelTestApplication
class SpringBootProjectTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testGutterMarks(): Unit = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/spring_boot")
    fixture.enableInspections(*SpringInspectionsRegistry.getInstance().getTestSpringInspectionClasses())
    fixture.performBazelSync()
    val gutters = withContext(Dispatchers.EDT) {
      fixture.findAllGutters("src/main/java/com/example/greeting/GreetingModule.java").map { gutter ->
        gutter.tooltipText ?: "<null>"
      }
    }

    gutters shouldBeEqual
      listOf(
        "<html>Navigate to the Spring bean declaration(s)<hr size=1 noshade>Select in Spring View</html>",
        "Navigate to autowired candidates",
        "Navigate to autowired candidates",
        "Navigate to the autowired dependencies",
      )
  }
}

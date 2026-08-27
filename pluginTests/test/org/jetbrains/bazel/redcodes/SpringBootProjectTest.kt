package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.spring.SpringInspectionsRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class SpringBootProjectTest {
  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/spring_boot",
    configure = { it.enableInspections(*SpringInspectionsRegistry.getInstance().getTestSpringInspectionClasses()) },
  )

  @Test
  @DisabledOnOs(OS.WINDOWS) // coursier
  fun testGutterMarks(): Unit = runBlocking(Dispatchers.Default) {
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

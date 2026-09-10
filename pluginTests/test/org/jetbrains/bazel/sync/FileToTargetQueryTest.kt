package org.jetbrains.bazel.sync

import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class FileToTargetQueryTest {

  private val project by bazelProjectFixture("redcodes/partial_sync", projectView = ".bazelproject")

  private suspend fun fileToTargets(vararg files: String): Map<Label, List<Label>> {
    val fileLabels = files.map { Label.parse(it) }.toSet()
    return project.connection.runWithServer { server -> FileToTargetQuery.findDependantTargetsFromFiles(server, fileLabels) }
  }

  @Test
  fun `query files to target test`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fileToTargets("//base:Base.java") shouldBe mapOf(Label.parse("//base:Base.java") to listOf(Label.parse("//base:base")))

      fileToTargets("//base:Base.java", "//extra:Extra.java", "//helper:Helper.java") shouldBe mapOf(
        Label.parse("//base:Base.java") to listOf(Label.parse("//base:base")),
        Label.parse("//extra:Extra.java") to listOf(Label.parse("//extra:extra")),
        Label.parse("//helper:Helper.java") to listOf(Label.parse("//helper:helper")),
      )

      val targets = fileToTargets("//base:Unused.java", "//base:Base.java")

      targets[Label.parse("//base:Unused.java")].shouldNotBeNull().shouldBeEmpty()
      targets[Label.parse("//base:Base.java")] shouldBe listOf(Label.parse("//base:base"))

      fileToTargets() shouldBe emptyMap()
    }
}

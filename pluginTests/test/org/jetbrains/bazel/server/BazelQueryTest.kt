package org.jetbrains.bazel.server

import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class BazelQueryTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  private suspend fun <T> query(params: BazelQueryParams<T>): BazelQueryResult<T> =
    fixture.project.connection.runWithServer { server -> server.query(params) }

  @Test
  fun `query gives the labels, the target protos and the raw output of one expression`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      val labels = query(BazelQueryParams("//base:base", BazelQueryOutput.Labels))
      labels.status shouldBe BazelStatus.SUCCESS
      labels.result shouldContain Label.parse("//base:base")

      val targets = query(BazelQueryParams("//base:base", BazelQueryOutput.Targets))
      targets.status shouldBe BazelStatus.SUCCESS
      targets.result.map { it.rule.name } shouldContain "//base:base"

      val proto = query(BazelQueryParams("//base:base", BazelQueryOutput.Proto))
      proto.status shouldBe BazelStatus.SUCCESS
      proto.result.targetList.map { it.rule.name } shouldContain "//base:base"

      val raw = query(BazelQueryParams("buildfiles(//base:base)", BazelQueryOutput.Raw()))
      raw.status shouldBe BazelStatus.SUCCESS
      raw.result shouldContain "//base:BUILD.bazel"
    }

  @Test
  fun `query gives a failing status and the errors instead of throwing`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      fixture.copyBazelTestProject("redcodes/partial_sync")
      fixture.setProjectView(".bazelproject")
      fixture.performBazelSync()

      val result = query(BazelQueryParams("//no/such:target", BazelQueryOutput.Labels, keepGoing = false))

      result.status shouldNotBe BazelStatus.SUCCESS
      result.result.shouldBeEmpty()
      result.stderrLines.shouldNotBeEmpty()
    }
}

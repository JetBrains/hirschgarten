package org.jetbrains.bsp.protocol

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.junit.jupiter.api.Test

class BuildTargetTest {
  private fun targetWithTags(vararg tags: String): BuildTarget = TestBuildTarget(tags = tags.toList())

  @Test
  fun `isManual should return false for a target without tags`() {
    targetWithTags().isManual shouldBe false
  }

  @Test
  fun `isManual should return true for a target tagged manual`() {
    targetWithTags(BuildTargetTag.MANUAL).isManual shouldBe true
  }

  @Test
  fun `isManual should return true for a target tagged manual among other tags`() {
    targetWithTags(BuildTargetTag.NO_IDE, BuildTargetTag.MANUAL, "my-custom-tag").isManual shouldBe true
  }

  @Test
  fun `isManual should return false for a target with other tags only`() {
    targetWithTags(BuildTargetTag.NO_IDE, "my-custom-tag").isManual shouldBe false
  }

  @Test
  fun `isManual should not match a tag which merely contains manual`() {
    targetWithTags("manually-created", "not-manual").isManual shouldBe false
  }
}

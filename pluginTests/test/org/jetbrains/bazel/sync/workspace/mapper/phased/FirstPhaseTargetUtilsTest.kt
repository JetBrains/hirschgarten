package org.jetbrains.bazel.sync.workspace.mapper.phased

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.phased.generatorName
import org.jetbrains.bazel.commons.phased.interestingDeps
import org.jetbrains.bazel.commons.phased.isManual
import org.jetbrains.bazel.commons.phased.isNoIde
import org.jetbrains.bazel.commons.phased.kind
import org.jetbrains.bazel.commons.phased.name
import org.jetbrains.bazel.commons.phased.srcs
import org.jetbrains.bazel.commons.phased.tags
import org.junit.jupiter.api.Test

class FirstPhaseTargetUtilsTest {
  @Test
  fun `should obtain target attributes using getters`() {
    // given
    val target =
      createMockTarget(
        name = "//target/name",
        kind = "java_library",
        tags = listOf("tag1", "tag2"),
        srcs = listOf("//target/name:src1.java", "//target/name:src2.java"),
        resources = listOf("//target/name:resource1.java", "//target/name:resource2.java"),
        deps = listOf("//target/name1", "//target/name2"),
        runtimeDeps = listOf("//target/name3", "//target/name4"),
        exports = listOf("//target/name5", "//target/name6"),
        generatorName = "other_target",
      )

    // when & then
    target.name shouldBe "//target/name"
    target.kind shouldBe "java_library"
    target.tags shouldContainExactlyInAnyOrder listOf("tag1", "tag2")
    target.srcs shouldContainExactlyInAnyOrder listOf("//target/name:src1.java", "//target/name:src2.java")
    target.interestingDeps shouldContainExactlyInAnyOrder
      listOf("//target/name1", "//target/name2", "//target/name3", "//target/name4", "//target/name5", "//target/name6")

    target.isManual shouldBe false
    target.isNoIde shouldBe false
    target.generatorName shouldBe "other_target"
  }

  @Test
  fun `isManual should return true for manual target`() {
    // given
    val target =
      createMockTarget(
        name = "//target/name",
        kind = "java_library",
        tags = listOf("manual"),
      )

    // when & then
    target.isManual shouldBe true
  }

  @Test
  fun `isNoIde should return true for no-ide target`() {
    // given
    val target =
      createMockTarget(
        name = "//target/name",
        kind = "java_library",
        tags = listOf("no-ide"),
      )

    // when & then
    target.isNoIde shouldBe true
  }
}

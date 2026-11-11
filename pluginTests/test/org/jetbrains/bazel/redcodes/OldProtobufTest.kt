package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class OldProtobufTest {
  private val fixture by bazelSyncCodeInsightFixture("redcodes/old_protobuf", buildProject = true)

  @Test
  @EnabledOnOs(OS.LINUX) // abseil :(
  fun testProtoHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("consumerJava/Main.java")
      fixture.checkHighlighting("libA/lib_a.proto")
      fixture.checkHighlighting("libB/lib_b.proto")
    }
  }
}

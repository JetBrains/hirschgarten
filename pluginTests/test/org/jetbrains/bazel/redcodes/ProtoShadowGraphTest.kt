package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProtoShadowGraphTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/proto_shadow_graph",
    buildProject = true,
    bazelVersion = "9.1.0",
  )

  @Test
  fun testProtoLibraryShadowResolvesGeneratedClass() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("App.java")
    }
  }
}

package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProtobufImportTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  /**
   *   requests.proto's import "google/protobuf/timestamp.proto" resolved to two physically different files, and the Protobuf plugin flags
   *   any import that multi-resolves to >1 file as "Ambiguous import". The two resolvers, both active by default:
   *   1. BazelProtobufFileResolveProvider → the real @protobuf copy that BazelProtobufSyncHook indexes during sync
   *      (the aspect emits a source_mapping for the transitive @protobuf//:timestamp_proto dep).
   *   2. SettingsFileResolveProvider → the Protobuf plugin's bundled well-known copy under include/,
   *      active because PbProjectSettings defaults includeWellKnownProtos = true, and the Bazel plugin never configures PbProjectSettings.
   *
   *   The fix: let the bundled protos own all of google/protobuf/... (they include both timestamp.proto and descriptor.proto),
   *   and make the Bazel provider decline well-known paths so exactly one resolver owns them.
   */
  @Test
  fun testProtoHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/protobuf_import")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("requests/requests.proto")
    }
  }
}

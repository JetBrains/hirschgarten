package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.kotlin.idea.i18n.KotlinInvalidBundleOrPropertyInspection
import org.junit.jupiter.api.Test

@BazelTestApplication
class ResourceRootMergingAllCasesTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/resource_merging_all_cases",
    configure = { it.enableInspections(KotlinInvalidBundleOrPropertyInspection()) },
  )

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("module/src/main/kotlin/com/example/Module.kt")
    }
  }
}

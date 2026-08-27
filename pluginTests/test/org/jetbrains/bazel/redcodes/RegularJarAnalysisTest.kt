package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.dataFlow.ConstantValueInspection
import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class RegularJarAnalysisTest {

  private val fixture by bazelSyncCodeInsightFixture("redcodes/regular_jar_analysis") {
    it.enableInspections(ConstantValueInspection())
  }

  @Test
  @DisabledOnOs(OS.WINDOWS) // coursier
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("Usage.java")
    }
  }
}

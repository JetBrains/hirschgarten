package org.jetbrains.bazel.import

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

@BazelTestApplication
class MixedImportTest {

  private val project by clionBazelProjectFixture("import/mixed", jvmToolchains = true)

  @Test
  fun testImportedTargets(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val importedTargets = project.targetStorage.allTargetSummaries().asSequence().filter { it.isWorkspace }.map { it.id }.toSet()
      // Verify that also the non-executable targets (that are not imported by default) are present of every language
      importedTargets shouldContain Label.parse("//cpp:library")
      importedTargets shouldContain Label.parse("//kotlin:library")
    }
  }
}

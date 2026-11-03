package org.jetbrains.bazel.languages.projectview.annotator

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
@SystemPropertyClassLevel(TrustedProjects.TRUST_HEADLESS_DISABLED_PROPERTY, "false")
internal class ProjectViewPathTrustTest {
  private val codeInsightFixture by bazelCodeInsightFixture()

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `checks that a path exists only in a trusted project`(trusted: Boolean) {
    val project = codeInsightFixture.project
    TrustedProjects.setProjectTrusted(project, trusted)
    val missingPath = project.rootDir.toNioPath().resolve("tools/missing_bazel")
    val message = BazelProjectViewBundle.message("annotator.path.not.exists.error", missingPath.toString())
    val value = if (trusted) """<warning descr="$message">tools/missing_bazel</warning>""" else "tools/missing_bazel"

    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(".bazelproject", "bazel_binary: $value")
        codeInsightFixture.checkHighlighting()
      }
    }
  }
}

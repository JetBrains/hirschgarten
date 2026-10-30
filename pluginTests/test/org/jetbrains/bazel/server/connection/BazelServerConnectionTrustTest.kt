package org.jetbrains.bazel.server.connection

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.writeText

@SystemPropertyClassLevel(TrustedProjects.TRUST_HEADLESS_DISABLED_PROPERTY, "false")
@BazelTestApplication
internal class BazelServerConnectionTrustTest {
  private val projectDir = tempPathFixture()
  private val project by projectFixture(projectDir, openAfterCreation = false)

  @Test
  fun `the server does not start in an untrusted project`(): Unit = timeoutRunBlocking {
    val root = projectDir.get()
    root.resolve("MODULE.bazel").writeText("")
    initializeBazelProject(project, root)
    TrustedProjects.setProjectTrusted(project, false)

    var taskRan = false
    val exception = shouldThrow<IllegalStateException> { project.connection.runWithServer { taskRan = true } }

    exception.message shouldContain "untrusted"
    taskRan shouldBe false
    // EnvironmentCreator writes this file before the server starts
    root.resolve(DOT_BAZELBSP_DIR_NAME).resolve(".gitignore").exists() shouldBe false
  }
}

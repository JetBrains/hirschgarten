package org.jetbrains.bazel.languages.starlark.formatting

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.exists
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

@BazelTestApplication
@SystemPropertyClassLevel(TrustedProjects.TRUST_HEADLESS_DISABLED_PROPERTY, "false")
@DisabledOnOs(OS.WINDOWS)
internal class StarlarkFormattingTrustTest {
  private val projectDir = tempPathFixture()
  private val project by projectFixture(pathFixture = projectDir, openAfterCreation = true)

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `buildifier runs only in a trusted project`(trusted: Boolean): Unit = timeoutRunBlocking {
    val root = projectDir.get()
    val marker = root.resolve("buildifier-ran")
    val buildifier = root.resolve("buildifier")
    buildifier.writeText("#!/bin/sh\ntouch '$marker'\ncat\n")
    buildifier.setPosixFilePermissions(PosixFilePermissions.fromString("rwx------"))
    project.bazelProjectSettings = project.bazelProjectSettings.withNewBuildifierExecutablePath(buildifier)
    TrustedProjects.setProjectTrusted(project, trusted)

    val buildFile = root.resolve("BUILD")
    buildFile.writeText("x = 1\n")
    val virtualFile = checkNotNull(buildFile.refreshAndFindVirtualFile())
    val psiFile = readAction { PsiManager.getInstance(project).findFile(virtualFile) } as StarlarkFile

    formatBuildFile(psiFile)

    marker.exists() shouldBe trusted
  }
}

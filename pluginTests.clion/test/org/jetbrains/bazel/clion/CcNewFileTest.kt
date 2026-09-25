package org.jetbrains.bazel.clion

import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurations
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.findResolveConfiguration
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcNewFileTest {

  private val project by clionBazelProjectFixture("clion/simple")

  @Test
  fun testFileInPackageDirectory() = assertConfiguration("lib/new.cc", expectedFrom = "lib/lib.cc")

  @Test
  fun testFileInPackageSubdirectory() = assertConfiguration("main/sub/new.cc", expectedFrom = "main/main.cc")

  private fun assertConfiguration(relativePath: String, expectedFrom: String): Unit = timeoutRunBlocking {
    val path = project.rootDir.toNioPath().resolve(relativePath)
    Files.createDirectories(path.parent)
    Files.writeString(path, "int foo() { return 0; }\n")

    val file = VfsUtil.findFile(path, /* refreshIfNeeded = */ true).assertNotNull()
    val expected = project.findResolveConfiguration(expectedFrom)

    val actual = readAction { OCResolveConfigurations.getPreselectedConfiguration(file, project) }
    assertThat(actual).isEqualTo(expected)
  }
}

package org.jetbrains.bazel.ignore

import com.intellij.testFramework.utils.vfs.createDirectory
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BazelIgnoreServiceTest: WorkspaceModelBaseTest() {
  @Test
  fun `test multiple repositories`() {
    runTestWriteAction {
      project.rootDir
        .createFile(".bazelignore")
        .setBinaryContent(
          """
            sources
            nested
          """.trimIndent().toByteArray(Charsets.UTF_8),
        )
      project.rootDir
        .createDirectory("nested")
        .createFile(".bazelignore")
        .setBinaryContent(
          """
            src
          """.trimIndent().toByteArray(Charsets.UTF_8),
        )
    }

    val workspaceRoot = Path.of(project.rootDir.path)
    (BazelIgnoreService.getInstance(project) as DefaultBazelIgnoreService)
      .update(
        workspaceRoot,
        BzlmodRepoMapping(
          canonicalRepoNameToLocalPath = mapOf("nested+" to Path.of("nested")),
          apparentRepoNameToCanonicalName = emptyMap(),
          canonicalRepoNameToPath = emptyMap(),
        )
      )

    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot).shouldBeFalse()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("sources")).shouldBeTrue()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("nested")).shouldBeFalse()
    BazelIgnoreService.getInstance(project).isIgnored(workspaceRoot.resolve("nested/src")).shouldBeTrue()
  }
}

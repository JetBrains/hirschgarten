package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

@DisplayName("RawUriToDirectoryPathTransformer.transform(rawUri) tests")
class RawUriToDirectoryPathTransformerTest {

  @Test
  fun `should return dir path for dir raw uri`() {
    // given
    val testDirPath = Files.createTempDirectory("dir")
    val rawTestDirUri = testDirPath.toUri().toString()

    // when
    val dirPath = RawUriToDirectoryPathTransformer.transform(rawTestDirUri)

    // then
    dirPath shouldBe testDirPath
  }

  @Test
  fun `should return dir path for file raw uri`() {
    // given
    val testFilePath = Files.createTempFile("dir", "File.txt")
    val rawTestFilePath = testFilePath.toUri().toString()

    // when
    val dirPath = RawUriToDirectoryPathTransformer.transform(rawTestFilePath)

    // then
    dirPath shouldBe testFilePath.parent
  }
}

package org.jetbrains.magicmetamodel.extensions

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Paths

@DisplayName("uri.toAbsolutePath() tests")
class URIToAbsolutePathExtensionTest {
  @Test
  fun `should map root uri to root path`() {
    // given
    val rootPath = "/"
    val rootUri = URI("file://$rootPath")

    // when
    val mappedRootAbsolutePath = rootUri.toAbsolutePath()

    // then
    mappedRootAbsolutePath shouldBe Paths.get(rootPath)
  }

  @Test
  fun `should map file uri to file path`() {
    // given
    val filePath = "/path/to/file.txt"
    val fileUri = URI("file://$filePath")

    // when
    val mappedFileAbsolutePath = fileUri.toAbsolutePath()

    // then
    mappedFileAbsolutePath shouldBe Paths.get(filePath)
  }

  @Test
  fun `should map directory uri to directory path`() {
    // given
    val directoryPath = "/path/to/directory/"
    val directoryUri = URI("file://$directoryPath")

    // when
    val mappedDirectoryAbsolutePath = directoryUri.toAbsolutePath()

    // then
    mappedDirectoryAbsolutePath shouldBe Paths.get(directoryPath)
  }
}

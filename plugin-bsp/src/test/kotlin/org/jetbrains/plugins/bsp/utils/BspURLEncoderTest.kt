package org.jetbrains.plugins.bsp.utils

import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.magicmetamodel.extensions.toAbsolutePath
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.createTempDirectory

class BspURLEncoderTest {
  @Test
  fun `valid URI should be left as original without further processing`() {
    // given
    val uri = "file:///path/to/my/project/my-project"

    // when
    val encodedUri = BspURLEncoder.encode(uri)

    // then
    encodedUri shouldBe "file:///path/to/my/project/my-project"
  }

  @Test
  fun `malformed URI with space should be encoded properly`() {
    // given
    val uri = "file:///path/to/my/project/my project"

    // when
    val encodedUri = BspURLEncoder.encode(uri)

    // then
    encodedUri shouldBe "file:///path/to/my/project/my%20project"
  }

  @Test
  fun `malformed URI with weird but valid file characters should be encoded properly`() {
    // given
    val uri = "file:///path/to/weird/project/my-project&*&'^@*&^!(*#%\\$>?><"

    // when
    val encodedUri = BspURLEncoder.encode(uri)

    // then
    encodedUri shouldBe "file:///path/to/weird/project/my-project%26*%26%27%5E%40*%26%5E%21%28*%23%25%5C%24%3E%3F%3E%3C"
  }

  @Test
  fun `malformed URI of a valid directory should be fully functional`() {
    // given
    val tempDir = createTempDirectory("my project&*&'^@*&^!(*#%\\\$>?><").also { it.toFile().deleteOnExit() }

    // this malformedURI will have the format of file:///.../my project&*&'^@*&^!(*#%\$>?><.../
    val malformedURI = URLDecoder.decode(tempDir.toUri().toString(), StandardCharsets.UTF_8.name())

    // when and then
    tempDir shouldBe malformedURI.safeCastToURI().toAbsolutePath()
  }
}

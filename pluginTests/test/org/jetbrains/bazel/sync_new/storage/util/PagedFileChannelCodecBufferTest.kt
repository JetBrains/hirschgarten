package org.jetbrains.bazel.sync_new.storage.util

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.sync_new.codec.CodecBufferTest
import org.jetbrains.bazel.sync_new.codec.CodecBufferTestCase
import org.jetbrains.bazel.test.framework.CartesianProduct
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestApplication
internal class PagedFileChannelCodecBufferTest : CodecBufferTest() {

  companion object {
    private val tempFixture = tempPathFixture()
  }

  fun testCases() = listOf(
    object : CodecBufferTestCase<PagedFileChannelCodecBuffer>(name = "normal") {
      override fun createWritableBuffer(): PagedFileChannelCodecBuffer {
        val path = tempFixture.get().resolve("mmfile.dat")
        Files.createDirectories(path.parent)
        val channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)
        return PagedFileChannelCodecBuffer(channel)
      }

      override fun createReadableBuffer(input: PagedFileChannelCodecBuffer): PagedFileChannelCodecBuffer {
        input.position = 0
        return input
      }
    }
  )

  fun testCasesWithBufferSizes() = CartesianProduct.make2d(testCases(), bufferSizes())
    .map { (c1, c2) -> Arguments.of(c1, c2) }

}

package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.JarFileSystem
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

/**
 * A project contains a source-less target `//packaging:bundle`.
 * The target merges the jar of the source module `//lib:leaf` with one generated jar.
 * So its output-jars library holds two jars.
 *
 * The import replaces only the shadowed `leaf` jar with the `leaf` source module.
 * It keeps the generated jar in the library. So `App.java` resolves both symbols.
 * `Leaf` resolves to the source file, and `Extra` resolves from the library jar.
 * See BAZEL-3477.
 **/
@BazelTestApplication
class JavaCustomPackagingTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/java_custom_packaging",
    buildProject = true,
  )

  @Test
  fun `test shadowed library`(): Unit = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("app/App.java")

      // The `leaf` jar is shadowed, so `Leaf` resolves through the source module.
      val psiFile = fixture.configureFromTempProjectFile("app/App.java")
      val offset = psiFile.text.indexOf("Leaf()")
      offset shouldBeGreaterThanOrEqual 0
      readAction {
        val resolved = psiFile.findReferenceAt(offset)?.resolve() ?: error("unresolved Leaf")
        val vf = resolved.containingFile?.virtualFile ?: error("no virtual file for Leaf")
        // Leaf comes from the source module, not from the compiled library jar.
        (vf.fileSystem is JarFileSystem) shouldBe false
        vf.path shouldEndWith "lib/Leaf.java"
      }
    }
  }
}

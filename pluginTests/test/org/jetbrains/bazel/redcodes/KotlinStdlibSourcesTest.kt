package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.psi.PsiFile
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

/**
 * The synthetic Kotlin stdlib library must carry the stdlib source jars, so that navigation into the
 * stdlib lands in Kotlin sources rather than in decompiled class files. See BAZEL-3317.
 */
@BazelTestApplication
class KotlinStdlibSourcesTest {

  private val fixture by bazelSyncCodeInsightFixture("redcodes/kotlin_stdlib_sources")

  @Test
  fun testStdlibSources(): Unit = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("Lib.kt")

      val snapshot = WorkspaceModel.getInstance(fixture.project).currentSnapshot
      val stdlib = snapshot.entities(LibraryEntity::class.java)
        .single { it.name.startsWith("rules_kotlin_kotlin-stdlibs") }
      stdlib.sourceRoots() shouldExist { it.endsWith("/kotlin-stdlib-sources.jar!/") }

      // `joinToString` must navigate into the stdlib sources, not into a decompiled class file
      readAction {
        val target = fixture.file.navigationFileOf("joinToString")
        target.fileSystem.shouldBeInstanceOf<JarFileSystem>()
        target.path shouldContain "/kotlin-stdlib-sources.jar!/"
        target.name shouldBe "_Collections.kt"
      }
    }
  }
}

private fun LibraryEntity.sourceRoots(): List<String> =
  roots.filter { it.type == LibraryRootTypeId.SOURCES }.map { it.url.url }

private fun PsiFile.navigationFileOf(usage: String): VirtualFile {
  val offset = text.lastIndexOf(usage)
  offset shouldBeGreaterThanOrEqual 0
  val resolved = findReferenceAt(offset)?.resolve() ?: error("unresolved reference at '$usage'")
  return resolved.navigationElement.containingFile?.virtualFile ?: error("no virtual file for '$usage'")
}

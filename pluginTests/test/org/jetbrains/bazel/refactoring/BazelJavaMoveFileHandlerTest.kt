package org.jetbrains.bazel.refactoring

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.vfs.readText
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.search.ProjectScope
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.utils.vfs.deleteRecursively
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@TestApplication
internal class BazelJavaMoveFileHandlerTest {
  private val fixture by bazelCodeInsightFixture()

  @Test
  fun testMoveClassToNonIndexableDir() = timeoutRunBlocking {
    val source = "package p; class Main {}"
    fixture.addFileToProject("src/Main.java", source)
    val javaClass =
      readAction { JavaPsiFacade.getInstance(fixture.project).findClass("p.Main", ProjectScope.getAllScope(fixture.project))!! }

    val nonIndexableDir = fixture.tempDirFixture.findOrCreateDir("nonIndexable")
    try {
      val nonIndexablePsi = readAction { PsiManager.getInstance(fixture.project).findDirectory(nonIndexableDir)!! }

      val handler = BazelJavaMoveFileHandler()
      assertTrue(readAction { handler.canMove(arrayOf(javaClass), nonIndexablePsi, null) })
      edtWriteAction { handler.doMove(fixture.project, arrayOf(javaClass), nonIndexablePsi, null) }

      val moved = nonIndexableDir.findChild("Main.java")
      checkNotNull(moved)
      assertEquals(source, moved.readText())
    }
    finally {
      writeAction { nonIndexableDir.deleteRecursively() }
    }
  }
}

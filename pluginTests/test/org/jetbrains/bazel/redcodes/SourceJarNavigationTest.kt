package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class SourceJarNavigationTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testNavigation(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.copyBazelTestProject("redcodes/java_source_jar_navigation")
    fixture.setProjectView(projectview = ".bazelproject")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      val psiFile = fixture.configureByFile("app/App.java")
      fixture.checkHighlighting()
      readAction {
        psiFile.sourceJarEntryOf("JavaLib.value").path shouldEndWith "!/main/com/example/javalib/JavaLib.java"
        psiFile.sourceJarEntryOf("JavaLibVariant.value").path shouldEndWith "!/JavaLibVariant.java"
        psiFile.sourceJarEntryOf("KotlinLib.INSTANCE").path shouldEndWith "!/main/com/example/kotlinlib/KotlinLib.kt"
        psiFile.sourceJarEntryOf("KotlinLibVariant.INSTANCE").path shouldEndWith "!/KotlinLibVariant.kt"
        psiFile.sourceJarEntryOf("KotlinLibWrongFileNameVariant.INSTANCE").path shouldEndWith "!/KotlinLibWrongFileName.kt"
        psiFile.navigationFileOf("ImportedClass.class").name shouldBe "ImportedClass.class"
      }
      val defaultPackagePsiFile = fixture.configureByFile("DefaultPackageApp.java")
      fixture.checkHighlighting()
      readAction {
        defaultPackagePsiFile.sourceJarEntryOf("JavaLibDefaultPackage.value").path shouldEndWith "!/main/JavaLibDefaultPackage.java"
        defaultPackagePsiFile.sourceJarEntryOf("KotlinLibDefaultPackage.INSTANCE").path shouldEndWith "!/main/KotlinLibDefaultPackage.kt"
      }
    }
  }
}

private fun PsiFile.navigationFileOf(usage: String): VirtualFile {
  val offset = text.lastIndexOf(usage)
  offset shouldBeGreaterThanOrEqual 0
  val resolved = findReferenceAt(offset)?.resolve() ?: error("unresolved reference at '$usage'")
  return resolved.navigationElement.containingFile?.virtualFile ?: error("no virtual file for '$usage'")
}

private fun PsiFile.sourceJarEntryOf(usage: String): VirtualFile {
  val file = navigationFileOf(usage)
  file.fileSystem.shouldBeInstanceOf<JarFileSystem>()
  return file
}

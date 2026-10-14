package org.jetbrains.bazel.test.framework

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject

/**
 * Creates a [CodeInsightTestFixture] in a new opened Bazel project. This is the JUnit 5 replacement of `BazelBasePlatformTestCase`.
 *
 * The project has one module. A new temporary directory is the content root and the source root of the module,
 * and the temporary directory of the fixture. The fixture marks the project as Bazel with this directory as the workspace root.
 * It excludes the `.bazelbsp` directory of the workspace root, as a synced Bazel project does.
 *
 * [CodeInsightTestFixture.configureByText] creates the file with the exact name that the test gives, as a light test does.
 * Bazel finds a file such as `BUILD` or `MODULE.bazel` by its name.
 *
 * Declare the result as an instance property of a class with [BazelTestApplication], so that each test gets a new project.
 * The fixture sets up from a background thread.
 */
@TestOnly
fun bazelCodeInsightFixture(): TestFixture<CodeInsightTestFixture> = testFixture("bazelCodeInsightFixture") {
  val rootDirFixture = tempPathFixture()
  val projectFixture = projectFixture(openAfterCreation = true)
  projectFixture.moduleFixture(rootDirFixture, addPathToSourceRoot = true).init()
  val codeInsightFixture = codeInsightFixture(projectFixture, rootDirFixture, ::BazelCodeInsightTestFixture).init()
  initializeBazelProject(codeInsightFixture.project, rootDirFixture.init())
  // initializeBazelProject writes the project view file to this directory. A synced Bazel project excludes it.
  codeInsightFixture.findFileInTempDir(".bazelbsp")?.let { PsiTestUtil.addExcludedRoot(codeInsightFixture.module, it) }
  initialized(codeInsightFixture) {}
}

@TestOnly
private class BazelCodeInsightTestFixture(
  projectFixture: IdeaProjectTestFixture,
  tempDirFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirFixture) {
  // The JUnit 5 project does not close on tearDown, so the pointers can stay alive here. TestApplication checks the leaks.
  override fun shouldTrackVirtualFilePointers(): Boolean = false

  // The base class gives the file a unique name in the temporary directory.
  override fun createFile(fileName: String, text: String): VirtualFile {
    // As the base class does, save the cached document before the write. Otherwise a second write to the same file gives a memory-disk conflict.
    val document = tempDirFixture.getFile(fileName)?.let { FileDocumentManager.getInstance().getCachedDocument(it) }
    if (document != null) {
      WriteCommandAction.runWriteCommandAction(project) {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        FileDocumentManager.getInstance().saveDocument(document)
      }
    }
    return addFileToProject(fileName, text).virtualFile
  }
}

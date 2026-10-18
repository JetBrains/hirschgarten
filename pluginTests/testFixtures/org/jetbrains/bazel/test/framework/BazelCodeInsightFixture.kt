package org.jetbrains.bazel.test.framework

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.framework.testproject.TestBazelProject
import org.jetbrains.bazel.test.framework.testproject.installedProjectFixture
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRootFixture

/**
 * Creates a [CodeInsightTestFixture] in a new test Bazel project. This is the JUnit 5 replacement of `BazelBasePlatformTestCase`.
 * Get the test Bazel project with [testBazel].
 *
 * The project is open and has one module. The workspace root is the content root and the source root of the module,
 * and the temporary directory of the fixture.
 *
 * [CodeInsightTestFixture.configureByText] creates the file with the exact name that the test gives, as a light test does.
 * Bazel finds a file such as `BUILD` or `MODULE.bazel` by its name.
 *
 * Declare the result as an instance property of a class with [BazelTestApplication], so that each test gets a new project.
 * The fixture sets up from a background thread.
 */
@TestOnly
fun bazelCodeInsightFixture(): TestFixture<CodeInsightTestFixture> =
  testBazelProjectFixture().codeInsightFixtureOnTestBazel()

/** Returns the test Bazel project of a fixture from [bazelCodeInsightFixture]. Fails for any other fixture. */
val CodeInsightTestFixture.testBazel: TestBazelProject
  get() = (this as? BazelCodeInsightTestFixture)?.testBazel ?: error("Create the fixture with bazelCodeInsightFixture()")

/**
 * Runs the basic completion and returns the lookup strings of all items. Set [dumbMode] to run the completion in dumb mode.
 * Fails when the completion inserts the only item, because then there is no item list.
 */
fun CodeInsightTestFixture.completeBasicLookupStrings(dumbMode: Boolean = false): List<String> {
  if (dumbMode) {
    return DumbModeTestUtils.computeInDumbModeSynchronously(project) { completeBasicLookupStrings() }
  }
  val items = completeBasic() ?: error("The completion inserted the only item, so it has no item list")
  return items.flatMap { it.allLookupStrings }
}

private fun TestFixture<TestBazelProject>.codeInsightFixtureOnTestBazel(): TestFixture<CodeInsightTestFixture> =
  testFixture("bazelCodeInsightFixture") {
    val testBazel = this@codeInsightFixtureOnTestBazel.init()
    val rootDirFixture = workspaceRootFixture()
    val projectFixture = installedProjectFixture()
    projectFixture.moduleFixture(rootDirFixture, addPathToSourceRoot = true).init()
    val codeInsightFixture = codeInsightFixture(projectFixture, rootDirFixture) { project, tempDir ->
      BazelCodeInsightTestFixture(project, tempDir, testBazel)
    }.init()
    initialized(codeInsightFixture) {}
  }

@TestOnly
private class BazelCodeInsightTestFixture(
  projectFixture: IdeaProjectTestFixture,
  tempDirFixture: TempDirTestFixture,
  val testBazel: TestBazelProject,
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

  // A JUnit 5 test runs off the EDT. The editor needs the EDT.
  override fun openFileInEditor(file: VirtualFile) {
    runInEdtAndWait { super.openFileInEditor(file) }
  }
}

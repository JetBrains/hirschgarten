package org.jetbrains.bazel.python.completion

import com.intellij.bazel.python.backend.updateBazelPythonResolveIndex
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.QualifiedName
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import java.nio.file.Path

class BazelPythonImportCompletionTest : BazelBasePlatformTestCase() {

  override fun getProjectDescriptor(): LightProjectDescriptor? = DefaultLightProjectDescriptor()

  override fun createTempDirTestFixture(): TempDirTestFixture = TempDirTestFixtureImpl()

  private lateinit var lib: Path
  private lateinit var things: Path
  private lateinit var widgets: Path
  private lateinit var libExtra: Path

  private fun setUpLibIndex() {
    val root = project.rootDir
    WriteCommandAction.runWriteCommandAction(project) {
      val libDir = root.createChildDirectory(this, "lib")
      val thingsDir = libDir.createChildDirectory(this, "things")
      val places = thingsDir.createChildDirectory(this, "places")
      places.createChildData(this, "rivers.py")
      lib = libDir.toNioPath()
      things = thingsDir.toNioPath()
      val widgetsDir = libDir.createChildDirectory(this, "widgets")
      widgetsDir.createChildData(this, "models.py")
      widgets = widgetsDir.toNioPath()
      // A second "li"-prefixed name so "li<caret>" isn't auto-inserted as the sole match.
      val libExtraDir = root.createChildDirectory(this, "libExtra")
      libExtra = libExtraDir.toNioPath()
    }
    project.updateBazelPythonResolveIndex(
      mapOf(
        QualifiedName.fromDottedString("lib") to lib,
        QualifiedName.fromDottedString("lib.things") to things,
        QualifiedName.fromDottedString("lib.widgets") to widgets,
        QualifiedName.fromDottedString("libExtra") to libExtra,
      ),
    )
  }

  fun testShouldCompleteIndexedChildOfImportSourceWithoutBazelImports() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from lib.<caret>")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  fun testShouldCompleteTopLevelQualifiedNameInFromImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from li<caret>")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "lib"
    lookups shouldContain "libExtra"
  }

  fun testShouldCompleteTopLevelQualifiedNameInPlainImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import li<caret>")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "lib"
    lookups shouldContain "libExtra"
  }

  fun testShouldCompleteIndexedChildOfImportSourceInPlainImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import lib.<caret>")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  fun testShouldCompleteChildOfImportSourceWithTrailingImportKeyword() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from lib.<caret> import x")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  fun testShouldCompleteChildOfImportSourceInPlainImportWithTrailingCode() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import lib.<caret>\nx = 1\n")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }
}

package org.jetbrains.bazel.python.completion

import com.intellij.bazel.python.backend.updateBazelPythonResolveIndex
import com.intellij.psi.util.QualifiedName
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.createDirectory
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelPythonImportCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project
  private val testBazel get() = myFixture.testBazel

  private fun setUpLibIndex() {
    testBazel.writeFile("lib/things/places/rivers.py")
    testBazel.writeFile("lib/widgets/models.py")
    project.updateBazelPythonResolveIndex(
      mapOf(
        QualifiedName.fromDottedString("lib") to testBazel.createDirectory("lib").toNioPath(),
        QualifiedName.fromDottedString("lib.things") to testBazel.createDirectory("lib/things").toNioPath(),
        QualifiedName.fromDottedString("lib.widgets") to testBazel.createDirectory("lib/widgets").toNioPath(),
        // A second "li"-prefixed name so "li<caret>" isn't auto-inserted as the sole match.
        QualifiedName.fromDottedString("libExtra") to testBazel.createDirectory("libExtra").toNioPath(),
      ),
    )
  }

  @Test
  fun testShouldCompleteIndexedChildOfImportSourceWithoutBazelImports() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from lib.<caret>")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  @Test
  fun testShouldCompleteTopLevelQualifiedNameInFromImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from li<caret>")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "lib"
    lookups shouldContain "libExtra"
  }

  @Test
  fun testShouldCompleteTopLevelQualifiedNameInPlainImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import li<caret>")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "lib"
    lookups shouldContain "libExtra"
  }

  @Test
  fun testShouldCompleteIndexedChildOfImportSourceInPlainImport() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import lib.<caret>")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  @Test
  fun testShouldCompleteChildOfImportSourceWithTrailingImportKeyword() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "from lib.<caret> import x")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }

  @Test
  fun testShouldCompleteChildOfImportSourceInPlainImportWithTrailingCode() {
    setUpLibIndex()

    myFixture.configureByText("main.py", "import lib.<caret>\nx = 1\n")
    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContain "things"
    lookups shouldContain "widgets"
    lookups shouldNotContain "places"
  }
}

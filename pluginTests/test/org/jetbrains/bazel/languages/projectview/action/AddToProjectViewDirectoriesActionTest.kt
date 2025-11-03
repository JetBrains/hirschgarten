package org.jetbrains.bazel.languages.projectview.action

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.ExcludableValue.Companion.included
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.languages.projectview.targets
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class AddToProjectViewDirectoriesActionTest : ProjectViewDirectoriesActionTestCase("Bazel.AddToProjectViewDirectoriesAction") {

  @Test
  fun `test presentation template is correct`() {
    val presentation = action.templatePresentation
    presentation.text shouldBe "Add to {0} Directories Section"
    presentation.description shouldBe "Add this directory to the project view file."
    presentation.icon shouldBe null
  }

  @Test
  fun `test presentation is disabled when no file in context`() {
    useAndOpenProjectView("""
      directories: .
    """.trimIndent())
    val presentation = testPresentationOn(createActionContext())
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when file is not a directory`() {
    useAndOpenProjectView("""
      directories: .
    """.trimIndent())
    val presentation = testPresentationOn(createActionContext(myFixture.createFile("foo", "file")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when file is a directory that is included directly`() {
    useAndOpenProjectView(
      """
      directories:
        foo
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled with when file is a directory that is included transitively`() {
    useAndOpenProjectView(
      """
      directories:
        .
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is enabled with when file is a directory that is not included`() {
    useAndOpenProjectView(
      """
      directories:
        bar
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Add to .user.bazelproject Directories Section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test presentation is enabled with when file is a directory that excluded directly`() {
    useAndOpenProjectView(
      """
      directories:
        -foo
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Add to .user.bazelproject Directories Section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test presentation is enabled with when file is a directory that excluded transitively`() {
    useAndOpenProjectView(
      """
      directories:
        -.
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Add to .user.bazelproject Directories Section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test includes given directory when not included and single other directory included`() {
    useAndOpenProjectView(
      """
      directories: 
        bar
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path("bar")),
      included(Path("foo")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test includes given directory when not included and multiple other directories included`() {
    useAndOpenProjectView(
      """
      directories: buzz
        bar
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path("buzz")),
      included(Path("bar")),
      included(Path("foo")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test includes given directory when not included and directories section is empty`() {
    useAndOpenProjectView("""
      directories:
      
    """.trimIndent())
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path("foo")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test includes given directory when not included and directories section not present`() {
    useAndOpenProjectView("""
      targets:
        //foo
        //bar
    """.trimIndent())
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path("foo")),
    )
    bazelProjectView.targets shouldHaveSize 2
    notifications shouldHaveSize 0
  }


  @Test
  fun `test does not change directories when given directory directly included`() {
    useAndOpenProjectView("""
      directories: .
        bar
    """.trimIndent())
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("bar")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test does not change directories when given directory transitively included`() {
    useAndOpenProjectView(
      """
      directories: .
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test removes excluded directory when transitively included and explicitly excluded`() {
    useAndOpenProjectView(
      """
      directories: -bar
        .
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test removes excluded directory duplicates when transitively included and explicitly excluded`() {
    useAndOpenProjectView(
      """
      directories: .
        -bar
        -bar
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test removes excluded directory and adds included when explicitly excluded and not included in any way`() {
    useAndOpenProjectView(
      """
      directories: buzz
        -bar
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path("buzz")),
      included(Path("bar")),
    )
    notifications shouldHaveSize 0
  }
}

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
    useProjectView(
      """
      directories: .
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext())
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when file is not a directory`() {
    useProjectView(
      """
      directories: .
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.createFile("foo", "file")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when file is a directory that is included directly`() {
    useProjectView(
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
    useProjectView(
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
    useProjectView(
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
    useProjectView(
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
    useProjectView(
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
    useProjectView(
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
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test includes given directory when not included and multiple other directories included`() {
    useProjectView(
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
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test includes given directory and root when directories section is empty`() {
    useProjectView(
      """
      directories:
      
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("foo")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test includes given directory and root when directories section not present`() {
    useProjectView(
      """
      targets:
        //foo
        //bar
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("foo")),
    )
    bazelProjectView.targets shouldHaveSize 2
    assertTrue(isProjectViewOpenedInEditor())
  }


  @Test
  fun `test does not change directories when given directory directly included`() {
    useProjectView(
      """
      directories: .
        bar
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("bar")),
    )
    assertFalse(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test does not change directories when given directory transitively included`() {
    useProjectView(
      """
      directories: .
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
    )
    assertFalse(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes excluded directory when transitively included and explicitly excluded`() {
    useProjectView(
      """
      directories: -bar
        .
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes excluded directory duplicates when transitively included and explicitly excluded`() {
    useProjectView(
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
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes excluded directory and adds included when explicitly excluded and not included in any way`() {
    useProjectView(
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
    assertTrue(isProjectViewOpenedInEditor())
  }
}

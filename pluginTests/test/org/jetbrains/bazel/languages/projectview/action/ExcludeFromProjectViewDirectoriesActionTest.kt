package org.jetbrains.bazel.languages.projectview.action

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.ExcludableValue.Companion.excluded
import org.jetbrains.bazel.commons.ExcludableValue.Companion.included
import org.jetbrains.bazel.languages.projectview.directories
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class ExcludeFromProjectViewDirectoriesActionTest : ProjectViewDirectoriesActionTestCase("Bazel.ExcludeFromProjectViewDirectoriesAction") {

  @Test
  fun `test presentation template is correct`() {
    val presentation = action.templatePresentation
    presentation.text shouldBe "Exclude from {0} Directories Section"
    presentation.description shouldBe "Exclude this directory from the project view file."
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
  fun `test presentation is disabled when file is a directory that is transitively excluded`() {
    useProjectView(
      """
      directories: 
        -foo
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo/bar")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when file is a directory that is directly excluded`() {
    useProjectView(
      """
      directories: 
        -foo
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is disabled when directories section is missing`() {
    useProjectView("")
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe false
  }

  @Test
  fun `test presentation is enabled when file is a directory that is transitively included`() {
    useProjectView(
      """
      directories: 
        .
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Exclude from .user.bazelproject Directories Section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test presentation is enabled when file is a directory that is directly included`() {
    useProjectView(
      """
      directories: 
        foo
    """.trimIndent(),
    )
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Exclude from .user.bazelproject Directories Section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test excludes given directory when not excluded and transitively included`() {
    useProjectView(
      """
      directories: .
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      excluded(Path("foo")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test excludes directory when not excluded and transitively included and other dir included`() {
    useProjectView(
      """
      directories: .
        foo
        bar
    """.trimIndent(),
    )
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("bar")),
      excluded(Path("foo")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes directory if it's included and not transitively included`() {
    useProjectView(
      """
       directories: bar
        foo
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path("foo")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes directory duplicates if it's included and not transitively included`() {
    useProjectView(
      """
       directories: foo
        bar
        bar
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path("foo")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test removes include and adds exclude if directory is included directly and transitively`() {
    useProjectView(
      """
       directories: foo
        foo/bar
    """.trimIndent(),
    )
    performActionOnProjectDir("foo/bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path("foo")),
      excluded(Path("foo/bar")),
    )
    assertTrue(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test does not change directories when given directory already excluded directly`() {
    useProjectView(
      """
       directories: .
        -bar     
    """.trimIndent(),
    )
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      excluded(Path("bar")),
    )
    assertFalse(isProjectViewOpenedInEditor())
  }

  @Test
  fun `test does not change directories when given directory already excluded transitively`() {
    useProjectView(
      """
       directories: .
        -foo
    """.trimIndent(),
    )
    performActionOnProjectDir("foo/bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      excluded(Path("foo")),
    )
    assertFalse(isProjectViewOpenedInEditor())
  }
}

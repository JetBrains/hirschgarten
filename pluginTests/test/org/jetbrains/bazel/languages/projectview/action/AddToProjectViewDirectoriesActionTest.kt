package org.jetbrains.bazel.languages.projectview.action

import com.intellij.notification.NotificationType
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.action.registered.projectViewDirectories.ProjectViewDirectoriesAction
import org.jetbrains.bazel.commons.ExcludableValue.Companion.excluded
import org.jetbrains.bazel.commons.ExcludableValue.Companion.included
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.languages.projectview.targets
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.io.path.Path

@RunWith(JUnit4::class)
class AddToProjectViewDirectoriesActionTest : ProjectViewDirectoriesActionTestCase("Bazel.AddToProjectViewDirectoriesAction") {

  @Before
  fun setup() {
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")
  }

  @Test
  fun `test presentation template is correct`() {
    val presentation = action.templatePresentation
    presentation.text shouldBe "Add to {0} directories section"
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
  fun `test presentation is enabled with when file is a directory`() {
    useAndOpenProjectView("""
      directories: .
    """.trimIndent())
    val presentation = testPresentationOn(createActionContext(myFixture.tempDirFixture.findOrCreateDir("foo")))
    presentation.isEnabledAndVisible shouldBe true
    presentation.text shouldBe "Add to .user.bazelproject directories section"
    presentation.description shouldBe null
    presentation.icon shouldBe null
  }

  @Test
  fun `test includes given directory when not included and single other directory included`() {
    useAndOpenProjectView("""
      directories: .
    """.trimIndent())
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("foo")),
    )
    notifications shouldHaveSize 0
  }

  @Test
  fun `test includes given directory when not included and multiple other directories included`() {
    useAndOpenProjectView("""
      directories: .
        bar
    """.trimIndent())
    performActionOnProjectDir("foo")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
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
  fun `test does not change directories and shows information when given directory included`() {
    useAndOpenProjectView("""
      directories: .
        bar
    """.trimIndent())
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      included(Path("bar")),
    )
    notifications shouldHaveSingleElement {
      it.groupId == ProjectViewDirectoriesAction.NotificationFactory.NOTIFICATION_GROUP_ID &&
        it.type == NotificationType.INFORMATION &&
        it.title == "Directory already included" &&
        it.content == "Directory 'bar' is already included in the project view file directories section."
    }
  }

  @Test
  fun `test does not change directories and shows warning when given directory is excluded`() {
    useAndOpenProjectView("""
      directories: .
        -bar
    """.trimIndent())
    performActionOnProjectDir("bar")
    bazelProjectView.directories shouldBe listOf(
      included(Path(".")),
      excluded(Path("bar")),
    )
    notifications shouldHaveSingleElement {
      it.groupId == ProjectViewDirectoriesAction.NotificationFactory.NOTIFICATION_GROUP_ID &&
        it.type == NotificationType.WARNING &&
        it.title == "Including excluded directory" &&
        it.content == "Attempted to include directory 'bar' which has been already excluded from the project view file directories section."
    }
  }
}

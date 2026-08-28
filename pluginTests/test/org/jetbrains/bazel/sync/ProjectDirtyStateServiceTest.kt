package org.jetbrains.bazel.sync

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class ProjectDirtyStateServiceTest : MockProjectBaseTest() {
  @Test
  fun `a new service is clean`() {
    val service = service()

    service.isDirty().shouldBeFalse()
    service.current() shouldBe ProjectDirtyState.EMPTY
  }

  @Test
  fun `markDirty adds the paths`() {
    val service = service()

    service.markDirty(listOf(path("a"), path("b"))).shouldBeTrue()

    service.isDirty().shouldBeTrue()
    service.current().paths shouldContainExactlyInAnyOrder listOf(path("a"), path("b"))
    service.current().wholeProject.shouldBeFalse()
  }

  @Test
  fun `markDirty with the same path twice reports no change`() {
    val service = service()

    service.markDirty(listOf(path("a"))).shouldBeTrue()
    service.markDirty(listOf(path("a"))).shouldBeFalse()
  }

  @Test
  fun `markDirty with an empty list reports no change`() {
    service().markDirty(emptyList()).shouldBeFalse()
  }

  @Test
  fun `markWholeProjectDirty replaces the paths`() {
    val service = service()
    service.markDirty(listOf(path("a")))

    service.markWholeProjectDirty("test").shouldBeTrue()

    service.current().wholeProject.shouldBeTrue()
    service.current().paths.shouldBeEmpty()
    service.isDirty().shouldBeTrue()
    service.markWholeProjectDirty("test").shouldBeFalse()
  }

  @Test
  fun `markDirty does nothing once the whole project is dirty`() {
    val service = service()
    service.markWholeProjectDirty("test")

    service.markDirty(listOf(path("a"))).shouldBeFalse()

    service.current().paths.shouldBeEmpty()
  }

  @Test
  fun `too many paths mark the whole project`() {
    val service = service()

    service.markDirty((0..5_000).map { path("pkg$it") }).shouldBeTrue()

    service.current().wholeProject.shouldBeTrue()
    service.current().paths.shouldBeEmpty()
  }
  
  @Test
  fun `the state flow reports the current value`() {
    val service = service()

    service.state.value shouldBe ProjectDirtyState.EMPTY
    service.markDirty(listOf(path("a")))
    service.state.value.paths shouldContainExactlyInAnyOrder listOf(path("a"))
  }

  @Test
  fun `clearAll resets the state`() {
    val service = service()
    service.markWholeProjectDirty("test")

    service.clearAll()

    service.isDirty().shouldBeFalse()
  }

  private fun service(): ProjectDirtyStateService = ProjectDirtyStateService.getInstance(project)

  private fun path(relative: String): Path = projectDir.get().resolve(relative)
}

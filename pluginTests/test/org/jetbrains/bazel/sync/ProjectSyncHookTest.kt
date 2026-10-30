package org.jetbrains.bazel.sync

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@BazelTestApplication
@DisplayName("ProjectSyncHook tests")
class ProjectSyncHookTest {
  private val project by projectFixture(openAfterCreation = false)

  @TestDisposable
  lateinit var disposable: Disposable

  @Test
  fun `should return all enabled project sync hooks`() {
    // given
    ProjectSyncHook.ep.point.registerExtension(TestProjectSyncHook(), disposable)
    ProjectSyncHook.ep.point.registerExtension(DisabledTestProjectSyncHook(), disposable)

    // when & then
    project.projectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
    project.projectSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectSyncHook::class.java
  }
}

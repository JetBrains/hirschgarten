package org.jetbrains.bazel.sync

import com.intellij.openapi.Disposable
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@BazelTestApplication
@DisplayName("ProjectPreSyncHook tests")
class ProjectPreSyncHookTest {
  private val project by projectFixture(openAfterCreation = false)

  @TestDisposable
  lateinit var disposable: Disposable

  @Test
  fun `should return all enabled project pre-sync hooks`() {
    // given
    ProjectPreSyncHook.ep.point.registerExtension(TestProjectPreSyncHook(), disposable)
    ProjectPreSyncHook.ep.point.registerExtension(DisabledTestProjectPreSyncHook(), disposable)

    // when & then
    project.projectPreSyncHooks.map { it::class.java } shouldContain TestProjectPreSyncHook::class.java
    project.projectPreSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectPreSyncHook::class.java
  }
}

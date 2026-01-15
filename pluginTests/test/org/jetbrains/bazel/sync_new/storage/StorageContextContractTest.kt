package org.jetbrains.bazel.sync_new.storage

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.runBlocking

internal abstract class StorageContextContractTest {
  companion object {
    private val tempDir = tempPathFixture()
    private val project = projectFixture(tempDir, openAfterCreation = true)
  }

  protected abstract fun createStorageContext(project: Project, disposable: Disposable): StorageContext

  protected fun <S> openContext(builder: StorageContext.() -> S, block: S.() -> Unit) {
    val disposable = Disposer.newDisposable()
    val ctx = createStorageContext(project = project.get(), disposable = disposable)
    runBlocking {
      withBackgroundProgress(project.get(), "Test") {
        val store = builder(ctx)
        block(store)
        Disposer.dispose(disposable)
      }
    }
  }

}

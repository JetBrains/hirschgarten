package org.jetbrains.bazel.test.framework

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.TestFixtures
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.test.compat.PluginTestsCompat
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

@TestOnly
@Target(AnnotationTarget.CLASS)
@ExtendWith(DisableVfsAccessChecksExtension::class, BazelIdeaTextExtension::class)
@TestApplication
@TestFixtures
annotation class BazelTestApplication

private class DisableVfsAccessChecksExtension : BeforeAllCallback, AfterAllCallback {
  private var oldPropertyValue: String? = null

  override fun beforeAll(context: ExtensionContext) {
    oldPropertyValue = System.getProperty("NO_FS_ROOTS_ACCESS_CHECK")
    System.setProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
  }

  override fun afterAll(context: ExtensionContext) {
    if (oldPropertyValue != null) {
      System.setProperty("NO_FS_ROOTS_ACCESS_CHECK", oldPropertyValue!!)
    }
    else {
      System.clearProperty("NO_FS_ROOTS_ACCESS_CHECK")
    }
  }
}

private class BazelIdeaTextExtension : BeforeAllCallback, AfterAllCallback {
  val disposable = Disposer.newDisposable()

  override fun beforeAll(context: ExtensionContext?) {
    PluginTestsCompat.setupTestSuite(disposable)
  }

  override fun afterAll(context: ExtensionContext?) {
    Disposer.dispose(disposable)
  }
}


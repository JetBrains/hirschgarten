package org.jetbrains.bazel.test.framework

import com.intellij.openapi.Disposable
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
@ExtendWith(TestStateLeakGuardExtension::class, DisableVfsAccessChecksExtension::class, BazelIdeaTextExtension::class)
@TestApplication
@TestFixtures
annotation class BazelTestApplication

// JUnit calls one extension instance for a class and for its @Nested classes,
// so the extensions keep their state for each class context, not in a field.

private class DisableVfsAccessChecksExtension : BeforeAllCallback, AfterAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    context.getStore(NAMESPACE).put(context.uniqueId, OldValue(System.getProperty(PROPERTY)))
    System.setProperty(PROPERTY, "true")
  }

  override fun afterAll(context: ExtensionContext) {
    val oldValue = context.getStore(NAMESPACE).remove(context.uniqueId, OldValue::class.java) ?: return
    if (oldValue.value != null) {
      System.setProperty(PROPERTY, oldValue.value)
    }
    else {
      System.clearProperty(PROPERTY)
    }
  }

  private class OldValue(val value: String?)

  private companion object {
    const val PROPERTY = "NO_FS_ROOTS_ACCESS_CHECK"
    val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(DisableVfsAccessChecksExtension::class.java)
  }
}

private class BazelIdeaTextExtension : BeforeAllCallback, AfterAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    val disposable = Disposer.newDisposable()
    context.getStore(NAMESPACE).put(context.uniqueId, disposable)
    PluginTestsCompat.setupTestSuite(disposable)
  }

  override fun afterAll(context: ExtensionContext) {
    context.getStore(NAMESPACE).remove(context.uniqueId, Disposable::class.java)?.let(Disposer::dispose)
  }

  private companion object {
    val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(BazelIdeaTextExtension::class.java)
  }
}

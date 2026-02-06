package org.jetbrains.bazel.test.framework

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

@TestOnly
@Target(AnnotationTarget.CLASS)
@ExtendWith(DisableVfsAccessChecksExtension::class)
@TestApplication
annotation class BazelTestApplication

private class DisableVfsAccessChecksExtension : BeforeAllCallback {
  override fun beforeAll(context: ExtensionContext) {
    System.setProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
  }
}

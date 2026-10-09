package org.jetbrains.bazel.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.project.BazelProjectFixtures.restoreProjectStoreOnDispose
import java.nio.file.Files

abstract class BazelBasePlatformTestCase : BasePlatformTestCase() {
  private lateinit var disposable: CheckedDisposable
  private var leakGuard: TestStateLeakGuard? = null

  override fun setUp() {
    disposable = Disposer.newCheckedDisposable()
    super.setUp()
    leakGuard = TestStateLeakGuard.capture(listOf(SystemPropertiesProbe, LightProjectBazelStateProbe(project)))

    // The light project stays open for the next tests.
    restoreProjectStoreOnDispose(project, disposable)
    val rootDir = myFixture.tempDirPath.toNioPathOrNull()
    initializeBazelProject(project, rootDir ?: Files.createTempDirectory("bazel-test-").also { tmpDir ->
      Disposer.register(disposable, Disposable {
        NioFiles.deleteRecursively(tmpDir)
      })
    })
  }

  override fun tearDown() {
    // super.tearDown() clears the fields of the test, so keep the guard in a local.
    val leakGuard = leakGuard
    val testName = name
    try {
      Disposer.dispose(disposable)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
    leakGuard?.assertNothingLeaked(testName)
  }

  fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}

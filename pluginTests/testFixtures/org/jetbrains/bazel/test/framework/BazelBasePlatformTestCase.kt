package org.jetbrains.bazel.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import java.nio.file.Files

abstract class BazelBasePlatformTestCase : BasePlatformTestCase() {
  private lateinit var disposable: CheckedDisposable

  override fun setUp() {
    disposable = Disposer.newCheckedDisposable()
    super.setUp()

    val rootDir = myFixture.tempDirPath.toNioPathOrNull()
    initializeBazelProject(project, rootDir ?: Files.createTempDirectory("bazel-test-").also { tmpDir ->
      Disposer.register(disposable, Disposable {
        NioFiles.deleteRecursively(tmpDir)
      })
    })
  }

  override fun tearDown() {
    Disposer.dispose(disposable)
    super.tearDown()
  }

  fun <T : Any> ExtensionPointName<T>.registerExtension(extension: T) {
    point.registerExtension(extension, disposable)
  }
}

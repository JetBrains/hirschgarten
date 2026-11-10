package org.jetbrains.bazel.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.replaceService
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import org.jetbrains.bazel.sync.environment.BazeliskDownload

/**
 * Forces the download of the [BazeliskDownload.GitHub] release through the JetBrains cache redirector.
 * The file keeps the pinned checksum of the release.
 */
private object CacheRedirectorBazeliskDownload : BazeliskDownload {
  private const val CACHE_REDIRECTOR_URL = "https://cache-redirector.jetbrains.com/"

  override fun bazeliskFile(os: OS, arch: CpuArch): BazeliskDownload.BazeliskFile? {
    val file = BazeliskDownload.GitHub.bazeliskFile(os, arch) ?: return null
    return file.copy(url = CACHE_REDIRECTOR_URL + file.url.removePrefix("https://"))
  }
}

/** Keeps the settings of [delegate], and forces the Bazelisk download of [CacheRedirectorBazeliskDownload]. */
private class TestBazelApplicationContextService(
  private val delegate: BazelApplicationContextService,
) : BazelApplicationContextService by delegate {
  override val forcedBazeliskDownload: BazeliskDownload
    get() = CacheRedirectorBazeliskDownload
}

/**
 * Makes each sync run a Bazelisk from the JetBrains cache redirector until [disposable] is disposed.
 *
 * So a sync does not look for Bazel on `PATH`, and it does not download Bazelisk from GitHub.
 */
internal fun installTestBazeliskDownload(disposable: Disposable) {
  val application = ApplicationManager.getApplication()
  val delegate = application.service<BazelApplicationContextService>()
  application.replaceService(BazelApplicationContextService::class.java, TestBazelApplicationContextService(delegate), disposable)
}

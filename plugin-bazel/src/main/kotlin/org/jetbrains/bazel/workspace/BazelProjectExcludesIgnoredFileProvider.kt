package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor
import com.intellij.openapi.vcs.changes.IgnoredFileProvider
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject

/**
 * This class's mission is to re-add modal `Add file to Git` for Bazel projects, which was disabled due to the issue explained in the kdoc of [DummyModuleExclusionWorkspaceFileIndexContributor]
 */
internal class BazelProjectExcludesIgnoredFileProvider : IgnoredFileProvider {
  private val delegate = getProjectExcludesIgnoredFileProviderClass()?.getConstructor()?.newInstance()

  override fun isIgnoredFile(project: Project, filePath: FilePath): Boolean {
    if (project.isBazelProject) return false
    return delegate?.isIgnoredFile(project, filePath) ?: false
  }

  override fun getIgnoredFiles(project: Project): Set<IgnoredFileDescriptor?> {
    if (project.isBazelProject) return emptySet()
    return delegate?.getIgnoredFiles(project) ?: emptySet()
  }

  override fun getIgnoredGroupDescription(): @NlsContexts.DetailedDescription String =
    BazelPluginBundle.message("text.bazel.ignored.group.description")
}

internal fun getProjectExcludesIgnoredFileProviderClass(): Class<IgnoredFileProvider>? =
  Class.forName("com.intellij.openapi.vcs.changes.ProjectExcludesIgnoredFileProvider") as? Class<IgnoredFileProvider>

fun unregisterProjectExcludesIgnoredFileProvider() {
  if (BazelFeatureFlags.fbsrSupportedInPlatform) return
  val clazz = getProjectExcludesIgnoredFileProviderClass()
  clazz?.also { IgnoredFileProvider.IGNORE_FILE.point.unregisterExtension(clazz) }
}

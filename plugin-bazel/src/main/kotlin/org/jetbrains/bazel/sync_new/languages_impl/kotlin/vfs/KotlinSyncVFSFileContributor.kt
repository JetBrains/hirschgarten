package org.jetbrains.bazel.sync_new.languages_impl.kotlin.vfs

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFileContributor
import java.nio.file.Path
import kotlin.io.path.extension

class KotlinSyncVFSFileContributor : SyncVFSFileContributor {
  override fun getWatchableFileExtensions(project: Project): List<String> = listOf("kt")

  override fun doesFileChangeInvalidateTarget(project: Project, file: Path): Boolean = file.extension == "kt"
}

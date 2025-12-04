package org.jetbrains.bazel.sync_new.languages_impl.java

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFileContributor
import java.nio.file.Path
import kotlin.io.path.extension

class JavaSyncVFSFileContributor : SyncVFSFileContributor {
  override fun getWatchableFileExtensions(project: Project): List<String> = listOf("java")

  override fun doesFileChangeInvalidateTarget(project: Project, file: Path): Boolean = file.extension == "java"
}

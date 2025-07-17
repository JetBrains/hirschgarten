package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.startup.utils.BazelProjectActivity
import org.jetbrains.bazel.sync.status.SyncStatusListener
import kotlin.io.path.relativeToOrNull

@Service(Service.Level.PROJECT)
class BazelBzlFileService(private val project: Project) {
  @Volatile
  var canonicalRepoNameToBzlFiles: Map<String, List<ResolvedLabel>> = emptyMap()

  init {
    BazelCoroutineService.getInstance(project).start { updateCache() }

    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {}

        override fun syncFinished(canceled: Boolean) {
          if (canceled) return
          BazelCoroutineService.getInstance(project).start { updateCache() }
        }
      },
    )
  }

  private fun updateCache() {
    val newMap = mutableMapOf<String, List<ResolvedLabel>>()
    val projectFileIndex = ProjectFileIndex.getInstance(project)

    val canonicalRepoPaths =
      project.canonicalRepoNameToPath.values
        .mapNotNull {
          VirtualFileManager.getInstance().findFileByNioPath(it)
        }.toSet()

    for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
      val root = VirtualFileManager.getInstance().findFileByNioPath(repoPath) ?: continue
      VfsUtilCore.visitChildrenRecursively(
        root,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (runReadAction { projectFileIndex.isExcluded(file) }) return SKIP_CHILDREN
            if (file != root && file in canonicalRepoPaths) return SKIP_CHILDREN
            if (file.isDirectory || file.extension != "bzl") return CONTINUE

            val targetName = file.name
            val targetBaseDirectory = file.parent ?: return CONTINUE
            val relativeTargetBaseDirectory = targetBaseDirectory.toNioPath().relativeToOrNull(repoPath) ?: return CONTINUE

            val label =
              ResolvedLabel(
                repo = Canonical.createCanonicalOrMain(canonicalName),
                packagePath = Package(relativeTargetBaseDirectory.toString().split("/")),
                target = SingleTarget(targetName),
              )
            newMap[canonicalName] = newMap.getOrDefault(canonicalName, emptyList()) + label
            return CONTINUE
          }
        },
      )
    }
    canonicalRepoNameToBzlFiles = newMap
  }

  fun getApparentRepoNameToFiles(): Map<String, List<Label>> = canonicalRepoNameToBzlFiles

  @TestOnly
  fun forceUpdateCache() {
    updateCache()
  }

  companion object {
    fun getInstance(project: Project): BazelBzlFileService = project.getService(BazelBzlFileService::class.java)
  }

  class BazelBzlFileServiceStartUpActivity : BazelProjectActivity() {
    override suspend fun executeForBazelProject(project: Project) {
      BazelBzlFileService.getInstance(project)
    }
  }
}

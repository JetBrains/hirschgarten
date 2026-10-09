package org.jetbrains.bazel.javascript.sync

import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.javascript.BazelJavaScriptBundle
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporterFactory
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.target.baseDirectoryLocation
import org.jetbrains.bazel.target.isWorkspace
import org.jetbrains.bazel.utils.filterPathsThatDontContainEachOther
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.isRegularFile

private const val JAVASCRIPT_WORKSPACE_MODULE_NAME = ".javascript"

/**
 * The JavaScript plugin expects sources to be under a module content root: e.g., the Vitest/Jest run configuration producers
 * only look for `package.json` and the test framework config file up to the content root of the test file,
 * and files outside content roots are not indexed.
 *
 * Bazel JS rules don't provide any IDE info yet, so JS packages are recognized by the kinds of the targets they contain.
 */
internal class JavaScriptWorkspaceImporter(private val context: WorkspaceImporterContext) : BazelWorkspaceImporter {
  class Factory : BazelWorkspaceImporterFactory {
    override fun createWorkspaceImporter(context: WorkspaceImporterContext): BazelWorkspaceImporter =
      JavaScriptWorkspaceImporter(context)
  }

  private lateinit var packageRoots: List<Path>

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelJavaScriptBundle.message("javascript.workspace.importer")

  override suspend fun import(phase: WorkspaceImporterPhase, snapshot: WorkspaceSnapshot, taskId: TaskId): WorkspaceImporterResult {
    when (phase) {
      is WorkspaceImporterPhase.Initialize -> {
        val workspaceRoot = context.project.rootDir.toNioPath()
        val localRepositories = snapshot.repoMapping.getLocalRepositories()
        val targetDirectories =
          snapshot.targetGraph.allTargets
            .mapNotNull { snapshot.targets.findTargetInfoByKey(it) }
            .filter { it.isWorkspace(snapshot.repoMapping) && isJavaScriptRuleKind(it.kind.kind) }
            .mapNotNull { context.outputResolver.resolve(it.baseDirectoryLocation, localRepositories) }
            .toSet()
        packageRoots = withContext(Dispatchers.IO) { findJavaScriptPackageRoots(workspaceRoot, targetDirectories) }
        if (packageRoots.isEmpty()) {
          return WorkspaceImporterResult.Abort
        }
      }

      is WorkspaceImporterPhase.WorkspaceApply -> {
        addJavaScriptWorkspaceModule(phase.builder, context.vfuManager, phase.entitySource)
      }

      else -> {}
    }
    return WorkspaceImporterResult.Success
  }

  private fun addJavaScriptWorkspaceModule(
    builder: MutableEntityStorage,
    vfuManager: VirtualFileUrlManager,
    entitySource: EntitySource,
  ) {
    val contentRoots = packageRoots.map { root ->
      ContentRootEntity(
        url = root.toVirtualFileUrl(vfuManager),
        entitySource = entitySource,
        // Installed packages are resolved by the JavaScript plugin as libraries, they must not be indexed as sources
        excludedPatterns = listOf("node_modules"),
      )
    }
    builder.addEntity(
      ModuleEntity(
        name = JAVASCRIPT_WORKSPACE_MODULE_NAME,
        dependencies = listOf(ModuleSourceDependency),
        entitySource = entitySource,
      ) {
        this.contentRoots = contentRoots
      },
    )
  }
}

/** Rules from `rules_js` (`js_binary`, `js_test`, `js_library`, ...) and `rules_ts` (`ts_project`, `ts_config`, ...). */
private fun isJavaScriptRuleKind(kind: String): Boolean = kind.startsWith("js_") || kind.startsWith("ts_")

/**
 * Maps directories of JS targets to the JS package they belong to: the closest directory with a `package.json`,
 * or the target directory itself if there is none below the workspace root.
 *
 * The workspace root is never used, even if it holds the root `package.json` of a pnpm workspace:
 * it would turn every file of the repository into an indexed source.
 */
@ApiStatus.Internal
fun findJavaScriptPackageRoots(workspaceRoot: Path, targetDirectories: Set<Path>): List<Path> =
  targetDirectories
    .filter { it.startsWith(workspaceRoot) && it != workspaceRoot }
    .map { targetDirectory ->
      generateSequence(targetDirectory) { it.parent }
        .takeWhile { it != workspaceRoot && it.startsWith(workspaceRoot) }
        .firstOrNull { it.resolve("package.json").isRegularFile() }
      ?: targetDirectory
    }
    .toSet()
    .filterPathsThatDontContainEachOther()
    .sorted()

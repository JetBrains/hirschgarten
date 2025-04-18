package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelBinPathResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val firstPhaseTargetToBspMapper: FirstPhaseTargetToBspMapper,
  private val projectProvider: ProjectProvider,
  private val bazelInfo: BazelInfo,
  private val workspaceContextProvider: WorkspaceContextProvider,
) {
  suspend fun workspaceBuildTargets(build: Boolean, originId: String): WorkspaceBuildTargetsResult {
    val project = projectProvider.refreshAndGet(build = build, originId = originId)
    return bspMapper.workspaceTargets(project)
  }

  suspend fun workspaceBuildTargetsPartial(targetsToSync: List<Label>): WorkspaceBuildTargetsResult {
    val project =
      projectProvider.updateAndGet(
        targetsToSync = targetsToSync,
      )
    return bspMapper.workspaceTargets(project)
  }

  suspend fun workspaceBuildFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult {
    val project = projectProvider.bazelQueryRefreshAndGet(params.originId)
    return firstPhaseTargetToBspMapper.toWorkspaceBuildTargetsResult(project)
  }

  suspend fun workspaceBuildLibraries(): WorkspaceLibrariesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceLibrariesResult(emptyList())
    return bspMapper.workspaceLibraries(project)
  }

  suspend fun workspaceBuildGoLibraries(): WorkspaceGoLibrariesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceGoLibrariesResult(emptyList())
    return bspMapper.workspaceGoLibraries(project)
  }

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult {
    val project = projectProvider.get()
    return bspMapper.workspaceDirectories(project)
  }

  fun workspaceInvalidTargets(): WorkspaceInvalidTargetsResult {
    // TODO: BAZEL-1644
    return WorkspaceInvalidTargetsResult(emptyList())
//    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceInvalidTargetsResult(emptyList())
//    return bspMapper.workspaceInvalidTargets(project)
  }

  suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.get()
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  fun workspaceBazelBinPath(): WorkspaceBazelBinPathResult = WorkspaceBazelBinPathResult(bazelInfo.bazelBin.toString())

  suspend fun workspaceName(): WorkspaceNameResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceNameResult(null)
    return WorkspaceNameResult(project.workspaceName)
  }

  suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return InverseSourcesResult(emptyList())
    return bspMapper.inverseSources(project, inverseSourcesParams)
  }

  suspend fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): DependencySourcesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return DependencySourcesResult(emptyList())
    return bspMapper.dependencySources(project, dependencySourcesParams)
  }

  suspend fun jvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JvmRunEnvironmentResult(emptyList())
    return bspMapper.jvmRunEnvironment(project, params)
  }

  suspend fun jvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JvmTestEnvironmentResult(emptyList())
    return bspMapper.jvmTestEnvironment(project, params)
  }

  suspend fun jvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JvmBinaryJarsResult(emptyList())
    return bspMapper.jvmBinaryJars(project, params)
  }

  suspend fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JavacOptionsResult(emptyList())
    return bspMapper.buildTargetJavacOptions(project, params)
  }

  suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return CppOptionsResult(emptyList())
    return bspMapper.buildTargetCppOptions(project, params)
  }

  suspend fun buildTargetPythonOptions(params: PythonOptionsParams): PythonOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return PythonOptionsResult(emptyList())
    return bspMapper.buildTargetPythonOptions(project, params)
  }

  suspend fun buildTargetScalacOptions(params: ScalacOptionsParams): ScalacOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return ScalacOptionsResult(emptyList())
    return bspMapper.buildTargetScalacOptions(project, params)
  }

  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    bspMapper.resolveLocalToRemote(params)

  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    bspMapper.resolveRemoteToLocal(params)

  fun workspaceContext(): WorkspaceContext =
    projectProvider.getIfLoaded()?.workspaceContext ?: workspaceContextProvider.readWorkspaceContext()
}

package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
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
import org.jetbrains.bsp.protocol.JvmCompileClasspathParams
import org.jetbrains.bsp.protocol.JvmCompileClasspathResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBazelBinPathResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val firstPhaseTargetToBspMapper: FirstPhaseTargetToBspMapper,
  private val projectProvider: ProjectProvider,
  private val bazelInfo: BazelInfo,
) {
  fun workspaceBuildTargets(build: Boolean): WorkspaceBuildTargetsResult {
    val project = projectProvider.refreshAndGet(build = build)
    return bspMapper.workspaceTargets(project)
  }

  fun workspaceBuildTargetsPartial(targetsToSync: List<Label>): WorkspaceBuildTargetsResult {
    val project =
      projectProvider.updateAndGet(
        targetsToSync = targetsToSync,
      )
    return bspMapper.workspaceTargets(project)
  }

  fun workspaceBuildFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult {
    val project = projectProvider.bazelQueryRefreshAndGet(params.originId)
    return firstPhaseTargetToBspMapper.toWorkspaceBuildTargetsResult(project)
  }

  fun workspaceBuildLibraries(): WorkspaceLibrariesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceLibrariesResult(emptyList())
    return bspMapper.workspaceLibraries(project)
  }

  fun workspaceBuildGoLibraries(): WorkspaceGoLibrariesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceGoLibrariesResult(emptyList())
    return bspMapper.workspaceGoLibraries(project)
  }

  fun workspaceNonModuleTargets(): NonModuleTargetsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return NonModuleTargetsResult(emptyList())
    return bspMapper.workspaceNonModuleTargets(project)
  }

  fun workspaceDirectories(): WorkspaceDirectoriesResult {
    val project = projectProvider.get()
    return bspMapper.workspaceDirectories(project)
  }

  fun workspaceInvalidTargets(): WorkspaceInvalidTargetsResult {
    // TODO: BAZEL-1644
    return WorkspaceInvalidTargetsResult(emptyList())
//    val project = projectProvider.get() as? AspectSyncProject ?: return WorkspaceInvalidTargetsResult(emptyList())
//    return bspMapper.workspaceInvalidTargets(project)
  }

  fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.get()
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  fun workspaceBazelBinPath(): WorkspaceBazelBinPathResult = WorkspaceBazelBinPathResult(bazelInfo.bazelBin.toString())

  fun buildTargetSources(sourcesParams: SourcesParams): SourcesResult {
    val project = projectProvider.get()
    return when (project) {
      is AspectSyncProject -> bspMapper.sources(project, sourcesParams)
      is FirstPhaseProject -> firstPhaseTargetToBspMapper.toSourcesResult(project, sourcesParams)
    }
  }

  fun buildTargetResources(resourcesParams: ResourcesParams): ResourcesResult {
    val project = projectProvider.get()
    return when (project) {
      is AspectSyncProject -> bspMapper.resources(project, resourcesParams)
      is FirstPhaseProject -> firstPhaseTargetToBspMapper.toResourcesResult(project, resourcesParams)
    }
  }

  suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return InverseSourcesResult(emptyList())
    return bspMapper.inverseSources(project, inverseSourcesParams)
  }

  fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): DependencySourcesResult {
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

  fun jvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JvmBinaryJarsResult(emptyList())
    return bspMapper.jvmBinaryJars(project, params)
  }

  suspend fun jvmCompileClasspath(params: JvmCompileClasspathParams): JvmCompileClasspathResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JvmCompileClasspathResult(emptyList())
    return bspMapper.jvmCompileClasspath(project, params)
  }

  suspend fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return JavacOptionsResult(emptyList())
    return bspMapper.buildTargetJavacOptions(project, params)
  }

  fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return CppOptionsResult(emptyList())
    return bspMapper.buildTargetCppOptions(project, params)
  }

  fun buildTargetPythonOptions(params: PythonOptionsParams): PythonOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return PythonOptionsResult(emptyList())
    return bspMapper.buildTargetPythonOptions(project, params)
  }

  suspend fun buildTargetScalacOptions(params: ScalacOptionsParams): ScalacOptionsResult {
    val project = projectProvider.get() as? AspectSyncProject ?: return ScalacOptionsResult(emptyList())
    return bspMapper.buildTargetScalacOptions(project, params)
  }

  fun rustWorkspace(params: RustWorkspaceParams): RustWorkspaceResult {
    val project =
      projectProvider.get() as? AspectSyncProject
        ?: return RustWorkspaceResult(emptyList(), emptyMap(), emptyMap(), emptyList())
    return bspMapper.rustWorkspace(project, params)
  }

  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    bspMapper.resolveLocalToRemote(params)

  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    bspMapper.resolveRemoteToLocal(params)
}

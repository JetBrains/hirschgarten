package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectClientProjectMapper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult

/**
 * Indirection between plugin and [JoinedBuildServer],
 * allows intercepting server calls in single place.
 * Also contains refactored endpoints from bsp server to plugin.
 */
interface BazelEndpointProxy {
  fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult

  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult

  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult

  fun buildJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult

  fun dependencySources(params: DependencySourcesParams): DependencySourcesResult

  suspend fun jvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult

  suspend fun jvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult

  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  suspend fun buildTargetTest(params: TestParams): TestResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRun(params: RunParams): RunResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo

  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult
}

class DefaultBazelEndpointProxy(
  val mapper: AspectClientProjectMapper,
  val project: BazelMappedProject,
  val server: JoinedBuildServer,
) : BazelEndpointProxy {
  override fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult = mapper.buildTargetJavacOptions(project, params)

  override fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    mapper.resolveLocalToRemote(params)

  override fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    mapper.resolveRemoteToLocal(params)

  override fun buildJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = mapper.buildJvmBinaryJars(project, params)

  override fun dependencySources(params: DependencySourcesParams): DependencySourcesResult = mapper.dependencySources(project, params)

  override suspend fun jvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    mapper.jvmRunEnvironment(server, project, params)

  override suspend fun jvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    mapper.jvmTestEnvironment(server, project, params)

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = server.buildTargetCompile(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = server.buildTargetTest(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = server.buildTargetAnalysisDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = server.buildTargetRun(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = server.buildTargetRunWithDebug(params)

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo = server.jvmToolchainInfoForTarget(target)

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult =
    server.buildTargetInverseSources(params)
}

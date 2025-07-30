package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectClientProjectMapper
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
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult

class BazelEndpointProxy(
  val mapper: AspectClientProjectMapper,
  val project: BazelMappedProject,
  val server: JoinedBuildServer,
) {
  fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult = mapper.buildTargetJavacOptions(project, params)

  fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult = mapper.resolveLocalToRemote(params)

  fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult = mapper.resolveRemoteToLocal(params)

  fun buildJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = mapper.buildJvmBinaryJars(project, params)

  fun dependencySources(params: DependencySourcesParams): DependencySourcesResult = mapper.dependencySources(project, params)

  suspend fun jvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    mapper.jvmRunEnvironment(server, project, params)

  suspend fun jvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    mapper.jvmTestEnvironment(server, project, params)

  suspend fun buildTargetCompile(params: CompileParams): CompileResult =
    server.buildTargetCompile(params)

  suspend fun buildTargetTest(params: TestParams): TestResult =
    server.buildTargetTest(params)

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult =
    server.buildTargetAnalysisDebug(params)

  suspend fun buildTargetRun(params: RunParams): RunResult = server.buildTargetRun(params)

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = server.buildTargetRunWithDebug(params)
}

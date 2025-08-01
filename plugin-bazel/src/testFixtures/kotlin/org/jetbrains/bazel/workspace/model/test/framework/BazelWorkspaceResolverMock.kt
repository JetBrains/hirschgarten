package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.workspace.BazelEndpointProxy
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolver
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.EarlyBazelSyncProject
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

class BazelWorkspaceResolverMock(
  private val resolvedWorkspace: BazelResolvedWorkspace? = null,
  private val mappedProject: BazelMappedProject? = null,
  private val earlyBazelSyncProject: EarlyBazelSyncProject? = null,
  private val endpointProxy: BazelEndpointProxy? = null,
) : BazelWorkspaceResolver {
  override suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope,
    taskId: String,
    force: Boolean,
  ): BazelResolvedWorkspace = resolvedWorkspace ?: error("resolved workspace is not set")

  override suspend fun getOrFetchMappedProject(
    scope: ProjectSyncScope,
    taskId: String,
    force: Boolean,
  ): BazelMappedProject = mappedProject ?: error("mapped project is not set")

  override suspend fun getOrFetchSyncedProject(
    build: Boolean,
    taskId: String,
    force: Boolean,
  ): EarlyBazelSyncProject = earlyBazelSyncProject ?: error("early bazel sync project is not set")

  override suspend fun <T> withEndpointProxy(func: suspend (BazelEndpointProxy) -> T): T {
    return func(endpointProxy ?: error("endpoint proxy is not set"))
  }
}

class BazelEndpointProxyMock(
  private val javacOptionsResult: JavacOptionsResult? = null,
  private val resolveLocalToRemoteResult: BazelResolveLocalToRemoteResult? = null,
  private val resolveRemoteToLocalResult: BazelResolveRemoteToLocalResult? = null,
  private val jvmBinaryJarsResult: JvmBinaryJarsResult? = null,
  private val dependencySourcesResult: DependencySourcesResult? = null,
  private val jvmRunEnvironmentResult: JvmRunEnvironmentResult? = null,
  private val jvmTestEnvironmentResult: JvmTestEnvironmentResult? = null,
  private val compileResult: CompileResult? = null,
  private val testResult: TestResult? = null,
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val runResult: RunResult? = null,
  private val runWithDebugResult: RunResult? = null,
) : BazelEndpointProxy {
  override fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult =
    javacOptionsResult ?: error("javacOptionsResult is not set")

  override fun resolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    resolveLocalToRemoteResult ?: error("resolveLocalToRemoteResult is not set")

  override fun resolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    resolveRemoteToLocalResult ?: error("resolveRemoteToLocalResult is not set")

  override fun buildJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult =
    jvmBinaryJarsResult ?: error("jvmBinaryJarsResult is not set")

  override fun dependencySources(params: DependencySourcesParams): DependencySourcesResult =
    dependencySourcesResult ?: error("dependencySourcesResult is not set")

  override suspend fun jvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    jvmRunEnvironmentResult ?: error("jvmRunEnvironmentResult is not set")

  override suspend fun jvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    jvmTestEnvironmentResult ?: error("jvmTestEnvironmentResult is not set")

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult =
    compileResult ?: error("compileResult is not set")

  override suspend fun buildTargetTest(params: TestParams): TestResult =
    testResult ?: error("testResult is not set")

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult =
    analysisDebugResult ?: error("analysisDebugResult is not set")

  override suspend fun buildTargetRun(params: RunParams): RunResult =
    runResult ?: error("runResult is not set")

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult =
    runWithDebugResult ?: error("runWithDebugResult is not set")
}

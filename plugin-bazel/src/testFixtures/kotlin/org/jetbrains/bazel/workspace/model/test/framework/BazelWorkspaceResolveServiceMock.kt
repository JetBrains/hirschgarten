package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.workspace.BazelEndpointProxy
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.sync.workspace.EarlyBazelSyncProject
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult

class BazelWorkspaceResolveServiceMock(
  private val resolvedWorkspace: BazelResolvedWorkspace? = null,
  private val earlyBazelSyncProject: EarlyBazelSyncProject? = null,
  private val endpointProxy: BazelEndpointProxy? = null,
) : BazelWorkspaceResolveService {
  override suspend fun getOrFetchResolvedWorkspace(scope: ProjectSyncScope, taskId: String): BazelResolvedWorkspace =
    resolvedWorkspace ?: error("resolved workspace is not set")

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: String): EarlyBazelSyncProject =
    earlyBazelSyncProject ?: error("early bazel sync project is not set")

  override suspend fun invalidateCachedState() {
    // no-op
  }

  override suspend fun <T> withEndpointProxy(func: suspend (BazelEndpointProxy) -> T): T =
    func(endpointProxy ?: error("endpoint proxy is not set"))
}

class BazelEndpointProxyMock(
  private val compileResult: CompileResult? = null,
  private val testResult: TestResult? = null,
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val runResult: RunResult? = null,
  private val runWithDebugResult: RunResult? = null,
) : BazelEndpointProxy {

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = compileResult ?: error("compileResult is not set")

  override suspend fun buildTargetTest(params: TestParams): TestResult = testResult ?: error("testResult is not set")

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult =
    analysisDebugResult ?: error("analysisDebugResult is not set")

  override suspend fun buildTargetRun(params: RunParams): RunResult = runResult ?: error("runResult is not set")

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult =
    runWithDebugResult ?: error("runWithDebugResult is not set")
}

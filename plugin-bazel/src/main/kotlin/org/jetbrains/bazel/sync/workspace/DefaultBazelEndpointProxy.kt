package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult

class DefaultBazelEndpointProxy(
  val server: JoinedBuildServer,
) : BazelEndpointProxy {
  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = server.buildTargetCompile(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = server.buildTargetTest(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = server.buildTargetAnalysisDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = server.buildTargetRun(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = server.buildTargetRunWithDebug(params)
}

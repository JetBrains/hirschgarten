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

/**
 * Indirection between plugin and [JoinedBuildServer],
 * allows intercepting server calls in single place.
 * Also contains refactored endpoints from bsp server to plugin.
 */
interface BazelEndpointProxy {

  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  suspend fun buildTargetTest(params: TestParams): TestResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRun(params: RunParams): RunResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult
}


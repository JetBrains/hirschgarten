package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.DependenciesExportedItem
import org.jetbrains.bsp.protocol.DependenciesExportedParams
import org.jetbrains.bsp.protocol.DependenciesExportedResult
import kotlin.time.Duration.Companion.seconds

object BazelBspDependenciesExportedProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createBazelClient()

  @JvmStatic
  fun main(args: Array<String>): Unit = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      compareDependenciesExportedResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/" +
        "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk17_$javaHomeArchitecture/"

    val jvmBuildTarget =
      JvmBuildTarget().also {
        it.javaVersion = "17"
        it.javaHome = javaHome
      }

    val capabilities =
      BuildTargetCapabilities().also {
        it.canCompile = true
        it.canDebug = false
        it.canRun = false
        it.canTest = false
      }

    val aId = BuildTargetIdentifier("$targetPrefix//:A")
    val bId = BuildTargetIdentifier("$targetPrefix//:B")
    val cId = BuildTargetIdentifier("$targetPrefix//:C")
    val dId = BuildTargetIdentifier("$targetPrefix//:D")
    val eId = BuildTargetIdentifier("$targetPrefix//:E")

    val aBuildTarget =
      BuildTarget(
        aId,
        listOf("library"),
        listOf("java"),
        listOf(bId, cId, dId, eId),
        capabilities,
      )
    aBuildTarget.displayName = "@//:A"
    aBuildTarget.baseDirectory = "file://\$WORKSPACE/"
    aBuildTarget.data = jvmBuildTarget
    aBuildTarget.dataKind = BuildTargetDataKind.JVM

    val bBuildTarget =
      BuildTarget(
        bId,
        listOf("library"),
        listOf("java"),
        emptyList(),
        capabilities,
      )
    bBuildTarget.displayName = "@//:B"
    bBuildTarget.baseDirectory = "file://\$WORKSPACE/"
    bBuildTarget.data = jvmBuildTarget
    bBuildTarget.dataKind = BuildTargetDataKind.JVM

    val cBuildTarget =
      BuildTarget(
        cId,
        listOf("library"),
        listOf("java"),
        emptyList(),
        capabilities,
      )
    cBuildTarget.displayName = "@//:C"
    cBuildTarget.baseDirectory = "file://\$WORKSPACE/"
    cBuildTarget.data = jvmBuildTarget
    cBuildTarget.dataKind = BuildTargetDataKind.JVM

    val dBuildTarget =
      BuildTarget(
        dId,
        listOf("library"),
        listOf("java"),
        emptyList(),
        capabilities,
      )
    dBuildTarget.displayName = "@//:D"
    dBuildTarget.baseDirectory = "file://\$WORKSPACE/"
    dBuildTarget.data = jvmBuildTarget
    dBuildTarget.dataKind = BuildTargetDataKind.JVM

    val eBuildTarget =
      BuildTarget(
        eId,
        listOf("library"),
        listOf("java"),
        emptyList(),
        capabilities,
      )
    eBuildTarget.displayName = "@//:E"
    eBuildTarget.baseDirectory = "file://\$WORKSPACE/"
    eBuildTarget.data = jvmBuildTarget
    eBuildTarget.dataKind = BuildTargetDataKind.JVM

    return WorkspaceBuildTargetsResult(listOf(aBuildTarget, bBuildTarget, cBuildTarget, dBuildTarget, eBuildTarget))
  }

  private fun expectedDependenciesExportedResult(): DependenciesExportedResult =
    DependenciesExportedResult(
      listOf(
        DependenciesExportedItem(
          target = BuildTargetIdentifier("$targetPrefix//:A"),
          // B is exported, C is not exported, D is exported, E is exported
          dependenciesExported = listOf(true, false, true, true),
        ),
        DependenciesExportedItem(
          target = BuildTargetIdentifier("$targetPrefix//:B"),
          dependenciesExported = emptyList(),
        ),
        DependenciesExportedItem(
          target = BuildTargetIdentifier("$targetPrefix//:C"),
          dependenciesExported = emptyList(),
        ),
        DependenciesExportedItem(
          target = BuildTargetIdentifier("$targetPrefix//:D"),
          dependenciesExported = emptyList(),
        ),
        DependenciesExportedItem(
          target = BuildTargetIdentifier("$targetPrefix//:E"),
          dependenciesExported = emptyList(),
        ),
      ),
    )

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      testClient.test(120.seconds) { session, _ ->
        val result = session.server.workspaceBuildTargets().await()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
      }
    }

  private fun compareDependenciesExportedResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare dependencies exported results",
    ) {
      testClient.test(60.seconds) { session, _ ->
        val params = DependenciesExportedParams(expectedTargetIdentifiers())
        val result = session.server.buildTargetDependenciesExported(params).await()
        testClient.assertJsonEquals<DependenciesExportedResult>(expectedDependenciesExportedResult(), result)
      }
    }
}

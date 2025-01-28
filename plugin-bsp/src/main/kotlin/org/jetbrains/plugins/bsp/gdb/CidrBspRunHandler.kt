package org.jetbrains.plugins.bsp.gdb

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrCommandLineState
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesCPP

class CidrBspRunHandler (val configuration: BspRunConfiguration) : BspRunHandler {
  private val buildToolName: String = configuration.project.assets.presentableName
  override val name: String = "Jvm $buildToolName Run Handler"

  override val state = GenericRunState()
  override fun getRunProfileState(executor: Executor, env: ExecutionEnvironment): RunProfileState{
    return CidrCommandLineState(env,  BazelCidrLauncher(configuration,  env,));
  }


  class CidrBspRunHandlerProvider: BspRunHandlerProvider{
    override val id: String = "CidrBspRunHandlerProvider"
    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = CidrBspRunHandler(configuration)
    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        // todo: write a better one
        it.languageIds.includesCPP()
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }

}

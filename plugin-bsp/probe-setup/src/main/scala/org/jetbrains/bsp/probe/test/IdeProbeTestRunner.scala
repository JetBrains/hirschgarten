package org.jetbrains.bsp.probe.test

import org.virtuslab.ideprobe.config.WorkspaceConfig
import org.virtuslab.ideprobe.dependencies.Resource
import org.virtuslab.ideprobe.protocol.Endpoints
import org.virtuslab.ideprobe.robot.{RobotPluginExtension, RobotProbeDriver}
import org.virtuslab.ideprobe.{IdeProbeFixture, IntelliJFixture, ProbeDriver, WaitDecision, WaitLogic, WorkspaceProvider}

import java.nio.file.Path
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class IdeProbeTestRunner extends IdeProbeFixture with RobotPluginExtension {

  private val fixture: IntelliJFixture =
    fixtureFromConfig("ideprobe.conf")

  def fixtureWithWorkspaceFromGit(uri: String, tag: String): IntelliJFixture = {
    fixture.copy(workspaceProvider = WorkspaceProvider.from(
      WorkspaceConfig.GitTag(Resource.from(uri), tag)
    ))
  }

  def runAfterOpen(updateFixture: IntelliJFixture, action: (ProbeDriver, RobotProbeDriver) => Unit): Unit = {
    updateFixture.run { intellij =>
      val probe = intellij.probe
      val robot = probe.withRobot
      val openingProjectWaitLogic = openProjectAsync(intellij.probe, intellij.workspace)
      robot.clickDialogButton("Open or Import Project", "OK")
      probe.await(openingProjectWaitLogic)
      robot.clickDialogButton("Import Project via BSP", "Next", 60.second)
      robot.clickDialogButton("Import Project via BSP", "Create")
      probe.await(robot.extendWaitLogic(WaitLogic.Default))
      action(probe, robot)
    }
  }

  implicit class RobotOpts(p: RobotProbeDriver) {
    def clickDialogButton(dialogTitle: String, buttonTitle: String, timeout: FiniteDuration = 10.second): Unit = {
      p.findWithTimeout(query.dialog(dialogTitle), timeout)
        .findWithTimeout(query.button("text" -> buttonTitle), 1.second)
        .doClick()
    }
  }

  def openProjectAsync(driver: ProbeDriver, path: Path): WaitLogic = {
    val future = driver.sendAsync(Endpoints.OpenProject, path)
    WaitLogic.basic() {
      if (future.isCompleted)
        WaitDecision.KeepWaiting("Waiting for opening the project")
      else
        WaitDecision.Done
    }
  }
}
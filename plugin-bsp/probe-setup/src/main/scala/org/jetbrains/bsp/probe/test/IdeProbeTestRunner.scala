package org.jetbrains.bsp.probe.test

import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import org.apache.commons.io.file.spi.FileSystemProviders.installed
import org.virtuslab.ideprobe.config.WorkspaceConfig
import org.virtuslab.ideprobe.dependencies.Resource
import org.virtuslab.ideprobe.protocol.Endpoints
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.reporting.AfterTestChecks
import org.virtuslab.ideprobe.robot.{RobotPluginExtension, RobotProbeDriver}
import org.virtuslab.ideprobe.{IdeProbeFixture, IntelliJFixture, ProbeDriver, RunnableIntelliJFixture, RunningIntelliJFixture, WaitDecision, WaitLogic, WorkspaceProvider}

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

  def openProject(intellij: RunningIntelliJFixture, configuration: Option[String] = None): (ProbeDriver, RobotProbeDriver) = {
    val probe = intellij.probe
    val robot = probe.withRobot
    val openingProjectWaitLogic = openProjectAsync(intellij.probe, intellij.workspace)
    robot.clickDialogButton("Open or Import Project", "OK", continueOnFail = true)
    probe.await(openingProjectWaitLogic)
    robot.clickDialogButton("Import Project via BSP", "Next", 360.second)
    configuration.map(robot.setText(query.className("JBTextArea"), _))
    robot.clickDialogButton("Import Project via BSP", "Create")
    probe.await(robot.extendWaitLogic(WaitLogic.Default))
    (probe, robot)
  }

  def runAfterOpen(updateFixture: IntelliJFixture, configuration: Option[String] = None, action: (ProbeDriver, RobotProbeDriver) => Unit): Unit = {
    updateFixture.run { intellij =>
      val (probe, robot) = openProject(intellij, configuration)
      action(probe, robot)
    }
  }

  def prepareInstance(baseFixture: IntelliJFixture) = {
    val workspace = baseFixture.setupWorkspace()
    val installed = baseFixture.installIntelliJ()
    (baseFixture, workspace, installed)
  }

  def runIntellij[A](data: (IntelliJFixture, Path, InstalledIntelliJ), action: RunningIntelliJFixture => A): A = {
    val (baseFixture, workspace, installed) = data
    val running = baseFixture.startIntelliJ(workspace, installed)
    val runningData = new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, installed.paths)
    try action(runningData)
    finally {
      AfterTestChecks(baseFixture.intelliJProvider.config.check, runningData.probe)
      baseFixture.closeIntellij(running)
    }
  }

  def cleanInstance(data: (IntelliJFixture, Path, InstalledIntelliJ)): Unit = {
    val (baseFixture, workspace, installed) = data
    baseFixture.cleanupIntelliJ(installed)
    baseFixture.deleteWorkspace(workspace)
  }


  implicit class RobotOpts(p: RobotProbeDriver) {
    def clickDialogButton(dialogTitle: String, buttonTitle: String, timeout: FiniteDuration = 10.second, continueOnFail: Boolean = false): Unit = {
      try {
        p.findWithTimeout(query.dialog(dialogTitle), timeout)
          .findWithTimeout(query.button("text" -> buttonTitle), 1.second)
          .doClick()
      } catch {
        case e: WaitForConditionTimeoutException => if (!continueOnFail) throw e
      }
    }

    def setText(query: String, text: String): Unit = {
      p.findWithTimeout(query, 60.second).setText(text)
    }
  }

  def openProjectAsync(driver: ProbeDriver, path: Path): WaitLogic = {
    val future = driver.sendAsync(Endpoints.OpenProject, path)
    WaitLogic.basic() {
      if (future.isCompleted)
        WaitDecision.Done
      else
        WaitDecision.KeepWaiting("Waiting for opening the project")
    }
  }
}
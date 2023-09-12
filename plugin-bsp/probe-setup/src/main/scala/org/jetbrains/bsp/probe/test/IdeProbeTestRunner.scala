package org.jetbrains.bsp.probe.test

import org.virtuslab.ideprobe.WaitDecision.{Done, KeepWaiting}
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.WorkspaceConfig
import org.virtuslab.ideprobe.dependencies.Resource
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.protocol.Endpoints
import org.virtuslab.ideprobe.reporting.AfterTestChecks
import org.virtuslab.ideprobe.robot.{RobotPluginExtension, RobotProbeDriver}
import org.virtuslab.ideprobe.wait.{BasicWaiting, DoOnlyOnce}

import java.nio.file.Path
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try

class IdeProbeTestRunner extends IdeProbeFixture with RobotPluginExtension {

  private val fixture: IntelliJFixture =
    fixtureFromConfig("ideprobe.conf")

  def fixtureWithWorkspaceFromGit(uri: String, tag: String): IntelliJFixture = {
    fixture.copy(workspaceProvider = WorkspaceProvider.from(
      WorkspaceConfig.GitTag(Resource.from(uri), tag)
    ))
  }

  private def tryRunning(action: => Unit): Unit = {
    new DoOnlyOnce(action).attempt()
  }

  def openProject(intellij: RunningIntelliJFixture, configuration: Option[String] = None): (ProbeDriver, RobotProbeDriver) = {
    val probe = intellij.probe
    val robot = probe.withRobot

    openProjectAsync(intellij.probe, intellij.workspace)

    val openingIntellijProjectWaitLogic = robot.startOpeningIntellijProject()
    probe.await(openingIntellijProjectWaitLogic)

    val closeTipOfTheDay = new DoOnlyOnce(robot.closeTipOfTheDay())

    val waitLogic = robot.waitForProjectImport().doWhileWaiting {
      closeTipOfTheDay.attempt()
      tryRunning(
        robot.find(query.className("StripeButton", ("text", "BSP")))
      )
    }
    probe.await(waitLogic)

    (probe, robot)
  }

  def runAfterOpen(updateFixture: IntelliJFixture, configuration: Option[String] = None, action: (ProbeDriver, RobotProbeDriver, RunningIntelliJFixture) => Unit): Unit = {
    updateFixture.run { intellij =>
      val (probe, robot) = openProject(intellij, configuration)
      action(probe, robot, intellij)
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

  def openProjectAsync(driver: ProbeDriver, path: Path) = {
    driver.sendAsync(Endpoints.OpenProject, path)
  }

  implicit class RobotOpts(p: RobotProbeDriver) {
    def clickDialogButton(dialogTitle: String, buttonTitle: String, timeout: FiniteDuration = 10.second) = {
      p.findWithTimeout(query.dialog(dialogTitle), timeout)
        .findWithTimeout(query.button("text" -> buttonTitle), 1.second)
        .doClick()
    }

    def setText(query: String, text: String): Unit = {
      p.findWithTimeout(query, 60.second).setText(text)
    }
  }

  private def waitForAnySuccess(actions: DoOnlyOnce*) = new BasicWaiting(5.seconds, 10.minutes, { _ =>
    actions.foreach(_.attempt())
    if (actions.exists(_.isSuccessful)) Done
    else KeepWaiting()
  })

  private implicit class BspImportOps(r: RobotProbeDriver) {

    def startOpeningIntellijProject() = {
      val startOpening = clickThroughRunnersDialog()
      val findImportDialog = new DoOnlyOnce(r.findMainWindow())
      waitForAnySuccess(startOpening, findImportDialog)
    }

    private def clickThroughRunnersDialog() = new DoOnlyOnce({
      r.findWithTimeout(
        query.div("title.key" -> "project.open.select.from.multiple.processors.dialog.title"),
        5.second
      )
      r.find(query.div("text" -> "BSP (experimental) project")).doClick()
      r.clickDialogButton("Open or Import Project", "OK")
    })

    def findMainWindow() =
      r.find(query.className("ToolWindowPane"))

    def waitForProjectImport(): WaitLogic = {
      WaitLogic.forUnstableCondition(
        basicCheckFrequency = 15.second,
        atMost = 1.hour,
        ensurePeriod = 15.second,
        ensureFrequency = 5.second
      ) {
        Try({
          r.find(query.className("BuildView"))
          r.find(query.className("ContentTabLabel", ("mytext", "Sync")))
          val textPanel = r.find(query.className("InlineProgressPanel"))
          if (textPanel.findAllText().isEmpty) WaitDecision.Done
          else throw new Exception()
        }).getOrElse(WaitDecision.KeepWaiting("Waiting for project import"))
      }
    }
  }
}
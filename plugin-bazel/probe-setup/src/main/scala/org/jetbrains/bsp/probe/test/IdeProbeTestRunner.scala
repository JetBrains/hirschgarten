package org.jetbrains.bsp.probe.test

import org.jetbrains.bsp.probe.test.IdeProbeTestRunner.{EXT, version}
import org.virtuslab.ideprobe.WaitDecision.{Done, KeepWaiting}
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.WorkspaceConfig
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Resource}
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.protocol.Endpoints
import org.virtuslab.ideprobe.robot.{RobotPluginExtension, RobotProbeDriver}
import org.virtuslab.ideprobe.wait.{BasicWaiting, DoOnlyOnce}

import java.nio.file.Path
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try

object IdeProbeTestRunner {

  private val EXT: String = if (OS.Current == OS.Mac) ".dmg" else ".zip"

  def version(build: String, release: Option[String]): IntelliJVersion = {
    IntelliJVersion(build, release, EXT)
  }

}

class IdeProbeTestRunner(workspaceConfig: WorkspaceConfig) extends IdeProbeFixture with RobotPluginExtension {

  def this(uri: String, tag: String) = {
    this(WorkspaceConfig.GitTag(Resource.from(uri), tag))
  }

  val fixture: IntelliJFixture = {
    val wrongVersion = fixtureFromConfig("ideprobe.conf")
    fixtureFromConfig("ideprobe.conf").withVersion(
      version(wrongVersion.version.build, Option.empty)
    ).copy(workspaceProvider =
      WorkspaceProvider.from(workspaceConfig)
    )
  }

  def closeProject(probe: ProbeDriver): Unit = {
    probe.closeProject(probe.listOpenProjects().head)
  }

  def openProject(intellij: RunningIntelliJFixture): (ProbeDriver, RobotProbeDriver) = {
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

  def runIntellijAndOpenProject(action: (ProbeDriver, RobotProbeDriver, RunningIntelliJFixture) => Unit): Unit = {
    fixture.run { intellij =>
      val (probe, robot) = openProject(intellij)
      action(probe, robot, intellij)
      closeProject(probe)
    }
  }

  def installIntelliJ(action: RunnableIntelliJFixture => Unit): Unit = {
    fixture.withWorkspace(action)
  }

  def prepareInstance() = {
    val workspace = fixture.setupWorkspace()
    val installed = fixture.installIntelliJ()
    (workspace, installed)
  }

  def runIntellij[A](data: (Path, InstalledIntelliJ), action: RunningIntelliJFixture => A): A = {
    val (workspace, installed) = data
    val running = fixture.startIntelliJ(workspace, installed)
    val runningData = new RunningIntelliJFixture(workspace, running.probe, fixture.config, installed.paths)
    try action(runningData)
    finally {
      fixture.closeIntellij(running)
    }
  }

  def cleanInstance(data: (Path, InstalledIntelliJ)): Unit = {
    val (workspace, installed) = data
    fixture.cleanupIntelliJ(installed)
    fixture.deleteWorkspace(workspace)
  }

  private def openProjectAsync(driver: ProbeDriver, path: Path) = {
    driver.sendAsync(Endpoints.OpenProject, path)
  }

  private def tryRunning(action: => Unit): Unit = {
    new DoOnlyOnce(action).attempt()
  }

  private def waitForAnySuccess(actions: DoOnlyOnce*) = new BasicWaiting(5.seconds, 10.minutes, { _ =>
    actions.foreach(_.attempt())
    if (actions.exists(_.isSuccessful)) Done
    else KeepWaiting()
  })

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
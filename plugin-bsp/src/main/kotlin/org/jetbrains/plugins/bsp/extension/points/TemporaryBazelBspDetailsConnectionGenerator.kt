package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.enableIf
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.visibleIf
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isFile
import com.intellij.util.io.readText
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFile
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizard.ImportProjectWizardStep
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.writeText

public class TemporaryBazelBspDetailsConnectionGenerator : BspConnectionDetailsGeneratorExtension {

  private lateinit var projectViewFilePathProperty: ObservableProperty<Path?>

  public override fun name(): String = "bazel"

  public override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "WORKSPACE" }

  override fun calculateImportWizardSteps(
    projectBasePath: Path,
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): List<ImportProjectWizardStep> {
    val step = BazelEditProjectViewStep(projectBasePath, connectionFileOrNewConnectionProperty)
    projectViewFilePathProperty = step.projectViewFilePathProperty

    return listOf(step)
  }

  public override fun generateBspConnectionDetailsFile(
    projectPath: VirtualFile,
    outputStream: OutputStream
  ): VirtualFile {
    executeAndWait(
      calculateInstallerCommand(projectPath),
      projectPath,
      outputStream
    )
    return getChild(projectPath, listOf(".bsp", "bazelbsp.json"))!!
  }

  private fun calculateInstallerCommand(projectPath: VirtualFile): List<String> {
    val coursierExecutable = findCoursierExecutableOrDownload(projectPath)

    return listOf(
      coursierExecutable.toString(),
      "launch",
      "org.jetbrains.bsp:bazel-bsp:2.4.0",
      "-M",
      "org.jetbrains.bsp.bazel.install.Install",
    ) + calculateProjectViewFileInstallerOption()
  }

  private fun findCoursierExecutableOrDownload(projectPath: VirtualFile): Path =
    findCoursierExecutable() ?: downloadCoursierIfNotDownloaded(projectPath)

  private fun findCoursierExecutable(): Path? =
    EnvironmentUtil.getEnvironmentMap()["PATH"]
      ?.split(File.pathSeparator)
      ?.map { File(it, "cs") }
      ?.firstOrNull { it.canExecute() }
      ?.toPath()

  private fun downloadCoursierIfNotDownloaded(projectPath: VirtualFile): Path {
    // TODO we should pass it to syncConsole - it might take some time if the connection is really bad
    val coursierUrl = "https://git.io/coursier-cli"
    val coursierDestination = calculateCoursierDownloadDestination(projectPath)

    downloadCoursierIfDoesntExistInTheDestination(coursierDestination, coursierUrl)

    return coursierDestination
  }

  private fun calculateCoursierDownloadDestination(projectPath: VirtualFile): Path {
    val dotBazelBsp = projectPath.toNioPath().resolve(".bazelbsp")
    Files.createDirectories(dotBazelBsp)

    return dotBazelBsp.resolve("cs")
  }

  private fun downloadCoursierIfDoesntExistInTheDestination(coursierDestination: Path, coursierUrl: String) {
    if (!coursierDestination.toFile().exists()) {
      downloadCoursier(coursierUrl, coursierDestination)
    }
  }

  private fun downloadCoursier(coursierUrl: String, coursierDestination: Path) {
    Files.copy(URL(coursierUrl).openStream(), coursierDestination)
    coursierDestination.toFile().setExecutable(true)
  }

  private fun calculateProjectViewFileInstallerOption(): List<String> =
    projectViewFilePathProperty.get()
      ?.let { listOf("--", "-p", "$it") } ?: emptyList()
}

public class BazelEditProjectViewStep(
  private val projectBasePath: Path,
  private val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
) : ImportProjectWizardStep() {

  private val propertyGraph = PropertyGraph(isBlockPropagation = false)

  public val projectViewFilePathProperty: GraphProperty<Path?> =
    propertyGraph
      .lazyProperty { calculateProjectViewFilePath(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateProjectViewFilePath(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateProjectViewFilePath(
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): Path? =
    when (val connectionFileOrNewConnection = connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile ->
        calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.locatedBspConnectionDetails.bspConnectionDetails)
          ?.let { Path(it) }

      else -> projectBasePath.resolve(defaultProjectViewFileName)
    }

  private val projectViewFileNameProperty =
    propertyGraph
      .lazyProperty { calculateProjectViewFileName(projectViewFilePathProperty) }
      .also {
        it.dependsOn(projectViewFilePathProperty) {
          calculateProjectViewFileName(projectViewFilePathProperty)
        }
        projectViewFilePathProperty.dependsOn(it) {
          calculateNewProjectViewFilePath(it)
        }
      }

  private fun calculateProjectViewFileName(projectViewFilePathProperty: GraphProperty<Path?>): String =
    projectViewFilePathProperty.get()?.name ?: "Not specified"

  private fun calculateNewProjectViewFilePath(projectViewFileNameProperty: GraphProperty<String>): Path? {
    val newFileName = projectViewFileNameProperty.get()
    return projectViewFilePathProperty.get()?.parent?.resolve(newFileName)
  }

  private val isProjectViewFileNameEditableProperty =
    propertyGraph
      .lazyProperty { calculateIsProjectViewFileNameEditable(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateIsProjectViewFileNameEditable(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateIsProjectViewFileNameEditable(
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): Boolean =
    when (connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile -> false
      else -> true
    }

  private val isProjectViewFileNameSpecifiedProperty =
    propertyGraph
      .lazyProperty { calculateIsProjectViewFileNameSpecified(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateIsProjectViewFileNameSpecified(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateIsProjectViewFileNameSpecified(
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): Boolean =
    when (val connectionFileOrNewConnection = connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile -> calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.locatedBspConnectionDetails.bspConnectionDetails) != null
      else -> true
    }

  private fun calculateProjectViewFileNameFromConnectionDetails(bspConnectionDetails: BspConnectionDetails): String? =
    bspConnectionDetails.argv.last().takeIf {
      try {
        Path(it).isFile()
      } catch (e: InvalidPathException) {
        false
      }
    }

  private val projectViewTextProperty =
    propertyGraph
      .lazyProperty { calculateProjectViewText(projectViewFilePathProperty) }
      .also {
        it.dependsOn(projectViewFilePathProperty) {
          calculateProjectViewText(projectViewFilePathProperty)
        }
      }

  private fun calculateProjectViewText(projectViewFilePathProperty: GraphProperty<Path?>): String =
    projectViewFilePathProperty.get()
      ?.takeIf { it.exists() }
      ?.readText().orEmpty()

  override val panel: DialogPanel = panel {
    row {
      textField()
        .label("Project view file name")
        .bindText(projectViewFileNameProperty)
        .enableIf(isProjectViewFileNameEditableProperty)
        .horizontalAlign(HorizontalAlign.FILL)
    }
    row {
      textArea()
        .bindText(projectViewTextProperty)
        .visibleIf(isProjectViewFileNameSpecifiedProperty)
        .horizontalAlign(HorizontalAlign.FILL)
        .rows(15)
    }
    row {
      text("Please choose a connection file with project view file or create a new connection in order to edit project view")
        .visibleIf(isProjectViewFileNameSpecifiedProperty.transform { !it })
    }
  }

  override fun commit(finishChosen: Boolean) {
    super.commit(finishChosen)

    if (finishChosen) {
      saveProjectViewToFileIfExist()
    }
  }

  private fun saveProjectViewToFileIfExist() =
    projectViewFilePathProperty.get()?.writeText(projectViewTextProperty.get())

  private companion object {
    private const val defaultProjectViewFileName = "projectview.bazelproject"
  }
}

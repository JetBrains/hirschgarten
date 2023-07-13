package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.util.io.isFile
import com.intellij.util.io.readText
import org.jetbrains.plugins.bsp.config.BspPluginTemplates
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFile
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizard.ImportProjectWizardStep
import java.io.OutputStream
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText

public const val BAZEL_BSP_ID: String = "bazelbsp"

public class TemporaryBazelBspDetailsConnectionGenerator : BspConnectionDetailsGeneratorExtension {

  private lateinit var projectViewFilePathProperty: ObservableProperty<Path?>

  public override fun id(): String = BAZEL_BSP_ID

  public override fun displayName(): String = "Bazel"

  public override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "WORKSPACE" || it.name == "WORKSPACE.bazel" }

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
    outputStream: OutputStream,
    project: Project
  ): VirtualFile {

    executeAndWait(
      command = calculateInstallerCommand(),
      projectPath = projectPath,
      outputStream = outputStream,
      project = project
    )
    return getChild(projectPath, listOf(".bsp", "bazelbsp.json"))!!
  }

  private fun calculateInstallerCommand(): List<String> = listOf(
      ExternalCommandUtils.calculateJavaExecPath(),
      "-cp",
      ExternalCommandUtils
        .calculateNeededJars(
          org = "org.jetbrains.bsp",
          name = "bazel-bsp",
          version = "2.7.2-20230706-f4c9b25-NIGHTLY"
        )
        .joinToString(":"),
      "org.jetbrains.bsp.bazel.install.Install",
    ) + calculateProjectViewFileInstallerOption()

  private fun calculateProjectViewFileInstallerOption(): List<String> =
    projectViewFilePathProperty.get()
      ?.let { listOf("-p", "$it") } ?: emptyList()
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
        calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.bspConnectionDetails)
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
      is ConnectionFile ->
        calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.bspConnectionDetails) != null

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
      ?.readText()
      ?: BspPluginTemplates.defaultBazelProjectViewContent

  override val panel: DialogPanel = panel {
    row {
      textField()
        .label("Project view file name")
        .bindText(projectViewFileNameProperty)
        .enabledIf(isProjectViewFileNameEditableProperty)
        .align(Align.FILL)
    }
    row {
      textArea()
        .bindText(projectViewTextProperty)
        .visibleIf(isProjectViewFileNameSpecifiedProperty)
        .align(Align.FILL)
        .rows(15)
    }.resizableRow()
    row {
      text("Please choose a connection file with project view file " +
        "or create a new connection in order to edit project view")
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

package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.isFile
import com.intellij.util.io.readText
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFile
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizard.ImportProjectWizardStep
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

public class TemporaryBazelBspDetailsConnectionGenerator : BspConnectionDetailsGeneratorExtension {

  private lateinit var projectViewFilePathProperty: ObservableProperty<Path?>

  public override fun id(): String = "bazelbsp"

  public override fun displayName(): String = "Bazel"

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
    val coursierExecutable = findCoursierExecutableOrPrepare(projectPath)

    return listOf(
      coursierExecutable.toString(),
      "launch",
      "org.jetbrains.bsp:bazel-bsp:2.6.0",
      "-M",
      "org.jetbrains.bsp.bazel.install.Install",
    ) + calculateProjectViewFileInstallerOption()
  }

  private fun findCoursierExecutableOrPrepare(projectPath: VirtualFile): Path =
    findCoursierExecutable() ?: prepareCoursierIfNotExists(projectPath)

  private fun findCoursierExecutable(): Path? =
    EnvironmentUtil.getEnvironmentMap()["PATH"]
      ?.split(File.pathSeparator)
      ?.map { File(it, CoursierUtils.calculateCoursierExecutableName()) }
      ?.firstOrNull { it.canExecute() }
      ?.toPath()

  private fun prepareCoursierIfNotExists(projectPath: VirtualFile): Path {
    // TODO we should pass it to syncConsole - it might take some time if the connection is really bad
    val coursierDestination = calculateCoursierExecutableDestination(projectPath)

    CoursierUtils.prepareCoursierIfDoesntExistInTheDestination(coursierDestination)

    return coursierDestination
  }

  private fun calculateCoursierExecutableDestination(projectPath: VirtualFile): Path {
    val dotBazelBsp = projectPath.toNioPath().resolve(".bazelbsp")
    Files.createDirectories(dotBazelBsp)

    return dotBazelBsp.resolve(CoursierUtils.calculateCoursierExecutableName())
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
        .enabledIf(isProjectViewFileNameEditableProperty)
        .align(Align.FILL)
    }
    row {
      textArea()
        .bindText(projectViewTextProperty)
        .visibleIf(isProjectViewFileNameSpecifiedProperty)
        .align(Align.FILL)
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


public object CoursierUtils {

  public fun calculateCoursierExecutableName(): String = when (OS.current) {
    OS.WINDOWS -> "cs.exe"
    else -> "cs"
  }

  public fun prepareCoursierIfDoesntExistInTheDestination(coursierDestination: Path) {
    if (!coursierDestination.toFile().exists()) {
      prepareCoursier(coursierDestination)
    }
  }

  private fun prepareCoursier(coursierDestination: Path) {
    val coursierParentDir = coursierDestination.parent
    val coursierZipPath = coursierParentDir.resolve(calculateCoursierZipName())
    val coursierExtractedPath = coursierParentDir.resolve(calculateCoursierExtractedName())
    downloadZipFile(calculateCoursierUrl(), coursierZipPath)
    extractZipFile(coursierZipPath, coursierParentDir)
    renameIfNeeded(coursierExtractedPath, coursierDestination)

    coursierDestination.toFile().setExecutable(true)
  }

  private fun calculateCoursierZipName() = when (OS.current) {
    OS.WINDOWS -> "cs.zip"
    else -> "cs.gz"
  }

  private fun calculateCoursierExtractedName() = when (OS.current) {
    OS.WINDOWS -> "cs-x86_64-pc-win32.exe"
    else -> "cs"
  }

  private fun calculateCoursierUrl() = when (OS.current) {
    OS.LINUX_ARM -> "https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-pc-linux.gz"
    OS.LINUX_INTEL -> "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz"
    OS.MAC_INTEL -> "https://github.com/coursier/launchers/raw/master/cs-x86_64-apple-darwin.gz"
    OS.MAC_ARM -> "https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-apple-darwin.gz"
    OS.WINDOWS -> "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-win32.zip"
    else -> throw UnsupportedOperationException("Unsupported OS")
  }

  private fun downloadZipFile(downloadUrl: String, path: Path) =
    Files.copy(URL(downloadUrl).openStream(), path, StandardCopyOption.REPLACE_EXISTING)

  private fun extractZipFile(zipPath: Path, workingDir: Path) =
    calculateExtractCommand(zipPath).executeCommand(workingDir.toFile())

  private fun renameIfNeeded(srcPath: Path, destPath: Path) {
    if (destPath != srcPath) Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING)
  }

  private fun calculateExtractCommand(zipPath: Path): List<String> =
    when (OS.current) {
      OS.WINDOWS -> listOf("tar", "-xf", "$zipPath")
      else -> listOf("gzip", "-d", "$zipPath")
    }

  private fun List<String>.executeCommand(workingDir: File = File(".")) =
    ProcessBuilder(this)
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .withRealEnvs()
      .start()
      .waitFor()
}

public enum class OS {
  WINDOWS,
  LINUX_ARM,
  LINUX_INTEL,
  MAC_INTEL,
  MAC_ARM;
  public companion object {
    public val current: OS?
      get() {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        return when {
          osName.startsWith("mac") && osArch.startsWith("x86") -> MAC_INTEL
          osName.startsWith("mac") && osArch.startsWith("aarch64") -> MAC_ARM
          osName.startsWith("linux") && osArch.startsWith("x86") -> LINUX_INTEL
          osName.startsWith("linux") && osArch.startsWith("aarch64") -> LINUX_ARM
          osName.startsWith("windows") -> WINDOWS
          else -> null
        }
      }
  }
}

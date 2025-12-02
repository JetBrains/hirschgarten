package org.jetbrains.bazel.intellij

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SlowOperations
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.idea.devkit.DevKitBundle
import org.jetbrains.idea.devkit.projectRoots.IdeaJdk
import org.jetbrains.idea.devkit.projectRoots.Sandbox
import org.jetbrains.idea.devkit.run.IdeaLicenseHelper
import org.jetbrains.idea.devkit.run.loadProductInfo
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

internal val INTELLIJ_PLUGIN_SANDBOX_KEY: Key<Path> = Key.create("INTELLIJ_PLUGIN_SANDBOX_KEY")

class IntellijPluginRunHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        BuildPluginBeforeRunTaskProvider().createTask(configuration),
        CopyPluginToSandboxBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val state: IntellijPluginRunHandlerState = IntellijPluginRunHandlerState()

  override val name: String
    get() = "IntelliJ Plugin Run Handler"

  // Mostly copied from org.jetbrains.idea.devkit.run.PluginRunConfiguration
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val ideaJdk =
      findIdeaJdk(state.intellijSdkName) ?: throw ExecutionException(
        BazelPluginBundle.message(
          "console.task.exception.no.intellij.platform.plugin.sdk",
        ),
      )

    var sandboxHome =
      (ideaJdk.sdkAdditionalData as Sandbox).sandboxHome
        ?: throw ExecutionException(DevKitBundle.message("sandbox.no.configured"))

    try {
      sandboxHome = File(sandboxHome).canonicalPath
    } catch (e: IOException) {
      throw ExecutionException(DevKitBundle.message("sandbox.no.configured"))
    }
    val canonicalSandbox = sandboxHome

    environment.putUserData(INTELLIJ_PLUGIN_SANDBOX_KEY, Path.of(canonicalSandbox).resolve("plugins"))

    // copy license from running instance of idea
    IdeaLicenseHelper.copyIDEALicense(sandboxHome)

    return object : JavaCommandLineState(environment) {
      override fun createJavaParameters(): JavaParameters {
        val ideaJdkHome = checkNotNull(ideaJdk.homePath)

        val params = JavaParameters()
        fillParameterList(params.programParametersList, state.programArguments)

        val vm = params.vmParametersList

        fillParameterList(vm, state.javaVmOptions)

        vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, canonicalSandbox + File.separator + "config")
        vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, canonicalSandbox + File.separator + "system")
        vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, canonicalSandbox + File.separator + "plugins")

        if (!vm.hasProperty("jdk.module.illegalAccess.silent")) {
          vm.defineProperty("jdk.module.illegalAccess.silent", "true")
        }

        // use product-info.json values if found, otherwise fallback to defaults
        val productInfo =
          loadProductInfo(ideaJdkHome) ?: throw ExecutionException(BazelPluginBundle.message("plugin.runner.idea.product.null"))
        productInfo.getCurrentLaunch().additionalJvmArguments.forEach { item ->
          vm.add(resolveIdeHomeVariable(item, ideaJdkHome))
        }

        if (SystemInfo.isMac) {
          vm.defineProperty("apple.awt.fileDialogForDirectories", "true")
        }

        vm.defineProperty(SlowOperations.IDEA_PLUGIN_SANDBOX_MODE, "true")

        params.workingDirectory = ideaJdkHome + File.separator + "bin" + File.separator
        params.jdk = ideaJdk

        val additionalBootClassPathJarNames = listOf("nio-fs.jar") // For some reason it's missing from the ProductInfo parsed by DevKit
        for (path in productInfo.getCurrentLaunch().bootClassPathJarNames + additionalBootClassPathJarNames) {
          params.classPath.add(ideaJdkHome + FileUtil.toSystemDependentName("/lib/$path"))
        }

        for (moduleJarPath in productInfo.getProductModuleJarPaths()) {
          params.classPath.add(ideaJdkHome + FileUtil.toSystemIndependentName("/$moduleJarPath"))
        }

        params.classPath.addFirst((ideaJdk.sdkType as JavaSdkType).getToolsPath(ideaJdk))

        params.mainClass = "com.intellij.idea.Main"

        return params
      }
    }
  }

  // Copied from ProductInfo.kt to fix an exception, remove when the fix arrives into the DevKit plugin
  fun resolveIdeHomeVariable(path: String, ideHome: String) =
    path
      .replace("\$APP_PACKAGE", ideHome)
      .replace("\$IDE_HOME", ideHome)
      .replace("%IDE_HOME%", ideHome)
      .replace("Contents/Contents", "Contents")
      .let { entry ->
        val value = entry.split("=").getOrNull(1) ?: entry
        when {
          runCatching { Path(value).exists() }.getOrElse { false } -> entry
          else -> entry.replace("/Contents", "")
        }
      }

  private fun fillParameterList(parameterList: ParametersList, parameters: String?) {
    if (!parameters.isNullOrEmpty()) {
      for (parameter in parameters.split(" ")) {
        if (parameter.isNotEmpty()) {
          parameterList.add(parameter)
        }
      }
    }
  }

  private fun findIdeaJdk(name: String?): Sdk? {
    val jdkType = IdeaJdk.getInstance()
    val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType)
    return if (name != null) {
      jdks.firstOrNull { it.name == name }
    } else {
      // Name is not set - choose the newest IdeaJdk
      jdks.maxWithOrNull(jdkType.comparator)
    }
  }
}

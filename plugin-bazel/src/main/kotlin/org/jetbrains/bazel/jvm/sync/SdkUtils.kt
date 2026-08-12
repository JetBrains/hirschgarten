package org.jetbrains.bazel.jvm.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToBaseJdkName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private val logger = Logger.getInstance(SdkUtils::class.java)

internal object SdkUtils {
  suspend fun addJdkIfNeeded(projectName: String, javaHome: Path) {
    // Resolve through JVM wrapper scripts if javaHome is not a valid JDK directory.
    // Bazel JVM wrappers (e.g., jvm_wrapper_runtime) create a java_runtime whose java_home
    // points to a directory containing only bin/java (a shell script), not a full JDK.
    // In that case, parse the wrapper to find the real JDK path.
    val resolvedHome = resolveJavaHome(javaHome)
    val resolvedJdkName = projectName.projectNameToJdkName(resolvedHome)
    // Normalize the JDK path, because some code in the platform compares paths using `startsWith`, e.g.
    // https://github.com/JetBrains/intellij-community/blob/b41a4084da5521effedd334e28896fd9d07410da/java/codeserver/core/src/com/intellij/java/codeserver/core/JpmsModuleAccessInfo.kt#L216
    val path = resolvedHome.normalize().toString()
    val jdk = ExternalSystemJdkProvider.getInstance().createJdk(resolvedJdkName, path)
    addSdkIfNeeded(jdk)

    // Module entities (via ModuleDetailsToJavaModuleTransformer) compute their SDK name from the
    // raw javaHome (wrapper path), while addJdkIfNeeded registers under the resolved path name.
    // To bridge this gap, also register an alias SDK under the original wrapper-path name so that
    // module-level SDK dependencies can resolve and code does not show red.
    if (resolvedHome != javaHome) {
      val originalJdkName = projectName.projectNameToJdkName(javaHome)
      val aliasJdk = ExternalSystemJdkProvider.getInstance().createJdk(originalJdkName, path)
      addSdkIfNeeded(aliasJdk)
    }
  }

  /**
   * If [javaHome] is a valid JDK home, returns it as-is.
   * Otherwise, attempts to resolve the real JDK through multiple strategies:
   * 1. Parse the wrapper script at `{javaHome}/bin/java` for exec paths
   * 2. Scan `external/` under the Bazel exec root for a matching JDK
   *
   * Internal so callers that need to compute a stable SDK name (e.g. for [setProjectSdk]) can
   * resolve the path through the same logic used by [addJdkIfNeeded].
   */
  internal fun resolveJavaHome(javaHome: Path): Path {
    if (javaSdkInstance.isValidSdkHome(javaHome.normalize().toString())) {
      return javaHome
    }
    logger.info("javaHome is not a valid JDK, attempting to resolve: $javaHome")
    val execRoot = findExecRoot(javaHome)

    // Strategy 1: Read the wrapper script if it exists
    val javaBin = javaHome.resolve("bin/java")
    if (javaBin.isRegularFile()) {
      try {
        val resolved = resolveRealJdkFromWrapper(javaBin.readText(), execRoot)
        if (resolved != null) return resolved
      } catch (e: Exception) {
        logger.debug("Failed to parse wrapper script: $javaBin", e)
      }
    }

    // Strategy 2: Scan external/ for a JDK matching the version hint in the path name
    if (execRoot != null) {
      val resolved = findJdkInExternalDir(execRoot, javaHome)
      if (resolved != null) return resolved
    }

    return javaHome
  }

  /**
   * Parses a JVM wrapper shell script to extract the real JDK home path.
   */
  private fun resolveRealJdkFromWrapper(scriptContent: String, execRoot: Path?): Path? {
    if (execRoot == null) return null
    // Match exec lines like: exec "path/to/bin/java" or exec path/to/bin/java
    val execPattern = Regex("""exec\s+["']?([^"'\s]+/bin/java)["']?""")
    for (match in execPattern.findAll(scriptContent)) {
      val javaPath = match.groupValues[1]
      // Skip lines with variable substitutions (e.g., $JAVA_RUNFILES)
      if ('$' in javaPath || '{' in javaPath) continue
      val resolvedJava = execRoot.resolve(javaPath)
      val resolvedHome = resolvedJava.parent?.parent // bin/java -> jdk_home
      if (resolvedHome != null && resolvedHome.exists() &&
        javaSdkInstance.isValidSdkHome(resolvedHome.normalize().toString())
      ) {
        logger.info("Resolved JVM wrapper to real JDK via script: $resolvedHome")
        return resolvedHome
      }
    }
    return null
  }

  /**
   * Scans the `external/` directory under the Bazel exec root for a valid JDK.
   * Uses the wrapper path name to infer the JDK version (e.g., "jdk21" from
   * "jdk21_jvm_wrapper_wrapper_script") and looks for matching directories.
   */
  private fun findJdkInExternalDir(execRoot: Path, wrapperHome: Path): Path? {
    val externalDir = execRoot.resolve("external")
    if (!externalDir.exists()) return null

    // Extract version hint from the wrapper path (e.g., "jdk21" from "jdk21_jvm_wrapper_wrapper_script")
    val wrapperName = wrapperHome.fileName?.toString() ?: ""
    val versionHint = Regex("""jdk(\d+)""").find(wrapperName)?.groupValues?.get(1)

    val candidates = externalDir.toFile().listFiles { file ->
      file.isDirectory && file.name.contains("jdk", ignoreCase = true)
    } ?: return null

    // Prefer candidates matching the version hint
    val sorted = if (versionHint != null) {
      candidates.sortedByDescending { it.name.contains(versionHint) }
    } else {
      candidates.toList()
    }

    for (candidate in sorted) {
      val candidatePath = candidate.toPath()
      if (javaSdkInstance.isValidSdkHome(candidatePath.normalize().toString())) {
        logger.info("Resolved JVM wrapper to real JDK via external/ scan: $candidatePath")
        return candidatePath
      }
    }
    return null
  }

  private fun findExecRoot(path: Path): Path? {
    var current = path
    while (current.parent != null) {
      if (current.fileName?.toString() == "_main" &&
        current.parent?.fileName?.toString() == "execroot"
      ) {
        return current
      }
      current = current.parent
    }
    return null
  }

  suspend fun addSdkIfNeeded(sdk: Sdk) {
    val sdkTable = ProjectJdkTable.getInstance()
    val existingSdk = sdkTable.findJdk(sdk.name, sdk.sdkType.name)
    if (existingSdk == null || existingSdk.homePath != sdk.homePath) {
      writeAction {
        existingSdk?.let { sdkTable.removeJdk(existingSdk) }
        sdkTable.addJdk(sdk)
      }
    }
  }

  suspend fun cleanUpInvalidJdks(project: Project) {
    val sdkTable = ProjectJdkTable.getInstance()
    val jdkPrefix = project.bazelProjectName.projectNameToBaseJdkName()
    getAllAvailableJdks()
      .filter { it.name.startsWith(jdkPrefix) && !isValidJdk(it) }
      .let { invalidJdks ->
        writeAction {
          invalidJdks.forEach { sdkTable.removeJdk(it) }
        }
      }

    // Removed the project SDK cleanup logic, as we actually rely on it for SDK inheritance to avoid repetitive SDK import & indexing
  }

  private fun isValidJdk(sdk: Sdk): Boolean {
    val homePath = sdk.homePath ?: return false
    return javaSdkInstance.isValidSdkHome(homePath) &&
      (sdk as? ProjectJdkImpl)?.rootProvider?.getUrls(OrderRootType.CLASSES)?.size != 0
  }

  fun getProjectJdkOrMostRecentJdk(project: Project): Sdk? =
    ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType == javaSdkInstance }
      ?: getMostRecentJdk()

  suspend fun setProjectSdk(project: Project, jdkName: String?) {
    if (jdkName == null) return

    val sdkTable = ProjectJdkTable.getInstance()
    val sdk = sdkTable.findJdk(jdkName, javaSdkInstance.name) ?: return

    val projectRootManager = ProjectRootManager.getInstance(project)
    if (projectRootManager.projectSdk?.name != jdkName) {
      writeAction {
        projectRootManager.projectSdk = sdk
      }
    }
  }

  private fun getMostRecentJdk(): Sdk? = getAllAvailableJdks().maxWithOrNull(javaSdkInstance.comparator)

  private fun getAllAvailableJdks(): List<Sdk> = ProjectJdkTable.getInstance().getSdksOfType(javaSdkInstance)

  private val javaSdkInstance: JavaSdk
    get() = JavaSdk.getInstance()
}

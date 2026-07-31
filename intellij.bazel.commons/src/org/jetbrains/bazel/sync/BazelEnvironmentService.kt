package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.eel.channels.EelDelicateApi
import com.intellij.platform.eel.environmentVariables
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApi
import com.intellij.util.ShellEnvironmentReader
import com.intellij.util.system.OS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<BazelEnvironmentService>()

/**
 * Fetches the environment of a login shell to avoid cases where `PATH` is different in IDEA and in terminal (BAZEL-883).
 * [com.intellij.util.EnvironmentUtil.getEnvironmentMap] already does this,
 * but only on macOS and only if IDEA wasn't launched from terminal or as a dev build (see com.intellij.platform.ide.bootstrap.StartupUtil#shouldLoadShellEnv).
 * For Bazel plugin we want to skip these conditions and always run Bazel as if it ran in the terminal.
 * @see ShellEnvironmentReader
 */
@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class BazelEnvironmentService(project: Project, scope: CoroutineScope) {
  @OptIn(EelDelicateApi::class)
  private val environmentDeferred: Deferred<Map<String, String>> = scope.async(Dispatchers.IO) {
    try {
      withTimeout(5.seconds) {
        // Using interactive login shell here to make sure the environment variables match the user's terminal exactly.
        // Non-interactive shells miss environment variables defined in files like ~/.bashrc or ~/.zshrc
        project.getEelDescriptor().toEelApi().exec.environmentVariables().loginInteractive().eelIt().await()
      }
    }
    catch (e: Throwable) {
      LOG.warn(e)
      System.getenv()
    }
  }

  suspend fun getEnvironment(): Map<String, String> =
    environmentDeferred.await()

  suspend fun findInPath(vararg executableNames: String): Path? {
    return getEnvironment()["PATH"]
      ?.split(File.pathSeparator)
      .orEmpty()
      .asSequence()
      .flatMap { pathStr ->
        executableNames.mapNotNull { executableName ->
          val actualExecutableName = if (OS.CURRENT == OS.Windows) "$executableName.exe" else executableName
          val path = Path.of(pathStr, actualExecutableName)
          path.takeIf { it.exists() && path.isExecutable() }
        }
      }
      .firstOrNull()
  }

  internal class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
      if (project.isBazelProject) {
        getInstance(project)  // Make sure environmentDeferred starts its calculation ASAP
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelEnvironmentService = project.service()
  }
}

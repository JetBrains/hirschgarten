package org.jetbrains.bazel.bazelrunner

import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags

/**
 * Masks secrets in the environment and in the arguments of a Bazel command, so that a log does not show them.
 *
 * A variable with a sensitive name, such as `GITHUB_TOKEN`, gets its whole value masked.
 * In all other values, the user info of a URL is masked, such as `user:password` in `https://user:password@host`.
 */
@ApiStatus.Internal
object BazelLogMasking {
  private const val MASK = "******"

  private val COMMON_SENSITIVE_PATTERNS = Regex(
    pattern = "SECRET|PASSWORD|PASSWD|PASSPHRASE|TOKEN|CREDENTIAL|API_?KEY|_KEY$|_AUTH$",
    option = RegexOption.IGNORE_CASE,
  )

  /** Proxy variables. Their value is one URL, which can have no scheme, such as `user:password@host:port`. */
  private val KNOWN_SENSITIVE_NAMES = setOf("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY")

  /** RFC 3986, section 3.1 */
  private const val SCHEME = "[a-zA-Z][a-zA-Z0-9+.-]*"

  /** RFC 3986, section 3.2 */
  private const val USER_INFO = """[^\s/?#\[\]@]+"""

  /** Finds the user info of each URL in a text, such as `user:password` in `https://user:password@host`. */
  private val URL_USER_INFO = Regex("""($SCHEME://)$USER_INFO@""")

  /** The flags that set a variable as `NAME=value`. */
  private val ENVIRONMENT_FLAGS = setOf("--action_env", "--host_action_env", "--test_env", "--repo_env")

  /**
   * The flags that Bazel tags `fully_redacted_in_logs`: the option "may contain sensitive credentials and should be redacted in logs".
   * Their whole value is masked.
   *
   * See the flags and the option metadata tags in https://bazel.build/reference/command-line-reference.
   */
  private val FULLY_REDACTED_FLAGS = setOf(
    "--bes_header",
    "--remote_header",
    "--remote_cache_header",
    "--remote_exec_header",
    "--remote_downloader_header",
    "--google_credentials",
    "--tls_client_key",
  )

  /** Masks the value of a variable. Returns the value as it is when [BazelFeatureFlags.maskSecretsInLog] is off. */
  fun maskEnvValue(name: String, value: String): String = when {
      BazelFeatureFlags.maskSecretsInLog -> maskVariableValue(name, value)
      else -> value
  }

  /**
   * Masks the arguments of a command. Returns the arguments as they are when [BazelFeatureFlags.maskSecretsInLog] is off.
   *
   * A flag from [ENVIRONMENT_FLAGS] or [FULLY_REDACTED_FLAGS] can have its value in the same argument, as `--flag=value`,
   * or in the next argument, as `--flag value`.
   */
  fun maskArguments(arguments: List<String>): List<String> = when {
      BazelFeatureFlags.maskSecretsInLog -> maskEachArgument(arguments)
      else -> arguments
  }

  private fun maskVariableValue(name: String, value: String): String = when {
    COMMON_SENSITIVE_PATTERNS.containsMatchIn(name) -> MASK
    name.uppercase() in KNOWN_SENSITIVE_NAMES && !URLUtil.containsScheme(value) -> value.withTemporaryScheme { it.maskUrlUserInfo() }
    else -> value.maskUrlUserInfo()
  }

  private fun maskEachArgument(arguments: List<String>): List<String> =
    // The empty first item is the previous argument of the first argument. It is not a flag.
    (listOf("") + arguments).zipWithNext { previous, argument ->
      when (previous) {
        in FULLY_REDACTED_FLAGS -> MASK
        in ENVIRONMENT_FLAGS -> maskVariable(argument)
        else -> maskArgument(argument)
      }
    }

  /** Masks one argument, which is either `--flag=value` or any other text. */
  private fun maskArgument(argument: String): String {
    val (flag, value) = argument.splitAtFirstEquals() ?: return argument.maskUrlUserInfo()
    return when (flag) {
      in FULLY_REDACTED_FLAGS -> "$flag=$MASK"
      in ENVIRONMENT_FLAGS -> "$flag=${maskVariable(value)}"
      else -> argument.maskUrlUserInfo()
    }
  }

  /** Masks the `NAME=value` of an environment flag. A value without `=`, such as an inherited variable `NAME`, has no secret. */
  private fun maskVariable(variable: String): String {
    val (name, value) = variable.splitAtFirstEquals() ?: return variable
    return "$name=${maskVariableValue(name, value)}"
  }

  /** Splits `name=value` at the first `=`, or returns `null` when there is no `=`. */
  private fun String.splitAtFirstEquals(): Pair<String, String>? {
    val index = indexOf('=')
    return when {
        index < 0 -> null
        else -> substring(0, index) to substring(index + 1)
    }
  }

  private fun String.maskUrlUserInfo(): String = replace(URL_USER_INFO, "$1$MASK@")

  private inline fun String.withTemporaryScheme(
    transform: (String) -> String,
  ): String = transform(URLUtil.addSchemaIfMissing(this)).removePrefix(URLUtil.HTTPS_PROTOCOL + URLUtil.SCHEME_SEPARATOR)
}

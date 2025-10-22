package org.jetbrains.bazel.sdkcompat

import com.intellij.debugger.engine.AsyncStacksUtils
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Key
import java.util.concurrent.atomic.AtomicReference

// Minimum coroutines library version (1.3.7-255) parsed to be used for comparison. Coroutines
// debugging is not available in earlier versions of the coroutines library.
private val MIN_LIB_VERSION = listOf(1, 3, 7, 255)
private val LIB_PATTERN = "(kotlinx-coroutines-core(-jvm)?)-(\\d[\\w.\\-]+)?\\.jar".toRegex()

private fun createJvmFlag(flag: String) = "--jvmopt=$flag"

private fun createJavaAgentFlag(jarPath: String) = createJvmFlag("-javaagent:$jarPath")

val COROUTINE_JVM_FLAGS_KEY = Key.create<AtomicReference<List<String>>>("bazel.coroutine.jvm.flags")

fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, module: Module) {
  val kotlinxCoroutinePath =
    OrderEnumerator
      .orderEntries(module)
      .recursively()
      .classes()
      .pathsList.pathList
      .firstOrNull { isKotlinxCoroutinesLib(it) }
      ?: return

  val coroutineJvmFlag = createJavaAgentFlag(kotlinxCoroutinePath)
  val javaParameters = JavaParameters()
  AsyncStacksUtils.addDebuggerAgent(javaParameters, environment.project, false)
  environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY).set(
    listOfNotNull(
      coroutineJvmFlag,
    ) + javaParameters.vmParametersList.parameters.map { createJvmFlag(it) },
  )
}

private fun isKotlinxCoroutinesLib(jarPath: String): Boolean {
  val m = LIB_PATTERN.find(jarPath)
  if (m != null && m.groupValues.size >= 3) {
    val version = m.groupValues[3]
    return isValidVersion(version)
  }
  return false
}

private fun isValidVersion(libVersion: String): Boolean {
  val versionParts = libVersion.split(regex = "[.-]".toRegex())
  val maxLength: Int = MIN_LIB_VERSION.size.coerceAtLeast(versionParts.size)
  for (i in 0..<maxLength) {
    val versionPart = if (i < versionParts.size) versionParts[i].toInt() else 0
    val minVersionPart: Int = if (i < MIN_LIB_VERSION.size) MIN_LIB_VERSION[i] else 0
    val res = versionPart.compareTo(minVersionPart)
    if (res != 0) {
      return res > 0
    }
  }
  return false
}

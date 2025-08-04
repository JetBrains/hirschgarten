package org.jetbrains.bazel.commons

interface SystemInfoProvider {
  val isWindows: Boolean
  val isMac: Boolean
  val isLinux: Boolean
  val isAarch64: Boolean

  companion object {
    private lateinit var instance: SystemInfoProvider

    fun getInstance(): SystemInfoProvider = instance

    fun provideSystemInfoProvider(spawner: SystemInfoProvider) {
      instance = spawner
    }
  }
}

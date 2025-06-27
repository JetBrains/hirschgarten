package org.jetbrains.bazel.commons

interface SystemInfoProvider {
  val isWindows: Boolean
  val isMac: Boolean
  val isLinux: Boolean
  val isAarch64: Boolean

  companion object {
    private lateinit var instance: SystemInfoProvider

    fun getInstance(): SystemInfoProvider =
      if (Companion::instance.isInitialized) instance else throw IllegalStateException("SystemInfoProvider not initialized")

    fun provideSystemInfoProvider(spawner: SystemInfoProvider) {
      instance = spawner
    }
  }
}

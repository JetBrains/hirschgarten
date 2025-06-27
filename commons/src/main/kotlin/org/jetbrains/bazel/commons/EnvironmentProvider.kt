package org.jetbrains.bazel.commons


interface EnvironmentProvider {
  fun getValue(name: String): String?

  companion object {
    private lateinit var instance: EnvironmentProvider

    fun getInstance(): EnvironmentProvider =
      if (Companion::instance.isInitialized) instance else throw IllegalStateException("EnvironmentProvider not initialized")

    fun provideEnvironmentProvider(spawner: EnvironmentProvider) {
      instance = spawner
    }
  }


}

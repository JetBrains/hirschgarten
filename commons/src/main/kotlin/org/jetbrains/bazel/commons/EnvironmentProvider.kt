package org.jetbrains.bazel.commons

interface EnvironmentProvider {
  fun getValue(name: String): String?

  companion object {
    private var instance: EnvironmentProvider? = null

    fun getInstance(): EnvironmentProvider = instance ?: error("No environment provider found")

    fun provideEnvironmentProvider(spawner: EnvironmentProvider) {
      instance = spawner
    }
  }
}

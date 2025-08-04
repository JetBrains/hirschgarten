package org.jetbrains.bazel.commons

interface EnvironmentProvider {
  fun getValue(name: String): String?

  companion object {
    private lateinit var instance: EnvironmentProvider

    fun getInstance(): EnvironmentProvider = instance

    fun provideEnvironmentProvider(spawner: EnvironmentProvider) {
      instance = spawner
    }
  }
}

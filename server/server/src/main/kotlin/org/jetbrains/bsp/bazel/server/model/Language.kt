package org.jetbrains.bsp.bazel.server.model

enum class Language(
  val id: String,
  val extensions: Set<String>,
  val targetKinds: Set<String> = hashSetOf(),
  dependentNames: Set<String> = hashSetOf(),
) {
  SCALA("scala", hashSetOf(".scala")),
  JAVA(
    "java",
    hashSetOf(".java"),
    targetKinds =
      setOf(
        "java_library",
        "java_binary",
        "java_test",
        "intellij_plugin_debug_target", // a workaround to register this target type as Java module in IntelliJ IDEA
      ),
  ),
  KOTLIN("kotlin", hashSetOf(".kt"), setOf("kt_jvm_binary", "kt_jvm_library", "kt_jvm_test"), hashSetOf(JAVA.id)),
  CPP("cpp", hashSetOf(".C", ".cc", ".cpp", ".CPP", ".c++", ".cp", "cxx", ".h", ".hpp")),
  PYTHON("python", hashSetOf(".py")),
  THRIFT("thrift", hashSetOf(".thrift")),
  RUST("rust", hashSetOf(".rs")),
  ANDROID(
    "android",
    emptySet(),
    setOf("android_binary", "android_library", "android_local_test"),
    hashSetOf(JAVA.id),
  ),
  GO("go", hashSetOf(".go"), setOf("go_binary")),
  ;

  val allNames: Set<String> = dependentNames + id

  companion object {
    private val ALL = values().toSet()

    fun all() = ALL
  }
}

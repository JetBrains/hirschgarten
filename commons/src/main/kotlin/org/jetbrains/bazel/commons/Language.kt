package org.jetbrains.bazel.commons

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
  KOTLIN(
    "kotlin",
    hashSetOf(".kt"),
    setOf(
      "kt_jvm_binary",
      "kt_jvm_library",
      "kt_jvm_test",
      // rules_jvm from IntelliJ monorepo
      "jvm_library",
      "jvm_binary",
      "jvm_resources",
    ),
    hashSetOf(JAVA.id),
  ),
  CPP("cpp", hashSetOf(".C", ".cc", ".cpp", ".CPP", ".c++", ".cp", "cxx", ".h", ".hpp")),
  PYTHON("python", hashSetOf(".py")),
  THRIFT("thrift", hashSetOf(".thrift")),
  ANDROID(
    "android",
    emptySet(),
    setOf("android_binary", "android_library", "android_local_test", "kt_android_library", "kt_android_local_test"),
    hashSetOf(JAVA.id),
  ),
  GO("go", hashSetOf(".go"), setOf("go_binary", "go_library", "go_test")),
  ;

  val allNames: Set<String> = dependentNames + id

  companion object {
    private val ALL = Language.entries.toSet()

    fun all() = ALL

    fun allOfKind(targetKind: String, transitiveCompileTimeJarsTargetKinds: Set<String> = emptySet()): Set<Language> =
      all()
        .filterTo(mutableSetOf()) { it.targetKinds.contains(targetKind) }
        .apply { if (targetKind in transitiveCompileTimeJarsTargetKinds) add(JAVA) }

    fun allOfSource(path: String): Set<Language> = all().filter { lang -> lang.extensions.any { path.endsWith(it) } }.toHashSet()
  }
}

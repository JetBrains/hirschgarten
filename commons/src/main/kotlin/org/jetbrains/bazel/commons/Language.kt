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
  PYTHON("python", hashSetOf(".py")),
  THRIFT("thrift", hashSetOf(".thrift")),
  GO("go", hashSetOf(".go"), setOf("go_binary")),
  ;

  val allNames: Set<String> = dependentNames + id

  companion object {
    private val ALL = Language.entries.toSet()

    fun all() = ALL

    fun allOfKind(targetKind: String): Set<Language> =
      all()
        .filterTo(mutableSetOf()) { it.targetKinds.contains(targetKind) }

    fun allOfSource(path: String): Set<Language> = all().filter { lang -> lang.extensions.any { path.endsWith(it) } }.toHashSet()
  }
}

package org.jetbrains.bazel.languages.starlark.bazel

object BazelNativeRules {
  val JAVA_NATIVE_RULES = setOf(
    "java_library",
    "java_test",
    "java_junit5_test",
    "java_test_suite",
    "java_binary",
    "java_import",
    "java_toolchain",
    "java_proto_library",
    "java_lite_proto_library",
    "java_mutable_proto_library",
    "java_plugin",
    "java_wrap_cc",
    "gwt_application",
    "gwt_host",
    "gwt_module",
    "gwt_test",
    "java_web_test",
  )
}

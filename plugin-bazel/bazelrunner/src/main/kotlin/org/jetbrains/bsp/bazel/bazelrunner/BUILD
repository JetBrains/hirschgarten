load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bazelrunner",
    srcs = glob(["*.kt"]),
    resources = ["//plugin-bazel/log4j_config"],
    visibility = ["//plugin-bazel/bazelrunner:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//plugin-bazel/bazelrunner/src/main/kotlin/org/jetbrains/bsp/bazel/bazelrunner/outputs",
        "//plugin-bazel/bazelrunner/src/main/kotlin/org/jetbrains/bsp/bazel/bazelrunner/params",
        "//plugin-bazel/bazelrunner/src/main/kotlin/org/jetbrains/bsp/bazel/bazelrunner/utils",
        "//plugin-bazel/commons",
        "//plugin-bazel/executioncontext/workspacecontext",
        "//plugin-bazel/logger",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)

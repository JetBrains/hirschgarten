load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "bazelrunner",
    srcs = glob(["*.kt"]),
    resources = ["//server/log4j_config"],
    visibility = ["//server/bazelrunner:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/outputs",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/params",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/utils",
        "//server/commons",
        "//server/logger",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/info",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils:structured_files_utils",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

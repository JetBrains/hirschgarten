load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "provider",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/bazelrunner:params",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "@maven//:commons_io_commons_io",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)

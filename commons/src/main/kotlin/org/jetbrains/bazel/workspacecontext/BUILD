load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "workspacecontext",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "@maven//:commons_io_commons_io",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

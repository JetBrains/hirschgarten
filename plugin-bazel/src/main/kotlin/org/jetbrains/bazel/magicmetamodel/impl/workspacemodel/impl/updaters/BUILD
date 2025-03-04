load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "updaters",
    srcs = glob(["**/*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
        "@maven//:com_google_code_gson_gson",
    ],
)

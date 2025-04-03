load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "dialogs",
    srcs = glob(["**/*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//server/bazelrunner",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

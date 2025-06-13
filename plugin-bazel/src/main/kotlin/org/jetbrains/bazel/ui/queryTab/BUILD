#load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "queryTab",
    srcs = glob(["**/*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/bazel",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/bazelrunner",
    ],
)

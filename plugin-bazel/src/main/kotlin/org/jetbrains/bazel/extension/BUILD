load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "extension",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/label",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/debug",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/extensionPoints",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/settings",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/settings:bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/widgets",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//intellij_platform_sdk:plugin_api",
    ],
)

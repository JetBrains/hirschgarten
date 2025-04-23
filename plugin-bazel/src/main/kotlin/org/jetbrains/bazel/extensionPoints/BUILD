load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "extensionPoints",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/jpsCompilation/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/android",
        "//rules_intellij/third_party/code_with_me",
        "//rules_intellij/third_party/devkit",
        "//rules_intellij/third_party/performance",
        "//rules_intellij/third_party/python",
    ],
)

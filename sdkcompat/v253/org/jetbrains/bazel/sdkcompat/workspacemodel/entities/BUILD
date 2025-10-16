load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

kt_jvm_library(
    name = "entities",
    srcs = select_for_plugin_api({
        "intellij-2025.1": ["Dummy.kt"],
        "intellij-2025.2": ["Dummy.kt"],
        "intellij-2025.3": glob([
            "*.kt",
            "impl/*.kt",
        ]),
    }),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

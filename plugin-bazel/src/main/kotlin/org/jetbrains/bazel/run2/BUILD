load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "run2",
    srcs = glob([
        "**/*.kt",
        "**/*.java",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/bep",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/io",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/targetKind",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/taskEvents",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//sdkcompat",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

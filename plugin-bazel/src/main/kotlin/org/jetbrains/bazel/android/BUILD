load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "android",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/startup",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//rules_intellij/third_party/android",
        "//sdkcompat",
        "@maven//:com_google_code_gson_gson",
    ],
)

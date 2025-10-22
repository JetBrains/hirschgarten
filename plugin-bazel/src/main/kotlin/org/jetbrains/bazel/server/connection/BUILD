load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//commons/src/main/kotlin/org/jetbrains/bazel/settings/bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages/bazelversion",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/server/client",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server",
    ],
)

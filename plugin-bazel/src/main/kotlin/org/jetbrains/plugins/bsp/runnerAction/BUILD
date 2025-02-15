load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "runnerAction",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/tasks",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
    ],
)

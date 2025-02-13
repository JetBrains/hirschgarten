load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "runnerAction",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/protocol/src/main/kotlin/org/jetbrains/bsp/protocol/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/tasks",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/target",
    ],
)

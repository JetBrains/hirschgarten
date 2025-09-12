load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "server",
    srcs = glob([
        "**/*.kt",
    ]),
    visibility = [
        #         "//server/bspcli:__subpackages__",
        #         "//server/install:__subpackages__",
        #         "//server/server:__subpackages__",
        "//visibility:public",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//server/bazelrunner",
        "//server/commons",
        "//server/logger",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bep",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/info",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/managers",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bzlmod",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/diagnostics",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

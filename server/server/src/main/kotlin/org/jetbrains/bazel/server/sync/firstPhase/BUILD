load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "firstPhase",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/managers",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils",
                "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/languages",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

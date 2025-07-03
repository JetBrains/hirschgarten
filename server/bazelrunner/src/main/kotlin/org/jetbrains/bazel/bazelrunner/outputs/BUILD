load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "outputs",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner/src/main/kotlin/org/jetbrains/bazel/bazelrunner/utils",
        "//server/commons",
        "//server/logger",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

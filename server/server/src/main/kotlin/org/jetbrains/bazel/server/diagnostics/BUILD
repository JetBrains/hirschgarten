load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "diagnostics",
    srcs = glob(["*.kt"]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/bazelrunner",
        "//server/commons",
            ],
)

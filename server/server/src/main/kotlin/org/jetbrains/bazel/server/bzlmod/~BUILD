load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "bzlmod",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/bazelrunner",
    ],
)

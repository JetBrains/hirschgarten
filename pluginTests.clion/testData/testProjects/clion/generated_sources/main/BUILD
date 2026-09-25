load("@rules_cc//cc:defs.bzl", "cc_binary")

cc_binary(
    name = "main",
    srcs = ["main.cc"],
    deps = ["//lib"],
)

cc_binary(
    name = "main_prebuild",
    srcs = ["main.cc"],
    deps = ["//lib:prebuild"],
)

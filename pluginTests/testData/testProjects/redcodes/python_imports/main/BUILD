load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "main",
    srcs = ["main.py"],
    visibility = ["//visibility:public"],
    deps = [
        "//lib/libA",
        "//tools/helper",
    ],
)

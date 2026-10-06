load("@rules_python//python:defs.bzl", "py_library")

py_library(
    name = "leaf",
    srcs = select({
        "//settings:mode_a": ["a/leaf.py"],
        "//settings:mode_b": ["b/leaf.py"],
    }),
    visibility = ["//visibility:public"],
)

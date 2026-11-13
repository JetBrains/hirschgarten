load("@rules_python//python:defs.bzl", "py_library")

py_library(
    name = "with_imports",
    srcs = ["src/pkg/mod.py"],
    imports = ["src"],
    visibility = ["//visibility:public"],
)

py_library(
    name = "plain",
    srcs = ["plain/pkg2/other.py"],
    visibility = ["//visibility:public"],
)

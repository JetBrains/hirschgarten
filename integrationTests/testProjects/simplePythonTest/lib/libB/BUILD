load("@rules_python//python:defs.bzl", "py_library")

py_library(
    name = "libB",
    visibility = ["//visibility:public"],
    srcs = glob(["src/**/*.py"]),
    imports = ["src"],
)
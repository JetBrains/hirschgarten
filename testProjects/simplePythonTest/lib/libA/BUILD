load("@rules_python//python:defs.bzl", "py_library")

py_library(
    visibility = ["//visibility:public"],
    name = "libA",
    srcs = glob(["src/**/*.py"]),
    imports = ["src"],
    deps = ["//lib/libB"]
)
load("@rules_python//python:defs.bzl", "py_library")

py_library(
    name = "libA",
    srcs = glob(["src/**/*.py"]) + ["util.py"],
    imports = ["src"],
    visibility = ["//visibility:public"],
    deps = ["//lib/libB"],
)

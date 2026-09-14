load("@rules_python//python:defs.bzl", "py_binary", "py_library")

py_library(
    name = "lib",
    srcs = ["lib.py"],
    srcs_version = "PY3",
)

py_binary(
    name = "main",
    srcs = ["main.py"],
    python_version = "PY3",
    deps = [":lib"],
)

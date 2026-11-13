load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "main",
    srcs = ["main.py"],
    deps = [
        "@local_lib//:plain",
        "@local_lib//:with_imports",
    ],
)

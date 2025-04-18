load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "munich",
    srcs = ["munich.py"],
    imports = ["."],
    deps = [
        "//server/example/amsterdam",
        "//server/example/krakow",
    ],
)

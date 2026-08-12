load("@rules_python//python:defs.bzl", "py_library")

py_library(
    name = "my_lib2",
    srcs = [
        "__init__.py",
        "util.py",
    ],
    imports = ["../"],
    visibility = ["//visibility:public"],
    deps = ["//python/libs/my_lib"],
)

load("@pip//:requirements.bzl", "requirement")
load("@rules_python//python:defs.bzl", "py_binary")

py_binary(
    name = "main",
    srcs = ["main.py"],
    deps = [
        # workspace dependency
        "//python/libs/my_lib2",
        # python dependency via pip
        requirement("certifi"),
        requirement("charset-normalizer"),
        requirement("idna"),
        requirement("urllib3"),
        requirement("numpy"),
        # python dependecny via http_archieve
        "@requests",
        # generated source
        "//python/generated:generated_directory_lib",
        "//python/generated:generated_files_lib",
    ],
)

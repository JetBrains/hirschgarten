load("@rules_python//python:defs.bzl", "py_binary")
load("//rules/kotlin:macros.bzl", "kt_library")
load(":integration_test.bzl", "synthetic_bazel_project")

kt_library(
    name = "intellij-bsp",
    neverlink = True,
    exports = [
        "//plugin-bazel/src:plugin-bazel-lib",
    ],
)

py_binary(
    name = "generator",
    srcs = glob(["*.py"]),
    imports = ["."],
    main = "gen.py",
    deps = [],
)

synthetic_bazel_project(
    name = "synthetic_project_medium",
    project_directory = "project_medium",
    project_size = 1000,
)

# bsp_cli_on_large_project(
#     name = "benchmark_medium",
#     bspcli = "//server/bspcli:bspcli",
#     project = ":synthetic_project_medium",
# )

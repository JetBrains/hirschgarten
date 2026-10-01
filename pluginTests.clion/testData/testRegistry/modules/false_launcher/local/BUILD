load(":toolchain.bzl", "false_launcher_toolchain")

exports_files(["launcher.sh"])

false_launcher_toolchain(
    name = "launcher_toolchain",
    binary = ":launcher.sh",
)

toolchain(
    name = "toolchain",
    toolchain = ":launcher_toolchain",
    toolchain_type = "@bazel_tools//tools/launcher:launcher_toolchain_type",
)

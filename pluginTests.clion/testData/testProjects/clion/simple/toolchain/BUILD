load("@rules_cc//cc:cc_binary.bzl", "cc_binary")
load("@rules_cc//cc/toolchains:cc_toolchain.bzl", "cc_toolchain")
load(":toolchain.bzl", "cc_toolchain_config")

filegroup(
    name = "empty",
    srcs = [],
)

cc_toolchain_config(
    name = "false_toolchain_config",
)

cc_toolchain(
    name = "false_cc_toolchain",
    all_files = ":empty",
    ar_files = ":empty",
    compiler_files = ":empty",
    dwp_files = ":empty",
    linker_files = ":empty",
    objcopy_files = ":empty",
    strip_files = ":empty",
    toolchain_config = ":false_toolchain_config",
)

toolchain(
    name = "toolchain",
    exec_compatible_with = [],
    target_compatible_with = [],
    toolchain = ":false_cc_toolchain",
    toolchain_type = "@bazel_tools//tools/cpp:toolchain_type",
)

load("@rules_cc//cc/toolchains:cc_toolchain.bzl", "cc_toolchain")
load("//toolchain:config.bzl", "cc_toolchain_config")

filegroup(
    name = "tools",
    srcs = ["//toolchain:cc-wrapper"],
)

cc_toolchain_config(
    name = "toolchain_config",
    cc = "//toolchain:cc-wrapper",
)

cc_toolchain(
    name = "cc_toolchain",
    all_files = ":tools",
    ar_files = ":tools",
    compiler_files = ":tools",
    dwp_files = ":tools",
    linker_files = ":tools",
    objcopy_files = ":tools",
    strip_files = ":tools",
    toolchain_config = ":toolchain_config",
)

toolchain(
    name = "toolchain",
    exec_compatible_with = [],
    target_compatible_with = [],
    toolchain = ":cc_toolchain",
    toolchain_type = "@bazel_tools//tools/cpp:toolchain_type",
)

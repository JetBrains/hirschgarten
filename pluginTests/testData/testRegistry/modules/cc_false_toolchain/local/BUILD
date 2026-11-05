load("@rules_cc//cc/toolchains:cc_toolchain.bzl", "cc_toolchain")
load(":toolchain.bzl", "cc_toolchain_config")

filegroup(
    name = "empty",
    srcs = [],
)

filegroup(
    name = "compiler",
    srcs = select({
       "@bazel_tools//src/conditions:windows": ["compiler.bat"],
       "//conditions:default": ["compiler.sh"],
    }),
)

cc_toolchain_config(
    name = "toolchain_config",
    compiler = select({
        "@bazel_tools//src/conditions:windows": "compiler.bat",
        "//conditions:default": "compiler.sh",
    }),
)

cc_toolchain(
    name = "cc_toolchain",
    all_files = ":empty",
    ar_files = ":empty",
    compiler_files = ":compiler",
    dwp_files = ":empty",
    linker_files = ":empty",
    objcopy_files = ":empty",
    strip_files = ":empty",
    toolchain_config = ":toolchain_config",
)

toolchain(
    name = "toolchain",
    exec_compatible_with = [],
    target_compatible_with = [],
    toolchain = ":cc_toolchain",
    toolchain_type = "@bazel_tools//tools/cpp:toolchain_type",
)

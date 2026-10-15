load("@rules_cc//cc:cc_toolchain_config_lib.bzl", "tool_path")
load("@rules_cc//cc:defs.bzl", "cc_common")
load("@rules_cc//cc/toolchains:cc_toolchain_config_info.bzl", "CcToolchainConfigInfo")

def _cc_toolchain_config_impl(ctx):
    tool_paths = [
        # compiler info collection needs to invoke the compiler during the sync
        tool_path(name = "gcc", path = "compiler.sh"),
        tool_path(name = "ld", path = "/usr/bin/false"),
        tool_path(name = "ar", path = "/usr/bin/false"),
        tool_path(name = "cpp", path = "/usr/bin/false"),
        tool_path(name = "gcov", path = "/usr/bin/false"),
        tool_path(name = "nm", path = "/usr/bin/false"),
        tool_path(name = "objdump", path = "/usr/bin/false"),
        tool_path(name = "strip", path = "/usr/bin/false"),
    ]

    return cc_common.create_cc_toolchain_config_info(
        ctx = ctx,
        toolchain_identifier = "cc-false-toolchain",
        host_system_name = "local",
        target_system_name = "local",
        target_cpu = "k8",
        target_libc = "unknown",
        compiler = "false",
        abi_version = "unknown",
        abi_libc_version = "unknown",
        features = [],
        tool_paths = tool_paths,
    )

cc_toolchain_config = rule(
    implementation = _cc_toolchain_config_impl,
    attrs = {},
    provides = [CcToolchainConfigInfo],
)

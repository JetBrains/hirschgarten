load("@rules_cc//cc:action_names.bzl", "ACTION_NAMES")
load("@rules_cc//cc:cc_toolchain_config_lib.bzl", "action_config", "tool", "tool_path")
load("@rules_cc//cc:defs.bzl", "cc_common")
load("@rules_cc//cc/toolchains:cc_toolchain_config_info.bzl", "CcToolchainConfigInfo")

def _cc_toolchain_config_impl(ctx):
    # compiler info collection needs to invoke the compiler during the sync
    action_configs = [
        action_config(
            action_name = action_name,
            enabled = True,
            tools = [tool(tool = ctx.file.compiler_stub)],
        )
        for action_name in [ACTION_NAMES.c_compile, ACTION_NAMES.cpp_compile]
    ]

    tool_paths = [
        tool_path(name = "gcc", path = "/usr/bin/false"),
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
        action_configs = action_configs,
        tool_paths = tool_paths,
    )

cc_toolchain_config = rule(
    implementation = _cc_toolchain_config_impl,
    attrs = {
        "compiler_stub": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
    },
    provides = [CcToolchainConfigInfo],
)

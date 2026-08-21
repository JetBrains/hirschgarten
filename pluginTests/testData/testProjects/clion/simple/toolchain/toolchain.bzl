load("@rules_cc//cc:action_names.bzl", "ACTION_NAMES")
load("@rules_cc//cc:cc_toolchain_config_lib.bzl", "env_entry", "env_set", "feature", "flag_group", "flag_set", "tool_path")
load("@rules_cc//cc:defs.bzl", "cc_common")
load("@rules_cc//cc/toolchains:cc_toolchain_config_info.bzl", "CcToolchainConfigInfo")

def _impl(ctx):
    env_feature = feature(
        name = "env",
        enabled = True,
        env_sets = [
            env_set(
                actions = [
                    ACTION_NAMES.c_compile,
                    ACTION_NAMES.cpp_compile,
                ],
                env_entries = [
                    env_entry(
                        key = "ENV_VARIABLE",
                        value = "ENV_VALUE",
                    ),
                ],
            ),
        ],
    )

    flag_feature = feature(
        name = "flags",
        enabled = True,
        flag_sets = [
            flag_set(
                actions = [ACTION_NAMES.c_compile],
                flag_groups = [flag_group(flags = ["-D__DEFINE__", "-std=c17"])],
            ),
            flag_set(
                actions = [ACTION_NAMES.cpp_compile],
                flag_groups = [flag_group(flags = ["-D__DEFINE__", "-std=c++17"])],
            ),
        ],
    )

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
        toolchain_identifier = "false-toolchain",
        host_system_name = "local",
        target_system_name = "local",
        target_cpu = "k8",
        target_libc = "unknown",
        compiler = "false",
        abi_version = "unknown",
        abi_libc_version = "unknown",
        features = [env_feature, flag_feature],
        tool_paths = tool_paths,
    )

cc_toolchain_config = rule(
    implementation = _impl,
    attrs = {},
    provides = [CcToolchainConfigInfo],
)

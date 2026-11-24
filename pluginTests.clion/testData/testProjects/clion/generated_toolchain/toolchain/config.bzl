load("@rules_cc//cc:cc_toolchain_config_lib.bzl", "tool_path")
load("@rules_cc//cc:defs.bzl", "cc_common")
load("@rules_cc//cc/toolchains:cc_toolchain_config_info.bzl", "CcToolchainConfigInfo")

def _cc_toolchain_config_impl(ctx):
    tool_paths = [
        tool_path(name = "gcc", path = ctx.file.cc.path),
        tool_path(name = "ld", path = "/usr/bin/ld"),
        tool_path(name = "ar", path = "/usr/bin/ar"),
        tool_path(name = "cpp", path = "/usr/bin/cpp"),
        tool_path(name = "gcov", path = "/usr/bin/gcov"),
        tool_path(name = "nm", path = "/usr/bin/nm"),
        tool_path(name = "objdump", path = "/usr/bin/objdump"),
        tool_path(name = "strip", path = "/usr/bin/strip"),
    ]

    return cc_common.create_cc_toolchain_config_info(
        cxx_builtin_include_directories = [ "/usr/include", "/usr/lib" ],
        ctx = ctx,
        toolchain_identifier = "cc-wrapped-toolchain",
        host_system_name = "local",
        target_system_name = "local",
        target_cpu = "k8",
        target_libc = "unknown",
        compiler = "wrapped",
        abi_version = "unknown",
        abi_libc_version = "unknown",
        features = [],
        tool_paths = tool_paths,
    )

cc_toolchain_config = rule(
    implementation = _cc_toolchain_config_impl,
    attrs = {
        "cc": attr.label(allow_single_file=True, mandatory=True),
    },
    provides = [CcToolchainConfigInfo],
)
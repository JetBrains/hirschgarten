def _false_launcher_toolchain_impl(ctx):
    return [platform_common.ToolchainInfo(binary = ctx.file.binary)]

false_launcher_toolchain = rule(
    implementation = _false_launcher_toolchain_impl,
    attrs = {
        "binary": attr.label(
            allow_single_file = True,
            mandatory = True,
        ),
    },
)

def _windows_toolchain_impl(_ctx):
    return [platform_common.ToolchainInfo()]

windows_toolchain = rule(implementation = _windows_toolchain_impl)

def _mode_transition_impl(settings, attr):
    return {"//settings:mode": attr.mode}

_mode_transition = transition(
    implementation = _mode_transition_impl,
    inputs = [],
    outputs = ["//settings:mode"],
)

def _mode_binary_impl(ctx):
    out = ctx.actions.declare_file(ctx.label.name + ".marker")
    ctx.actions.write(out, "mode=" + ctx.attr.mode + "\n")
    return [DefaultInfo(files = depset([out]))]

mode_binary = rule(
    implementation = _mode_binary_impl,
    attrs = {
        "mode": attr.string(mandatory = True, values = ["a", "b"]),
        "dep": attr.label(cfg = _mode_transition, mandatory = True),
    },
)

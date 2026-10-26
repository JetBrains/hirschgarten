"""A minimal rule that holds a no-ide target in a custom label attribute."""

def _wrapper_impl(ctx):
    out = ctx.actions.declare_file(ctx.label.name + ".txt")
    ctx.actions.write(out, "wraps " + str(ctx.attr.inner.label))
    return [DefaultInfo(files = depset([out]))]

wrapper = rule(
    implementation = _wrapper_impl,
    attrs = {
        "inner": attr.label(mandatory = True),
    },
)

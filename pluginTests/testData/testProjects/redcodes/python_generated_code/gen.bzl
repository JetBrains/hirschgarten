"""Minimal code generator mimicking dataset/partition style codegen macros."""

def _gen_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.file)
    ctx.actions.write(out, ctx.attr.text)
    return [DefaultInfo(files = depset([out]))]

gen = rule(
    implementation = _gen_impl,
    attrs = {
        "file": attr.string(mandatory = True),
        "text": attr.string(mandatory = True),
    },
)

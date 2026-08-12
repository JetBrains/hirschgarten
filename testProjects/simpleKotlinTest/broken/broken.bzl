def _broken_impl(ctx):
    fail("Should not look at target %s" % (str(ctx.label),))

broken = rule(
    implementation = _broken_impl,
)


def _wrapper(ctx):
   out = ctx.actions.declare_file(ctx.attr.wrap_as)
   cmd = "echo '#!/bin/sh' > $2 && echo 'echo Calling wrapper for '\"$1\"' 1>&2' >> $2 && echo 'exec '\"$1\"' \"$@\"' >> $2 && echo 'exit 64' >> $2 && chmod 755 $2"
   ctx.actions.run_shell(
      outputs = [out],
      command = cmd,
      arguments = [ctx.attr.tool, out.path],
   )
   return [DefaultInfo(files = depset([out]))]

wrapper = rule(
    implementation = _wrapper,
    attrs = {
        "tool": attr.string(),
        "wrap_as": attr.string(),
    },
)
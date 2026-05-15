load("//:dummy.bzl", <weak_warning descr="Load value \"lo\" is never used">lo</weak_warning> = "symbol")

def <weak_warning descr="Function \"foo\" is never used">foo</weak_warning>(
    <weak_warning descr="Parameter \"oof\" is never used">oof</weak_warning>, two): 40 + two

lambda x, <weak_warning descr="Parameter \"y\" is never used">y</weak_warning>, z: x + z

<weak_warning descr="Variable \"bar\" is never used">bar</weak_warning> = 43

_ = 44

rab = 45
rab + rab

def top_level():
    def mapping():
        return 1

    def map(fun):
        fun()

    map(mapping)

top_level()

def <weak_warning descr="Function \"_my_func_impl\" is never used">_my_func_impl</weak_warning>(_target, ctx, _):
    _foo, some_val = some(ctx)
    return some_val

# Unused function calling a used helper
# https://youtrack.jetbrains.com/issue/BAZEL-3039
def _helper(x):
    return x + 1

def <weak_warning descr="Function \"_unused_caller\" is never used">_unused_caller</weak_warning>():
    return _helper(42)

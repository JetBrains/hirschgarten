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

[x for x in [1, 2, 3] if x > 2]
[x for x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z in [(1, 2, 3), (4, 5, 6)] if z > 5]
[x for (x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z) in [(1, 2, 3), (4, 5, 6)] if z > 5]
{x: x for x in [1, 2, 3] if x > 2}
{x: x for x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z in [(1, 2, 3), (4, 5, 6)] if z > 5}
{x: x for (x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z) in [(1, 2, 3), (4, 5, 6)] if z > 5}

def new_scope():
    for x in []:
        x
    for x, y in []:
        x + y
    for (x, y) in []:
        x + y

new_scope()

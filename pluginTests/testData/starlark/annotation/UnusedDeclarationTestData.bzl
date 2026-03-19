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
[x for x, (y, z) in [(1, (2, 3)), (4, (5, 6))] if y + z > 5]
[x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z) in [(1, (2, 3)), (4, (5, 6))] if z > 5]
[x for (y, z), x in [((2, 3), 1)] if y + z > 5]
[x for (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), x in [((2, 3), 1)] if z > 5]
[x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, (z, <weak_warning descr="Variable \"w\" is never used">w</weak_warning>)) in [(1, (2, (3, 4)))] if z > 5]
[x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, (<weak_warning descr="Variable \"z\" is never used">z</weak_warning>, w)) in [(1, (2, (3, 4)))] if w > 5]
[x for ((<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), <weak_warning descr="Variable \"w\" is never used">w</weak_warning>), x in [(((2, 3), 4), 1)] if z > 5]
[x for ((<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), w), x in [(((2, 3), 4), 1)] if z + w > 5]
{x: x for x in [1, 2, 3] if x > 2}
{x: x for x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z in [(1, 2, 3), (4, 5, 6)] if z > 5}
{x: x for (x, <weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z) in [(1, 2, 3), (4, 5, 6)] if z > 5}
{x: x for x, (y, z) in [(1, (2, 3)), (4, (5, 6))] if y + z > 5}
{x: x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z) in [(1, (2, 3)), (4, (5, 6))] if z > 5}
{x: x for (y, z), x in [((2, 3), 1)] if y + z > 5}
{x: x for (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), x in [((2, 3), 1)] if z > 5}
{x: x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, (z, <weak_warning descr="Variable \"w\" is never used">w</weak_warning>)) in [(1, (2, (3, 4)))] if z > 5}
{x: x for x, (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, (<weak_warning descr="Variable \"z\" is never used">z</weak_warning>, w)) in [(1, (2, (3, 4)))] if w > 5}
{x: x for ((<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), <weak_warning descr="Variable \"w\" is never used">w</weak_warning>), x in [(((2, 3), 4), 1)] if z > 5}
{x: x for ((<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), w), x in [(((2, 3), 4), 1)] if z + w > 5}

def new_scope():
    for x in []:
        x
    for x, y in []:
        x + y
    for (x, y) in []:
        x + y
    for x, (y, z) in []:
        x + y + z
    for (y, z), x in []:
        x + y + z
    for x, (y, (z, w)) in []:
        x + y + z + w
    for ((y, z), w), x in []:
        x + y + z + w
    for x, (y, (<weak_warning descr="Variable \"z\" is never used">z</weak_warning>, w)) in []:
        x + y + w
    for (<weak_warning descr="Variable \"y\" is never used">y</weak_warning>, z), x in []:
        x + z

new_scope()

def <weak_warning descr="Function \"_my_func_impl\" is never used">_my_func_impl</weak_warning>(_target, ctx, _):
    _foo, some_val = some(ctx)
    return some_val

# Test that reassignments are not incorrectly marked as unused
def reassignment_test():
    # Variable reassigned in conditional block - should NOT be marked unused
    # because references resolve to the latest binding before them
    runfiles = create_runfiles()
    if some_condition:
        runfiles = runfiles.merge(other_runfiles)
    runfiles = runfiles.merge(more_runfiles)
    return runfiles

reassignment_test()

def reassignment_in_else():
    result = 1
    if True:
        result = 2
    else:
        result = 3
    return result

reassignment_in_else()

def multiple_reassignments():
    value = initial()
    value = transform1(value)
    value = transform2(value)
    return value

multiple_reassignments()

def unused_reassignment():
    # Both assignments are unused because nothing reads either value
    <weak_warning descr="Variable \"value\" is never used">value</weak_warning> = 1
    <weak_warning descr="Variable \"value\" is never used">value</weak_warning> = 2

unused_reassignment()

def first_binding_unused_but_reassignment_used():
    # The first binding is unused because its value is never read
    # (no RHS reference on the second line, and the return resolves to line 2)
    # but the reassignment is used because return result resolves to it
    <weak_warning descr="Variable \"result\" is never used">result</weak_warning> = compute_initial()
    result = compute_final()
    return result

first_binding_unused_but_reassignment_used()

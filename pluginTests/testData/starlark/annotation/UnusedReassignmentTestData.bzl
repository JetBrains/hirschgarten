# Reassignment under if
def reassign_if():
    x = 1
    if True:
        x = 2
    return x

reassign_if()

# Reassignment under if else
# https://youtrack.jetbrains.com/issue/BAZEL-2854
def reassign_if_else():
    x = 1
    if True:
        x = 2
    else:
        x = 3
    return x

reassign_if_else()

def reassign_nested_if():
    x = 1
    if True:
        if True:
            x = 2
    return x

reassign_nested_if()

# Unused variable inside if
def unused_in_if():
    if True:
        <weak_warning descr="Variable \"y\" is never used">y</weak_warning> = 1
    return 0

unused_in_if()

# Straight-line reassignment with no reads
def dead_store():
    <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 2
    <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 3

dead_store()

# Tuple destructuring reassignment
def tuple_reassign():
    x = 1
    x, y = 2, 3
    return x + y

tuple_reassign()

# Parameter reassignment
def param_reassign(x):
    x = 2
    return x

param_reassign(1)

# Reassignment in for-loop body
def reassign_in_loop():
    x = 0
    for i in [1, 2]:
        x = i
    return x

reassign_in_loop()

# Multiple sequential reassignments
def multi_reassign():
    x = 1
    x = 2
    x = 3
    return x

multi_reassign()

# First assignment in if/else branches
def first_assign_if_else(cond):
    if cond:
        x = 1
    else:
        x = 2
    return x

first_assign_if_else(True)

# Unread if/else assignments
def unread_if_else(cond):
    if cond:
        <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 1
    else:
        <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 2

unread_if_else(True)

# Unused variable in for-loop body
def unused_in_loop():
    for i in [1, 2]:
        <weak_warning descr="Variable \"y\" is never used">y</weak_warning> = i
    return 0

unused_in_loop()

# RHS reference counts as a read — no warning on the source variable
def rhs_reference():
    x = 1
    y = x
    return y

rhs_reference()

# y is unused, but x has a read reference (RHS of y = x) so no warning on x
def rhs_reference_unused_target():
    x = 1
    <weak_warning descr="Variable \"y\" is never used">y</weak_warning> = x

rhs_reference_unused_target()

# Cross-scope: function variable shadows module-level variable (not a reassignment)
<weak_warning descr="Variable \"_cross_scope_x\" is never used">_cross_scope_x</weak_warning> = 1
def cross_scope_func():
    _cross_scope_x = 2
    return _cross_scope_x

cross_scope_func()

# Cross-scope: comprehension variable shadows module-level variable
_comp_x = 1
[_comp_x for _comp_x in [1, 2, 3]]
_comp_x

# Cross-scope: nested function variable shadows outer function variable
def outer_func():
    x = 1
    def inner_func():
        x = 2
        return x
    return inner_func() + x

outer_func()

# Cross-scope: comprehension variable shadows function variable
def func_with_comp():
    <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 1
    result = [x for x in [2, 3]]
    return result

func_with_comp()

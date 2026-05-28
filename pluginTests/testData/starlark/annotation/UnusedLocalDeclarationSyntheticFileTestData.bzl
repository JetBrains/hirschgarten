def foo(<weak_warning descr="Parameter \"b\" is never used">b</weak_warning>):
    <weak_warning descr="Variable \"x\" is never used">x</weak_warning> = 1
    y = 1
    return y

<weak_warning descr="Variable \"_x\" is never used">_x</weak_warning> = 1
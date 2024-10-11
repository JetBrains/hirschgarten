load("//:dummy.bzl", <weak_warning descr="Load value \"lo\" is never used">lo</weak_warning> = "symbol")

def <weak_warning descr="Function \"foo\" is never used">foo</weak_warning>(
    <weak_warning descr="Parameter \"oof\" is never used">oof</weak_warning>, two): 40 + two

<weak_warning descr="Variable \"bar\" is never used">bar</weak_warning> = 43

_ = 44

rab = 45
rab + rab

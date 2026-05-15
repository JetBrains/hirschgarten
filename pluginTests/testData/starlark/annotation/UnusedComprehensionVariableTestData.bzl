# https://youtrack.jetbrains.com/issue/BAZEL-1368
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
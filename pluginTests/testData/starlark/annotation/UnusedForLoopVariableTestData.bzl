def scope():
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

scope()
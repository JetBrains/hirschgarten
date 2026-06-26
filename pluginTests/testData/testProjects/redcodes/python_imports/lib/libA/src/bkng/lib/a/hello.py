from bkng.lib.b.hello import b_my_function
from bkng.lib.<error>c</error>.hello import <error>c_my_function</error>

from . import sibling

def a_my_function():
    b_my_function()
    sibling.sibling_func()

from lib.libA.src.bkng.lib.a.hello import a_my_function
from bkng.lib.b.hello import b_my_function
from bkng.lib.<error>c</error>.hello import <error>c_my_function</error>
from lib.libA.util import util_function
from tools.helper.op import op_func
import <error>does_not_exist_anywhere</error>

if __name__ == "__main__":
    a_my_function()
    util_function()
    op_func()

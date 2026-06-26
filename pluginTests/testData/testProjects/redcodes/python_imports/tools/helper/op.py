# Relative import within a regular package (`__init__.py`) that has NO `imports` attribute.
from . import other


def op_func():
    other.other_func()

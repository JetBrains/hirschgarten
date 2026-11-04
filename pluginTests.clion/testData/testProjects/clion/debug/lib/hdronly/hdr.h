#pragma once

#define HDRONLY_LIB_VALUE 61

int hdronly_lib_function(void) {
  const int val = HDRONLY_LIB_VALUE;
  return val;
}

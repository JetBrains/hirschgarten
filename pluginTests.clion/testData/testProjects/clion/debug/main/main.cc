#include "lib/default/hdr.h"
#include "lib/hdronly/hdr.h"
#include "lib/generated/hdr.h"
#include "lib/external/hdr.h"

int main(void) {
  int sum = 0;
  sum += default_lib_function();
  sum += hdronly_lib_function();
  sum += external_lib_function();
  sum += generated_lib_function();

  return sum;
}

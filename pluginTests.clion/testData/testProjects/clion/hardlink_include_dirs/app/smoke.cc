#include <cstdio>

#include "fakelib/fakelib.h"     // source include dir of the external repo
#include "fakelib/generated.h"   // generated (tree artifact) include dir of the external repo
#include "config/answer.h"       // generated (tree artifact) include dir from a code generator rule
#include "config/version.h"

int main() {
  const int sum = fakelib::add(40, 2);
  if (sum != FAKELIB_GENERATED_ANSWER) {
    std::fprintf(stderr, "got %d\n", sum);
    return 1;
  }
  if (sum != CONFIG_ANSWER || CONFIG_VERSION != 3) {
    std::fprintf(stderr, "generated config mismatch: answer=%d version=%d\n", CONFIG_ANSWER, CONFIG_VERSION);
    return 1;
  }
  std::puts("ok");
  return 0;
}

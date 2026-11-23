#include "lib/lib.h"
#include "lib/private.h"

static const char* const kColorNames[kColorCount] = {
#define COLOR(name) #name,
#include "lib/colors.inl"
#undef COLOR
};

const char* color_name(Color color) {
  return kColorNames[color];
}

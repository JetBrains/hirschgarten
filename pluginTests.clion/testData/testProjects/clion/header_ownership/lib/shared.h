#pragma once

enum Color {
#define COLOR(name) name,
#include "lib/colors.inl"
#undef COLOR
};

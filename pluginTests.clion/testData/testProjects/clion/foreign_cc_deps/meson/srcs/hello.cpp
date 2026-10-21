#include "hello.h"

#include <sstream>

auto hello(std::string name) -> std::string {
  std::ostringstream out{};
  out << "Hello " << name << "!";
  out << " Greetings form the foreign meson library.";
  return out.str();
}


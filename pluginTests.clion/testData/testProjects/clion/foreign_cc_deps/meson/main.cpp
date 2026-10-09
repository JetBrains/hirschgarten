#include "include/hello.h"

#include <iostream>
#include <ostream>

int main(int argc, char **argv) {

  std::cout << hello("world") << std::endl;

  return 0;
}

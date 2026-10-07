#include "Speaker.h"

#include <iostream>
#include <ostream>

void Speaker::sayHello() {
  std::cout << "Hello, I am " << name_ << "!" << std::endl;
}

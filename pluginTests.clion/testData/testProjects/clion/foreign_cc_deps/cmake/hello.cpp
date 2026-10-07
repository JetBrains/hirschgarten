#include <Speaker.h>

int main(int argc, char *argv[]) {
  Speaker* speaker = new Speaker("An Example");

  speaker->sayHello();  
}

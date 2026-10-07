#ifndef SPEAKER_H
#define SPEAKER_H

#include <string>
#include <utility>

class Speaker {

  std::string name_;

  public:
    Speaker(std::string const& name): name_{std::move(name)}{}
    void sayHello();
};

#endif
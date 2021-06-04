#ifndef JAMMER_H
#define JAMMER_H

#include <chrono>
#include <random>

#include "channel.hpp"
#include "../tools/utils.hpp"

class Jammer : public Channel {
public:
    explicit Jammer(double p);
    ~Jammer() = default;
};

#endif  // JAMMER_H
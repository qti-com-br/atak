#ifndef BSC_H
#define BSC_H

#include <chrono>
#include <random>

#include "channel.hpp"
#include "../tools/utils.hpp"

class BSC : public Channel {
public:
    explicit BSC(double p);
    ~BSC() = default;
};

#endif  // BSC_H
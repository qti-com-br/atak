#include "jammer.hpp"

Jammer::Jammer(double p) {
    bit_op = [p](Bitlist& bits) {
        std::mt19937 mt_rand(std::chrono::high_resolution_clock::now()
                                 .time_since_epoch()
                                 .count());

        for (int i = 0; i < bits.size(); i++) {
            // Using one sig fig
            if (mt_rand() % 1000 < p * 10) {
                // Turn on the bit p percent of the time
                bits[i] = 1;
            }
        }
    };
}
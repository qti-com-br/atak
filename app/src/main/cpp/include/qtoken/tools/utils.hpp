#ifndef UTILS_H
#define UTILS_H

#include <openssl/hmac.h>
#include <openssl/sha.h>
#include <algorithm>
#include <chrono>
#include <climits>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <vector>

#include "../crypto/EVP/digest.hpp"
#include "byte_vector.hpp"
#include "globals/logger.hpp"

#include "types.hpp"

using namespace Qtoken;

/**
 * Convert char* bytes to templated Container
 * Container must implement push_back()
 * @param bytes bytes to be converted
 * @param num_bytes length of byte stream
 * @return bit vector
 */
template <class Container>
inline Container bytes_to_bits(Bytelist bytes) {
    Container bits;
    for (int i = 0; i < bytes.size(); i++) {
        char c = bytes[i];
        for (int j = CHAR_BIT - 1; j >= 0; j--) {
            bits.push_back(int((c >> j) & 1));
        }
    }
    return bits;
}

/**
 * Convert templated container bits to char* bytes
 * Container must implement size()
 * @param bits binary to be converted
 * @return byte stream is allocated on the return char*
 */
template <class Container>
inline Bytelist bits_to_bytes(Container bits) {
    int num_bytes = bits.size() / 8;
    Bytelist bytes;
    for (int i = 0; i < num_bytes; i++) {
        char c = 0x00;  // Zeroed out char
        for (int j = CHAR_BIT - 1; j >= 0; j--) {
            c |= bits[(i * CHAR_BIT) + ((CHAR_BIT - 1) - j)] << j;
        }
        bytes.push_back(c);
    }
    return bytes;
}

/**
 * Computes sha256 hash of input string.
 * @param str input string to hash.
 * @return sha256 hash of str.
 */
inline std::string sha256(const std::string& str) {
    byte_vector bv;
    bv.FromString(str);
    Crypto::EVP::Digest dgst(Crypto::EVP::DigestType::SHA256);
    return dgst.Hash(bv).ToHexString();
}

/**
 * Variable length XOR for two char arrays.
 * @param a first operand.
 * @param b second operand.
 * @return result of a xor b.
 */
inline Bytelist XOR_bytes(Bytelist a, Bytelist b) {
    int a_size = a.size();
    int b_size = b.size();

    Bytelist greater = (a_size >= b_size) ? a : b;
    Bytelist lesser = (a_size < b_size) ? a : b;
    Bytelist out;

    for (int i = lesser.size(); i < greater.size(); i++) lesser.push_back(0);
    for (int i = 0; i < greater.size(); i++) {
        out.push_back(0);
        if (i >= lesser.size())
            lesser.push_back(0);
    }

    std::transform(begin(greater), end(greater), begin(lesser), begin(out),
                   std::bit_xor<char>());

    return out;
}

/**
 * Returns a random uint8_t
 * RNG is seeded with clock time
 * @return psuedo random uint8_t
 */
inline uint8_t get_rand_seed() {
    return (uint8_t)std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::time_point_cast<std::chrono::milliseconds>(
                   std::chrono::system_clock::now())
                   .time_since_epoch())
        .count();
}

/**
 * Returns a random uint32_t
 * RNG is seeded with clock time
 * @return psuedo random uint32_t
 */
inline uint32_t get_rand_seed_uint32_t() {
    return (uint32_t)std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::time_point_cast<std::chrono::milliseconds>(
                   std::chrono::system_clock::now())
                   .time_since_epoch())
        .count();
}

/**
 * Returns true is s is a number.
 * @return true if s is a number.
 */
inline bool is_number(const std::string& s) {
    return !s.empty() && std::find_if(s.begin(), s.end(), [](unsigned char c) {
                             return !std::isdigit(c);
                         }) == s.end();
}

#endif  // UTILS_H
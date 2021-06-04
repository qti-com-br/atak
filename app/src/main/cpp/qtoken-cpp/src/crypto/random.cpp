#include "crypto/random.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
/*
 * Return new byte_vector that is 'length' bytes long and contains random bytes.
 */
byte_vector Random::Bytes(int length) {
    byte_vector buf(length);
    Bytes(buf);
    return buf;
}

/*
 * Fill passed in byte_vector with random bytes.
 */
void Random::Bytes(byte_vector& value) {
    RAND_bytes(value.c_data(), value.size());
}

/*
 * Seed random number generator with 'length' number of bytes.
 */
void Random::Seed(int length) {
    byte_vector buf;

    buf = Bytes(length);
    Seed(buf);
}

/*
 * Seed random number generator with bytes in value.
 */
void Random::Seed(const byte_vector& value) {
    RAND_seed(value.data(), value.size());
}

/*
 * Return new string with 'length' number of random ASCII characters.
 */
string Random::RandomChars(int length) {
    string rand(length, '0');

    for (int i = 0; i < length; i++)
        rand[i] = (char)RandomNum_Uniform<uint32_t>(0x5e) + 0x20;

    return rand;
}
}  // namespace Crypto
}  // namespace Qtoken

#ifndef _CRYPTO_RANDOM_H
#define _CRYPTO_RANDOM_H

#include <cstddef>
#include <string>

#include <openssl/rand.h>

#include "../tools/byte_vector.hpp"

namespace Qtoken {
namespace Crypto {
class Random {
public:
    static byte_vector Bytes(int);
    static void Bytes(byte_vector&);

    static void Seed(int);
    static void Seed(const byte_vector&);

    /*
     * Returns single random value of type T.
     */
    template <typename T>
    static T RandomNum() {
        auto rnd = Bytes(sizeof(T));
        T rv = 0;
        for (T i = 0; i < sizeof(T); i++)
            rv |= (T)(std::to_integer<T>(rnd[i]) & 0xff)
                  << (sizeof(T) - (i + 1));
        return rv;
    }

    /*
     * Returns single random value of type T uniformly distributed but less than
     * upper_bound. This is recommended over constructs like 'RandomNum() %
     * upper_bound' as it avoids 'modulo bias' when the upper bound is not a
     * power of two.
     */
    template <typename T>
    static T RandomNum_Uniform(T upper_bound) {
        T r, min;

        if (upper_bound < 2)
            return 0;

        // 2**32 % x == (2**32 - x) % x
        min = (T)(-upper_bound % upper_bound);

        for (;;) {
            r = RandomNum<T>();
            if (r >= min)
                break;
        }

        return r % upper_bound;
    }

    static std::string RandomChars(int);
};
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPTO_RANDOM_H */

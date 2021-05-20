#ifndef _CRYPTO_EC_H
#define _CRYPTO_EC_H

#include <string>

#include <openssl/ec.h>

#include "tools/byte_vector.hpp"

namespace Qtoken {
namespace Crypto {
namespace EVP {
class PKey;
}
class Bio;
class EC {
    friend EVP::PKey;
    friend Bio;

private:
    const EC_GROUP *group;
    EC_KEY *key;

    EC();
    explicit EC(const byte_vector &);
    explicit EC(EC_KEY *);

    static byte_vector GetPrivateKey(EC_KEY *);
    static byte_vector GetPublicKey(EC_KEY *, const EC_GROUP *);

    byte_vector GetPrivateKey();

    void NewFromCurveName(const std::string &curve, bool compress = false);
    void SetPrivateKey(const byte_vector &);

public:
    explicit EC(const std::string &name);
    EC(const EC &);
    virtual ~EC();

    byte_vector GetPublicKey();
};
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPTO_EC_H */
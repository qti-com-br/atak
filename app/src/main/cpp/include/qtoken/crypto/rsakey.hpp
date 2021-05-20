#ifndef _CRYPT_RSA_H
#define _CRYPT_RSA_H

#include <openssl/rsa.h>

#include "tools/byte_vector.hpp"

namespace Qtoken {
namespace Crypto {
namespace EVP {
class PKey;
}
class Bio;
class RsaKey {
    friend EVP::PKey;
    friend Bio;

private:
    RSA *key;

    RsaKey();
    explicit RsaKey(const byte_vector &);
    explicit RsaKey(RSA *);

    byte_vector GetPrivateKey();
    static byte_vector GetPrivateKey(RSA *);
    static byte_vector GetPublicKey(RSA *);
    void SetPrivateKey(const byte_vector &);

public:
    explicit RsaKey(int);
    RsaKey(RsaKey &);
    virtual ~RsaKey();

    byte_vector GetPublicKey();
};
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPT_RSA_H */

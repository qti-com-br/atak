#ifndef _CRYPTO_X509_H
#define _CRYPTO_X509_H

#include <openssl/x509.h>

#include "../tools/byte_vector.hpp"
#include "../crypto/EVP/pkey.hpp"

namespace Qtoken {
namespace Crypto {
class CMS;
class x509 {
    friend CMS;

private:
    X509 *cert;

    x509();

    void SetCertificate(const byte_vector &);

    static byte_vector GetCertificate(X509 *);

public:
    explicit x509(const byte_vector &);
    explicit x509(const std::string &);
    x509(const x509 &);
    ~x509();

    EVP::PKey GetPublicKey();
};
}  // namespace Crypto
}  // namespace Qtoken
#endif /* !_CRYPTO_X509_H */
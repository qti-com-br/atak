#ifndef _CRYPTO_EVP_PKEY_H
#define _CRYPTO_EVP_PKEY_H

#include <string>

#include <openssl/evp.h>

#include "../tools/byte_vector.hpp"
#include "../crypto/ec.hpp"
#include "../crypto/rsakey.hpp"
#include "../crypto/EVP/digest.hpp"

namespace Qtoken {
namespace Crypto {
class x509;
class CMS;
namespace EVP {
enum class PKeyType {
    DSA,
    RSA,
    ECDSA,
};

class PKey {
    friend x509;
    friend CMS;

private:
    EVP_PKEY *pkey;
    EVP_PKEY_CTX *ctx;

    PKeyType Type;
    Digest *dgst;

    PKey();
    explicit PKey(EVP_PKEY *);

    static byte_vector GetPrivateKey(EVP_PKEY *);
    static byte_vector GetPublicKey(EVP_PKEY *);

    static void SetEcdhKdfMd(EVP_PKEY_CTX *, DigestType);
    static void SetRsaPssMd(EVP_PKEY_CTX *);
    static void SetRsaOaepMd(EVP_PKEY_CTX *, DigestType);

    byte_vector GetPrivateKey();

public:
    explicit PKey(RsaKey);
    explicit PKey(EC);
    explicit PKey(std::string);
    virtual ~PKey();

    PKeyType GetType();

    void SignInit(DigestType);
    void SignUpdate(const byte_vector &);
    byte_vector SignFinal();
    byte_vector Sign(DigestType, const byte_vector &);
    byte_vector SignFile(DigestType, std::string);

    void VerifyInit(DigestType);
    void VerifyUpdate(const byte_vector &);
    bool VerifyFinal(const byte_vector &);
    bool Verify(DigestType, const byte_vector &, const byte_vector &);
    bool VerifyFile(DigestType, std::string, const byte_vector &);

    byte_vector Encrypt(const byte_vector &data);

    void SetEcdhKdfMd(DigestType);
    void SetRsaPssMd();
    void SetRsaOaepMd(DigestType);

    byte_vector GetPublicKey();
};
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPTO_EVP_PKEY_H */

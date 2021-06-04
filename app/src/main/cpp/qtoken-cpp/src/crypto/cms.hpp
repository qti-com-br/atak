#ifndef _CRYPTO_CMS_H
#define _CRYPTO_CMS_H

#include <openssl/cms.h>

#include "EVP/cipher.hpp"
#include "EVP/digest.hpp"
#include "EVP/pkey.hpp"
#include "x509.hpp"

namespace Qtoken {
namespace Crypto {
class CMS {
private:
    CMS_ContentInfo* ci;
    CMS_RecipientInfo* ri;
    x509* cert;
    EVP::PKey* pkey;

    bool encrypt;

public:
    explicit CMS(const EVP::CipherType);
    explicit CMS(const byte_vector&);
    explicit CMS(const std::string&);
    virtual ~CMS();

    void SetCertificate(const x509&);
    void SetPrivateKeyForDecrypt(const EVP::PKey&);
    void SetEncMd(EVP::DigestType);
    byte_vector Encrypt(const byte_vector&);
    byte_vector Decrypt();
};
}  // namespace Crypto
}  // namespace Qtoken
#endif /* !_CRYPTO_CMS_H */
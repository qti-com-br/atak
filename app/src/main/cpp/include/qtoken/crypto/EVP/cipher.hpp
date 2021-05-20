#ifndef _CRYPTO_EVP_CIPHER_H
#define _CRYPTO_EVP_CIPHER_H

#include <string>

#include <openssl/cmac.h>
#include <openssl/evp.h>

#include "tools/byte_vector.hpp"

namespace Qtoken {
namespace Crypto {
class CMS;
namespace EVP {
enum class CipherType {
    AES128_ECB,
    AES128_CBC,
    AES128_CCM,
    AES128_CFB1,
    AES128_CFB8,
    AES128_CFB128,
    AES128_CTR,
    AES128_GCM,
    AES128_OFB,
    AES128_XTS,
    AES192_ECB,
    AES192_CBC,
    AES192_CCM,
    AES192_CFB1,
    AES192_CFB8,
    AES192_CFB128,
    AES192_CTR,
    AES192_GCM,
    AES192_OFB,
    AES256_ECB,
    AES256_CBC,
    AES256_CCM,
    AES256_CFB1,
    AES256_CFB8,
    AES256_CFB128,
    AES256_CTR,
    AES256_GCM,
    AES256_OFB,
    AES256_XTS,
};

class Cipher {
    friend CMS;

private:
    int block_size;
    int iv_size;
    int key_size;

    CipherType Type;

    EVP_CIPHER_CTX* ctx;
    CMAC_CTX* cmac_ctx;
    const EVP_CIPHER* evptype;

    static const EVP_CIPHER* GetEvpType(CipherType);

public:
    explicit Cipher(CipherType);
    Cipher(const Cipher&);
    ~Cipher();

    int GetBlockSize();
    int GetIvSize();
    int GetKeySize();
    CipherType GetCipherType();

    void EncryptInit(const byte_vector& key, const byte_vector& iv = {},
                     bool padding = true);
    byte_vector EncryptUpdate(const byte_vector& data);
    byte_vector EncryptFinal();
    byte_vector Encrypt(const byte_vector& key, const byte_vector& data,
                        const byte_vector& iv = {}, bool padding = true);

    void DecryptInit(const byte_vector& key, const byte_vector& iv = {},
                     bool padding = true);
    byte_vector DecryptUpdate(const byte_vector& data);
    byte_vector DecryptFinal();
    byte_vector Decrypt(const byte_vector& key, const byte_vector& data,
                        const byte_vector& iv = {}, bool padding = true);

    void CmacInit(const byte_vector&);
    void CmacUpdate(const byte_vector&);
    byte_vector CmacFinal();
    byte_vector Cmac(const byte_vector&, const byte_vector&);
    byte_vector CmacFile(const byte_vector&, std::string);
};
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken

#endif /* _CRYPTO_EVP_CIPHER_H */
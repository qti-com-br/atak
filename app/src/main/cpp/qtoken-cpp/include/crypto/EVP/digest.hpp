#ifndef _CRYPTO_EVP_DIGEST_H
#define _CRYPTO_EVP_DIGEST_H

#include <string>

#include <openssl/evp.h>
#include <openssl/hmac.h>

#include "../tools/byte_vector.hpp"

namespace Qtoken {
namespace Crypto {
namespace EVP {
// Some systems expect integer value for different SHA algorithms to be in this
// order (0 - SHA1, 1 - SHA256, etc.)
enum class DigestType {
    SHA1,
    SHA256,
    SHA384,
    SHA512,
    SHA224,
};

class PKey;
class Digest {
    friend PKey;

private:
    int block_size;
    int md_size;

    DigestType Type;
    EVP_MD_CTX *ctx;
    HMAC_CTX *hmac_ctx;
    const EVP_MD *evptype;

    static const EVP_MD *GetEvpType(DigestType);

public:
    explicit Digest(DigestType);
    Digest(const Digest &);
    virtual ~Digest();

    int GetBlockSize();
    int GetMDSize();
    DigestType GetDigestType();

    void HashInit();
    void HashUpdate(const byte_vector &);
    byte_vector HashFinal();
    byte_vector Hash(const byte_vector &);
    byte_vector HashFile(std::string);

    void HmacInit(const byte_vector &);
    void HmacUpdate(const byte_vector &);
    byte_vector HmacFinal();
    byte_vector Hmac(const byte_vector &, const byte_vector &);
    byte_vector HmacFile(const byte_vector &, std::string);
};
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken

#endif /* !_CRYPTO_EVP_DIGEST_H */

#include <openssl/pem.h>
#include <openssl/x509.h>

#include "crypto/bio.hpp"
#include "crypto/exception.hpp"
#include "crypto/random.hpp"
#include "crypto/rsakey.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
RsaKey::RsaKey() {
    this->key = nullptr;
}

/*
 * Create new RsaKey object with private key passed in from byte_vector.
 */
RsaKey::RsaKey(const byte_vector &keyb)
    : RsaKey() {
    SetPrivateKey(keyb);
}

/*
 * Create new RsaKey object with private key passed in from RSA pointer.
 */
RsaKey::RsaKey(RSA *key)
    : RsaKey() {
    SetPrivateKey(GetPrivateKey(key));
}

/*
 * Get byte_vector of private key in DER-encoded format for this RsaKey object.
 */
byte_vector RsaKey::GetPrivateKey() {
    return GetPrivateKey(this->key);
}

/*
 * Get byte_vector of private key in DER-encoded format for passed in RSA
 * pointer.
 */
byte_vector RsaKey::GetPrivateKey(RSA *key) {
    SecureMemoryBio bio;
    int siz = i2d_RSAPrivateKey_bio(bio.bio, key);
    if (siz < 0)
        throw OpenSSLException("i2d_RSAPrivateKey_bio", __func__, __LINE__,
                               true);

    return bio.GetData();
}

void RsaKey::SetPrivateKey(const byte_vector &srckey) {
    SecureMemoryBio bio;
    bio.Write(srckey);
    RSA_free(this->key);
    this->key = nullptr;
    this->key = d2i_RSAPrivateKey_bio(bio.bio, nullptr);
    if (this->key == nullptr)
        throw OpenSSLException("d2i_RSAPrivateKey_bio", __func__, __LINE__,
                               true);
}

byte_vector RsaKey::GetPublicKey(RSA *key) {
    MemoryBio bio;
    int siz = i2d_RSAPublicKey_bio(bio.bio, key);
    if (siz < 0)
        throw OpenSSLException("i2d_RSAPublicKey_bio", __func__, __LINE__,
                               true);

    return bio.GetData();
}

RsaKey::RsaKey(int bits)
    : RsaKey() {
    this->key = RSA_new();
    if (this->key == nullptr)
        throw OpenSSLException("RSA_new", __func__, __LINE__, true);

    BIGNUM *e = BN_new();
    if (e == nullptr)
        throw OpenSSLException("BN_new", __func__, __LINE__, true);
    // Set exponent to 65537
    BN_set_word(e, RSA_F4);

    Random::Seed(512);

    if (RSA_generate_key_ex(this->key, bits, e, nullptr) == 0)
        throw OpenSSLException("RSA_generate_key_ex", __func__, __LINE__, true);

    OPENSSL_free(e);
}

RsaKey::RsaKey(RsaKey &rkey)
    : RsaKey(rkey.GetPrivateKey()) {}

RsaKey::~RsaKey() {
    RSA_free(this->key);
    this->key = nullptr;
}

byte_vector RsaKey::GetPublicKey() {
    return GetPublicKey(this->key);
}
}  // namespace Crypto
}  // namespace Qtoken

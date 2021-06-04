#include <string>

#include <openssl/pem.h>

#include "crypto/bio.hpp"
#include "crypto/cms.hpp"
#include "crypto/exception.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
/*
 * Start a CMS object for encryption. Must specify the cipher type. Suggested
 * type is AES256_CBC.
 */
CMS::CMS(const EVP::CipherType ctype) {
    this->ci = CMS_encrypt(nullptr, nullptr, EVP::Cipher::GetEvpType(ctype),
                           CMS_BINARY | CMS_PARTIAL);
    if (this->ci == nullptr)
        throw OpenSSLException("CMS_encrypt", __func__, __LINE__, true);

    this->ri = nullptr;
    this->cert = nullptr;
    this->pkey = nullptr;
    this->encrypt = true;
}

/*
 * Start a CMS object for decryption. Data passed in must be in ASN.1 format.
 */
CMS::CMS(const byte_vector& data) {
    MemoryBio d;
    d.Write(data);

    this->ci = d2i_CMS_bio(d.bio, nullptr);
    if (this->ci == nullptr)
        throw OpenSSLException("d2i_CMS_bio", __func__, __LINE__, true);

    this->ri = nullptr;
    this->cert = nullptr;
    this->pkey = nullptr;
    this->encrypt = false;
}

/*
 * Start a CMS object for decryption. File name passed in must be in either PEM
 * or ASN.1 format.
 */
CMS::CMS(const string& file) {
    auto bio = Bio::ReadFile(file);

    this->ci = PEM_read_bio_CMS(bio.bio, nullptr, nullptr, nullptr);
    if (this->ci == nullptr) {
        bio = Bio::ReadFile(file);
        this->ci = d2i_CMS_bio(bio.bio, nullptr);
        if (this->ci == nullptr)
            throw OpenSSLException("d2i_CMS_bio", __func__, __LINE__, true);
    }

    this->ri = nullptr;
    this->cert = nullptr;
    this->pkey = nullptr;
    this->encrypt = false;
}

CMS::~CMS() {
    CMS_ContentInfo_free(this->ci);
    this->ci = nullptr;
    // the above also frees this->ri
    this->ri = nullptr;

    delete this->cert;
    this->cert = nullptr;

    // this->pkey is passed as a reference so we don't free it
    this->pkey = nullptr;
}

/*
 * Set certificate for encryption or decryption.
 */
void CMS::SetCertificate(const x509& cert) {
    if (this->encrypt) {
        this->ri = CMS_add1_recipient_cert(this->ci, cert.cert, CMS_KEY_PARAM);
        if (this->ri == nullptr)
            throw OpenSSLException("CMS_add1_recipient_cert", __func__,
                                   __LINE__, true);
    }

    this->cert = new x509(cert);
}

/*
 * Set private key for decryption only.
 */
void CMS::SetPrivateKeyForDecrypt(const EVP::PKey& pkey) {
    if (this->encrypt)
        throw OpenSSLException(
            "SetPrivateKeyForDecrypt: can only be used for CMS decryption "
            "object",
            __func__, __LINE__);

    this->pkey = new EVP::PKey(pkey);
}

/*
 * Set message digest for encryption only. Suggested digest type is SHA512.
 */
void CMS::SetEncMd(EVP::DigestType dtype) {
    if (!this->encrypt)
        throw OpenSSLException(
            "SetEncMd: can only be used for CMS encryption object", __func__,
            __LINE__);
    auto pkey = this->cert->GetPublicKey();

    EVP_PKEY_CTX* p = CMS_RecipientInfo_get0_pkey_ctx(this->ri);
    if (p == nullptr)
        throw OpenSSLException("CMS_RecipientInfo_get0_pkey_ctx", __func__,
                               __LINE__, true);

    switch (pkey.GetType()) {
    case EVP::PKeyType::RSA:
        EVP::PKey::SetRsaOaepMd(p, dtype);
        break;
    case EVP::PKeyType::ECDSA:
        EVP::PKey::SetEcdhKdfMd(p, dtype);
        break;
    default:
        throw OpenSSLException("Unsupported PKey Type", __func__, __LINE__);
    }
}

/*
 * Encrypt passed in data and return ASN.1 format object.
 */
byte_vector CMS::Encrypt(const byte_vector& data) {
    if (!this->encrypt)
        throw OpenSSLException(
            "Encrypt: can only use for CMS encryption object", __func__,
            __LINE__);

    SecureMemoryBio d;
    d.Write(data);
    if (CMS_final(this->ci, d.bio, nullptr, CMS_BINARY) == 0)
        throw OpenSSLException("CMS_final", __func__, __LINE__, true);

    MemoryBio b;
    if (i2d_CMS_bio(b.bio, this->ci) == 0)
        throw OpenSSLException("i2d_CMS_bio", __func__, __LINE__, true);

    return b.GetData();
}

/*
 * Decrypt data passed in at object creation and return raw data as byte_vector.
 */
byte_vector CMS::Decrypt() {
    if (this->encrypt)
        throw OpenSSLException(
            "Decrypt: can only use for CMS decryption object", __func__,
            __LINE__);

    SecureMemoryBio b;
    if (CMS_decrypt(this->ci, this->pkey->pkey, this->cert->cert, nullptr,
                    b.bio, CMS_BINARY) == 0)
        throw OpenSSLException("CMS_decrypt", __func__, __LINE__, true);

    return b.GetData();
}
}  // namespace Crypto
}  // namespace Qtoken

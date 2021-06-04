#include <fstream>

#include "crypto/exception.hpp"
#include "crypto/EVP/cipher.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
namespace EVP {
const EVP_CIPHER* Cipher::GetEvpType(CipherType type) {
    switch (type) {
    case CipherType::AES128_ECB:
        return EVP_aes_128_ecb();
    case CipherType::AES128_CBC:
        return EVP_aes_128_cbc();
    case CipherType::AES128_CCM:
        return EVP_aes_128_ccm();
    case CipherType::AES128_CFB1:
        return EVP_aes_128_cfb1();
    case CipherType::AES128_CFB8:
        return EVP_aes_128_cfb8();
    case CipherType::AES128_CFB128:
        return EVP_aes_128_cfb128();
    case CipherType::AES128_CTR:
        return EVP_aes_128_ctr();
    case CipherType::AES128_GCM:
        return EVP_aes_128_gcm();
    case CipherType::AES128_OFB:
        return EVP_aes_128_ofb();
    case CipherType::AES128_XTS:
        return EVP_aes_128_xts();
    case CipherType::AES192_ECB:
        return EVP_aes_192_ecb();
    case CipherType::AES192_CBC:
        return EVP_aes_192_cbc();
    case CipherType::AES192_CCM:
        return EVP_aes_192_ccm();
    case CipherType::AES192_CFB1:
        return EVP_aes_192_cfb1();
    case CipherType::AES192_CFB8:
        return EVP_aes_192_cfb8();
    case CipherType::AES192_CFB128:
        return EVP_aes_192_cfb128();
    case CipherType::AES192_CTR:
        return EVP_aes_192_ctr();
    case CipherType::AES192_GCM:
        return EVP_aes_192_gcm();
    case CipherType::AES192_OFB:
        return EVP_aes_192_ofb();
    case CipherType::AES256_ECB:
        return EVP_aes_256_ecb();
    case CipherType::AES256_CBC:
        return EVP_aes_256_cbc();
    case CipherType::AES256_CCM:
        return EVP_aes_256_ccm();
    case CipherType::AES256_CFB1:
        return EVP_aes_256_cfb1();
    case CipherType::AES256_CFB8:
        return EVP_aes_256_cfb8();
    case CipherType::AES256_CFB128:
        return EVP_aes_256_cfb128();
    case CipherType::AES256_CTR:
        return EVP_aes_256_ctr();
    case CipherType::AES256_GCM:
        return EVP_aes_256_gcm();
    case CipherType::AES256_OFB:
        return EVP_aes_256_ofb();
    case CipherType::AES256_XTS:
        return EVP_aes_256_xts();
    default:
        throw OpenSSLException("Unknown cipher type", __func__, __LINE__);
    }
}

Cipher::Cipher(CipherType type) {
    this->ctx = EVP_CIPHER_CTX_new();
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_CIPHER_CTX_new", __func__, __LINE__, true);

    this->cmac_ctx = CMAC_CTX_new();
    if (this->cmac_ctx == nullptr)
        throw OpenSSLException("CMAC_CTX_new", __func__, __LINE__, true);

    this->evptype = GetEvpType(type);
    this->Type = type;

    this->block_size = EVP_CIPHER_block_size(this->evptype);
    this->iv_size = EVP_CIPHER_iv_length(this->evptype);
    this->key_size = EVP_CIPHER_key_length(this->evptype);
}

Cipher::Cipher(const Cipher& value)
    : Cipher(value.Type) {}

Cipher::~Cipher() {
    EVP_CIPHER_CTX_free(this->ctx);
    this->ctx = nullptr;

    CMAC_CTX_free(this->cmac_ctx);
    this->cmac_ctx = nullptr;

    this->evptype = nullptr;
}

int Cipher::GetBlockSize() {
    return this->block_size;
}

int Cipher::GetIvSize() {
    return this->iv_size;
}

int Cipher::GetKeySize() {
    return this->key_size;
}

CipherType Cipher::GetCipherType() {
    return this->Type;
}

void Cipher::EncryptInit(const byte_vector& key, const byte_vector& iv,
                         bool padding) {
    if (EVP_EncryptInit_ex(this->ctx, this->evptype, nullptr, key.c_data(),
                           iv.c_data()) == 0)
        throw OpenSSLException("EVP_EncryptInit_ex", __func__, __LINE__, true);

    EVP_CIPHER_CTX_set_padding(this->ctx, (padding ? 1 : 0));
}

byte_vector Cipher::EncryptUpdate(const byte_vector& data) {
    byte_vector outb(data.size() + this->block_size);
    int outl;

    if (EVP_EncryptUpdate(this->ctx, outb.c_data(), &outl, data.c_data(),
                          data.size()) == 0)
        throw OpenSSLException("EVP_EncryptUpdate", __func__, __LINE__, true);
    outb.resize(outl);
    return outb;
}

byte_vector Cipher::EncryptFinal() {
    byte_vector outb(this->block_size);
    int outl;

    if (EVP_EncryptFinal_ex(this->ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("EVP_EncryptFinal_ex", __func__, __LINE__, true);
    outb.resize(outl);
    return outb;
}

byte_vector Cipher::Encrypt(const byte_vector& key, const byte_vector& data,
                            const byte_vector& iv, bool padding) {
    byte_vector outb, t;

    EncryptInit(key, iv, padding);
    outb = EncryptUpdate(data);
    t = EncryptFinal();

    outb += t;
    return outb;
}

void Cipher::DecryptInit(const byte_vector& key, const byte_vector& iv,
                         bool padding) {
    if (EVP_DecryptInit_ex(this->ctx, this->evptype, nullptr, key.c_data(),
                           iv.c_data()) == 0)
        throw OpenSSLException("EVP_DecryptInit_ex", __func__, __LINE__, true);

    EVP_CIPHER_CTX_set_padding(this->ctx, (padding ? 1 : 0));
}

byte_vector Cipher::DecryptUpdate(const byte_vector& data) {
    byte_vector outb(data.size() + this->block_size);
    int outl;

    if (EVP_DecryptUpdate(this->ctx, outb.c_data(), &outl, data.c_data(),
                          data.size()) == 0)
        throw OpenSSLException("EVP_DecryptUpdate", __func__, __LINE__, true);
    outb.resize(outl);
    return outb;
}

byte_vector Cipher::DecryptFinal() {
    byte_vector outb(this->block_size);
    int outl;

    if (EVP_DecryptFinal_ex(this->ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("EVP_DecryptFinal_ex", __func__, __LINE__, true);
    outb.resize(outl);
    return outb;
}

byte_vector Cipher::Decrypt(const byte_vector& key, const byte_vector& data,
                            const byte_vector& iv, bool padding) {
    byte_vector outb, t;

    DecryptInit(key, iv, padding);
    outb = DecryptUpdate(data);
    t = DecryptFinal();

    outb += t;
    return outb;
}

void Cipher::CmacInit(const byte_vector& key) {
    if (CMAC_Init(this->cmac_ctx, key.c_data(), key.size(), this->evptype,
                  nullptr) == 0)
        throw OpenSSLException("CMAC_Init", __func__, __LINE__, true);
}

void Cipher::CmacUpdate(const byte_vector& data) {
    if (CMAC_Update(this->cmac_ctx, data.c_data(), data.size()) == 0)
        throw OpenSSLException("CMAC_Update", __func__, __LINE__, true);
}

byte_vector Cipher::CmacFinal() {
    byte_vector outb(this->block_size);
    size_t outl;

    if (CMAC_Final(this->cmac_ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("CMAC_Final", __func__, __LINE__, true);

    outb.resize(outl);
    return outb;
}

byte_vector Cipher::Cmac(const byte_vector& key, const byte_vector& data) {
    CmacInit(key);
    CmacUpdate(data);
    return CmacFinal();
}

byte_vector Cipher::CmacFile(const byte_vector& key, string file) {
    byte_vector buf(1024);
    ifstream in(file, ifstream::binary);
    off_t flen;

    if (!in)
        throw OpenSSLException("Unable to open file: " + file, __func__,
                               __LINE__);

    in.seekg(0, in.end);
    flen = in.tellg();
    in.seekg(0, in.beg);

    CmacInit(key);

    while (flen > 0) {
        if (flen < (off_t)buf.size())
            buf.resize(flen);

        in.read(reinterpret_cast<char*>(buf.data()), buf.size());
        CmacUpdate(buf);
        flen -= buf.size();
    }

    return CmacFinal();
}
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken
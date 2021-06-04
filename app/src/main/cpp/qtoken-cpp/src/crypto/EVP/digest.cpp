#include <fstream>

#include "crypto/exception.hpp"
#include "crypto/EVP/digest.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
namespace EVP {
const EVP_MD* Digest::GetEvpType(DigestType type) {
    switch (type) {
    case DigestType::SHA1:
        return EVP_sha1();
    case DigestType::SHA224:
        return EVP_sha224();
    case DigestType::SHA256:
        return EVP_sha256();
    case DigestType::SHA384:
        return EVP_sha384();
    case DigestType::SHA512:
        return EVP_sha512();
    default:
        throw OpenSSLException("invalid Digest type", __func__, __LINE__);
    }
}

Digest::Digest(DigestType type) {
    this->ctx = EVP_MD_CTX_create();
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_MD_CTX_create", __func__, __LINE__, true);

    this->hmac_ctx = HMAC_CTX_new();
    if (this->hmac_ctx == nullptr)
        throw OpenSSLException("HMAC_CTX_new", __func__, __LINE__, true);

    this->evptype = GetEvpType(type);
    this->Type = type;

    this->md_size = EVP_MD_size(this->evptype);
    this->block_size = EVP_MD_block_size(this->evptype);
}

Digest::Digest(const Digest& value)
    : Digest(value.Type) {}

Digest::~Digest() {
    EVP_MD_CTX_free(this->ctx);
    this->ctx = nullptr;

    HMAC_CTX_free(this->hmac_ctx);
    this->hmac_ctx = nullptr;

    this->evptype = nullptr;
}

int Digest::GetBlockSize() {
    return this->block_size;
}

int Digest::GetMDSize() {
    return this->md_size;
}

DigestType Digest::GetDigestType() {
    return this->Type;
}

void Digest::HashInit() {
    if (EVP_DigestInit(this->ctx, this->evptype) == 0)
        throw OpenSSLException("EVP_DigestInit", __func__, __LINE__, true);
}

void Digest::HashUpdate(const byte_vector& data) {
    if (EVP_DigestUpdate(this->ctx, data.c_data(), data.size()) == 0)
        throw OpenSSLException("EVP_DigestUpdate", __func__, __LINE__, true);
}

byte_vector Digest::HashFinal() {
    byte_vector outb(this->md_size);
    unsigned int outl = outb.size();

    if (EVP_DigestFinal(this->ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("EVP_DigestFinal", __func__, __LINE__, true);

    outb.resize(outl);
    return outb;
}

byte_vector Digest::Hash(const byte_vector& data) {
    HashInit();
    HashUpdate(data);
    return HashFinal();
}

byte_vector Digest::HashFile(string file) {
    byte_vector buf(1024);
    ifstream in(file, ifstream::binary);
    off_t flen;

    if (!in)
        throw OpenSSLException("Unable to open file: " + file, __func__,
                               __LINE__);

    in.seekg(0, in.end);
    flen = in.tellg();
    in.seekg(0, in.beg);

    HashInit();

    while (flen > 0) {
        if (flen < (off_t)buf.size())
            buf.resize(flen);

        in.read(reinterpret_cast<char*>(buf.data()), buf.size());
        HashUpdate(buf);
        flen -= buf.size();
    }

    return HashFinal();
}

void Digest::HmacInit(const byte_vector& key) {
    if (HMAC_Init_ex(this->hmac_ctx, key.c_data(), key.size(), this->evptype,
                     nullptr) == 0)
        throw OpenSSLException("HMAC_Init_ex", __func__, __LINE__, true);
}

void Digest::HmacUpdate(const byte_vector& data) {
    if (HMAC_Update(this->hmac_ctx, data.c_data(), data.size()) == 0)
        throw OpenSSLException("HMAC_Update", __func__, __LINE__, true);
}

byte_vector Digest::HmacFinal() {
    byte_vector outb(this->md_size);
    unsigned int outl = outb.size();

    if (HMAC_Final(this->hmac_ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("HMAC_Final", __func__, __LINE__, true);

    outb.resize(outl);
    return outb;
}

byte_vector Digest::Hmac(const byte_vector& key, const byte_vector& data) {
    HmacInit(key);
    HmacUpdate(data);
    return HmacFinal();
}

byte_vector Digest::HmacFile(const byte_vector& key, string file) {
    byte_vector buf(1024);
    ifstream in(file, ifstream::binary);
    off_t flen;

    if (!in)
        throw OpenSSLException("Unable to open file: " + file, __func__,
                               __LINE__);

    in.seekg(0, in.end);
    flen = in.tellg();
    in.seekg(0, in.beg);

    HmacInit(key);

    while (flen > 0) {
        if (flen < (off_t)buf.size())
            buf.resize(flen);

        in.read(reinterpret_cast<char*>(buf.data()), buf.size());
        HmacUpdate(buf);
        flen -= buf.size();
    }

    return HmacFinal();
}
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken

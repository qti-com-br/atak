#include <openssl/pem.h>

#include <fstream>
#include <string>

#include "../crypto/bio.hpp"
#include "../crypto/exception.hpp"
#include "../crypto/EVP/pkey.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
namespace EVP {
PKey::PKey() {
    this->pkey = EVP_PKEY_new();
    if (this->pkey == nullptr)
        throw OpenSSLException("EVP_PKEY_new", __func__, __LINE__, true);
    this->ctx = nullptr;
    this->dgst = nullptr;
    this->Type = PKeyType::RSA;
}

PKey::PKey(EVP_PKEY* key) {
    SecureMemoryBio b;

    switch (EVP_PKEY_base_id(key)) {
    case EVP_PKEY_RSA:
    case EVP_PKEY_RSA2:
        this->Type = PKeyType::RSA;
        break;
    case EVP_PKEY_DSA:
    case EVP_PKEY_DSA1:
    case EVP_PKEY_DSA2:
    case EVP_PKEY_DSA3:
    case EVP_PKEY_DSA4:
        this->Type = PKeyType::DSA;
        break;
    case EVP_PKEY_EC:
        this->Type = PKeyType::ECDSA;
        break;
    default:
        throw OpenSSLException("Invalid EVP_PKEY type", __func__, __LINE__);
    }

    this->pkey = key;
    this->ctx = EVP_PKEY_CTX_new(this->pkey, nullptr);
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_PKEY_CTX_new", __func__, __LINE__, true);

    this->dgst = nullptr;
}

PKey::PKey(RsaKey key)
    : PKey() {
    this->Type = PKeyType::RSA;
    if (EVP_PKEY_set1_RSA(this->pkey, key.key) == 0)
        throw OpenSSLException("EVP_PKEY_set1_RSA", __func__, __LINE__, true);

    this->ctx = EVP_PKEY_CTX_new(this->pkey, nullptr);
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_PKEY_CTX_new", __func__, __LINE__, true);
}

PKey::PKey(EC key)
    : PKey() {
    this->Type = PKeyType::ECDSA;
    if (EVP_PKEY_set1_EC_KEY(this->pkey, key.key) == 0)
        throw OpenSSLException("EVP_PKEY_set1_EC_KEY", __func__, __LINE__,
                               true);

    this->ctx = EVP_PKEY_CTX_new(this->pkey, nullptr);
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_PKEY_CTX_new", __func__, __LINE__, true);
}

PKey::PKey(string file)
    : PKey() {
    auto b = Bio::ReadFile(file);
    this->pkey = PEM_read_bio_PrivateKey(b.bio, nullptr, nullptr, nullptr);
    if (this->pkey == nullptr)
        throw OpenSSLException("PEM_read_bio_PrivateKey", __func__, __LINE__,
                               true);

    switch (EVP_PKEY_base_id(this->pkey)) {
    case EVP_PKEY_RSA:
        this->Type = PKeyType::RSA;
        break;
    case EVP_PKEY_EC:
        this->Type = PKeyType::ECDSA;
        break;
    default:
        throw OpenSSLException("Unsupported private key type", __func__,
                               __LINE__);
    }

    this->ctx = EVP_PKEY_CTX_new(this->pkey, nullptr);
    if (this->ctx == nullptr)
        throw OpenSSLException("EVP_PKEY_CTX_new", __func__, __LINE__, true);
}

PKey::~PKey() {
    EVP_PKEY_free(this->pkey);
    this->pkey = nullptr;
    EVP_PKEY_CTX_free(this->ctx);
    this->ctx = nullptr;
    delete this->dgst;
    this->dgst = nullptr;
}

byte_vector PKey::GetPrivateKey(EVP_PKEY* key) {
    SecureMemoryBio b;

    if (PEM_write_bio_PKCS8PrivateKey(b.bio, key, nullptr, nullptr, 0, nullptr,
                                      nullptr) == 0)
        throw OpenSSLException("PEM_write_bio_PrivateKey", __func__, __LINE__,
                               true);

    return b.GetData();
}

byte_vector PKey::GetPublicKey(EVP_PKEY* key) {
    MemoryBio b;

    if (i2d_PUBKEY_bio(b.bio, key) == 0)
        throw OpenSSLException("i2d_PUBKEY_bio", __func__, __LINE__, true);

    return b.GetData();
}

void PKey::SetEcdhKdfMd(EVP_PKEY_CTX* ctx, DigestType dtype) {
    if (EVP_PKEY_derive_init(ctx) <= 0)
        throw OpenSSLException("EVP_PKEY_derive_init", __func__, __LINE__,
                               true);

    if (EVP_PKEY_CTX_set_ecdh_kdf_md(ctx, (void*)Digest::GetEvpType(dtype)) <=
        0)
        throw OpenSSLException("EVP_PKEY_CTX_set_ecdh_kdf_md", __func__,
                               __LINE__, true);
}

void PKey::SetRsaPssMd(EVP_PKEY_CTX* ctx) {
    if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_PSS_PADDING) <= 0)
        throw OpenSSLException("EVP_PKEY_CTX_set_rsa_padding", __func__,
                               __LINE__, true);

    if (EVP_PKEY_CTX_set_rsa_pss_saltlen(ctx, RSA_PSS_SALTLEN_MAX) <= 0)
        throw OpenSSLException("EVP_PKEY_CTX_set_rsa_pss_saltlen", __func__,
                               __LINE__, true);
}

void PKey::SetRsaOaepMd(EVP_PKEY_CTX* ctx, DigestType dtype) {
    if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING) <= 0)
        throw OpenSSLException("EVP_PKEY_CTX_set_rsa_padding", __func__,
                               __LINE__, true);

    if (EVP_PKEY_CTX_set_rsa_oaep_md(ctx, (void*)Digest::GetEvpType(dtype)) <=
        0)
        throw OpenSSLException("EVP_PKEY_CTX_set_rsa_oaep_md", __func__,
                               __LINE__, true);
}

byte_vector PKey::GetPrivateKey() {
    return GetPrivateKey(this->pkey);
}

PKeyType PKey::GetType() {
    return this->Type;
}

void PKey::SignInit(DigestType dtype) {
    delete this->dgst;
    this->dgst = new Digest(dtype);

    if (EVP_DigestSignInit(this->dgst->ctx, nullptr, this->dgst->evptype,
                           nullptr, this->pkey) == 0)
        throw OpenSSLException("EVP_DigestSignInit", __func__, __LINE__, true);
}

void PKey::SignUpdate(const byte_vector& data) {
    if (EVP_DigestSignUpdate(this->dgst->ctx, data.c_data(), data.size()) == 0)
        throw OpenSSLException("EVP_DigestSignUpdate", __func__, __LINE__,
                               true);
}

byte_vector PKey::SignFinal() {
    byte_vector outb;
    size_t outl;

    if (EVP_DigestSignFinal(this->dgst->ctx, nullptr, &outl) == 0)
        throw OpenSSLException("EVP_DigestSignFinal", __func__, __LINE__, true);

    outb.resize(outl);
    if (EVP_DigestSignFinal(this->dgst->ctx, outb.c_data(), &outl) == 0)
        throw OpenSSLException("EVP_DigestSignFinal", __func__, __LINE__, true);

    delete this->dgst;
    this->dgst = nullptr;
    outb.resize(outl);
    return outb;
}

byte_vector PKey::Sign(DigestType dtype, const byte_vector& data) {
    SignInit(dtype);
    SignUpdate(data);
    return SignFinal();
}

byte_vector PKey::SignFile(DigestType dtype, string file) {
    byte_vector buf(1024);
    ifstream in(file, ifstream::binary);
    off_t flen;

    if (!in)
        throw OpenSSLException("Unable to open file: " + file, __func__,
                               __LINE__);

    in.seekg(0, in.end);
    flen = in.tellg();
    in.seekg(0, in.beg);

    SignInit(dtype);

    while (flen > 0) {
        if (flen < (off_t)buf.size())
            buf.resize(flen);

        in.read(reinterpret_cast<char*>(buf.data()), buf.size());
        SignUpdate(buf);
        flen -= buf.size();
    }

    return SignFinal();
}

void PKey::VerifyInit(DigestType dtype) {
    delete this->dgst;
    this->dgst = new Digest(dtype);

    if (EVP_DigestVerifyInit(this->dgst->ctx, nullptr, this->dgst->evptype,
                             nullptr, this->pkey) == 0)
        throw OpenSSLException("EVP_DigestVerifyInit", __func__, __LINE__,
                               true);
}

void PKey::VerifyUpdate(const byte_vector& data) {
    if (EVP_DigestVerifyUpdate(this->dgst->ctx, data.c_data(), data.size()) ==
        0)
        throw OpenSSLException("EVP_DigestVerifyUpdate", __func__, __LINE__,
                               true);
}

bool PKey::VerifyFinal(const byte_vector& sig) {
    int rv = EVP_DigestVerifyFinal(this->dgst->ctx, sig.c_data(), sig.size());
    switch (rv) {
    case 1:
        return true;
    case 0:
        return false;
    default:
        throw OpenSSLException("EVP_DigestVerifyFinal", __func__, __LINE__,
                               true);
    }
}

bool PKey::Verify(DigestType dtype, const byte_vector& data,
                  const byte_vector& sig) {
    VerifyInit(dtype);
    VerifyUpdate(data);
    return VerifyFinal(sig);
}

bool PKey::VerifyFile(DigestType dtype, string file, const byte_vector& sig) {
    byte_vector buf(1024);
    ifstream in(file, ifstream::binary);
    off_t flen;

    if (!in)
        throw OpenSSLException("Unable to open file: " + file, __func__,
                               __LINE__);

    in.seekg(0, in.end);
    flen = in.tellg();
    in.seekg(0, in.beg);

    VerifyInit(dtype);
    while (flen > 0) {
        if (flen < (off_t)buf.size())
            buf.resize(flen);

        in.read(reinterpret_cast<char*>(buf.data()), buf.size());
        VerifyUpdate(buf);
        flen -= buf.size();
    }

    return VerifyFinal(sig);
}

byte_vector PKey::Encrypt(const byte_vector& data) {
    if (EVP_PKEY_encrypt_init(this->ctx) <= 0)
        throw OpenSSLException("EVP_PKEY_encrypt_init", __func__, __LINE__,
                               true);

    size_t outl;
    if (EVP_PKEY_encrypt(this->ctx, nullptr, &outl, data.c_data(),
                         data.size()) <= 0)
        throw OpenSSLException("EVP_PKEY_encrypt", __func__, __LINE__, true);

    byte_vector outb(outl);
    if (EVP_PKEY_encrypt(this->ctx, outb.c_data(), &outl, data.c_data(),
                         data.size()) <= 0)
        throw OpenSSLException("EVP_PKEY_encrypt", __func__, __LINE__, true);

    outb.resize(outl);
    return outb;
}

void PKey::SetEcdhKdfMd(DigestType dtype) {
    if (this->Type != PKeyType::ECDSA)
        return;

    SetEcdhKdfMd(this->ctx, dtype);
}

/*
 * Future feature, allow user to pass in salt length.
 */
void PKey::SetRsaPssMd() {
    if (this->Type != PKeyType::RSA)
        return;

    SetRsaPssMd(this->ctx);
}

void PKey::SetRsaOaepMd(DigestType dtype) {
    if (this->Type != PKeyType::RSA)
        return;

    SetRsaOaepMd(this->ctx, dtype);
}

byte_vector PKey::GetPublicKey() {
    return GetPublicKey(this->pkey);
}
}  // namespace EVP
}  // namespace Crypto
}  // namespace Qtoken

#include "../crypto/x509.hpp"
#include <string>
#include "../crypto/bio.hpp"
#include "../crypto/exception.hpp"

#include <openssl/pem.h>

using namespace std;

namespace Qtoken {
namespace Crypto {
x509::x509() {
    this->cert = nullptr;
}

void x509::SetCertificate(const byte_vector& cert) {
    MemoryBio bio;
    bio.Write(cert);
    this->cert = d2i_X509_bio(bio.bio, nullptr);
    if (this->cert == nullptr)
        throw OpenSSLException("d2i_X509_bio", __func__, __LINE__, true);
}

byte_vector x509::GetCertificate(X509* x) {
    MemoryBio bio;

    if (i2d_X509_bio(bio.bio, x) == 0)
        throw OpenSSLException("i2d_X509_bio", __func__, __LINE__, true);

    return bio.GetData();
}

x509::x509(const byte_vector& cert)
    : x509() {
    SetCertificate(cert);
}

x509::x509(const string& file) {
    auto bio = Bio::ReadFile(file);
    this->cert = PEM_read_bio_X509_AUX(bio.bio, nullptr, nullptr, nullptr);
    if (this->cert == nullptr) {
        // PEM format didn't work, try DER
        bio = Bio::ReadFile(file);
        this->cert = d2i_X509_bio(bio.bio, nullptr);
        if (this->cert == nullptr)
            throw OpenSSLException("d2i_X509_bio", __func__, __LINE__, true);
    }
}

x509::x509(const x509& cert)
    : x509() {
    SetCertificate(GetCertificate(cert.cert));
}

x509::~x509() {
    X509_free(this->cert);
    this->cert = nullptr;
}

EVP::PKey x509::GetPublicKey() {
    EVP_PKEY* key = X509_get_pubkey(this->cert);
    if (key == nullptr)
        throw OpenSSLException("X509_get_pubkey", __func__, __LINE__, true);

    auto pkey = new EVP::PKey(key);
    return *pkey;
}
}  // namespace Crypto
}  // namespace Qtoken

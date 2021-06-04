#include <string>

#include <openssl/x509.h>

#include "../crypto/EVP/pkey.hpp"
#include "../crypto/bio.hpp"
#include "../crypto/ec.hpp"
#include "../crypto/exception.hpp"
#include "../crypto/random.hpp"

using namespace std;

namespace Qtoken {
namespace Crypto {
EC::EC() {
    this->key = nullptr;
    this->group = nullptr;
}

byte_vector EC::GetPrivateKey(EC_KEY *key) {
    SecureMemoryBio bio;
    int siz = i2d_ECPrivateKey_bio(bio.bio, key);
    if (siz < 0)
        throw OpenSSLException("i2d_ECPrivateKey_bio", __func__, __LINE__,
                               true);

    return bio.GetData();
}

/*
 * This is a raw EC public key. It is almost never used in this format. In case
 * it is needed, we're leaving this function here. EC public keys are point +
 * group.
 */
byte_vector EC::GetPublicKey(EC_KEY *key, const EC_GROUP *group) {
    const EC_POINT *point = EC_KEY_get0_public_key(key);
    if (point == nullptr)
        throw OpenSSLException("EC_KEY_get0_public_key", __func__, __LINE__,
                               true);

    size_t len = EC_POINT_point2oct(group, point, POINT_CONVERSION_UNCOMPRESSED,
                                    nullptr, 0, nullptr);
    if (len == 0)
        throw OpenSSLException("EC_POINT_point2oct", __func__, __LINE__, true);

    byte_vector buf(len);
    if (EC_POINT_point2oct(group, point, POINT_CONVERSION_UNCOMPRESSED,
                           buf.c_data(), buf.size(), nullptr) == 0)
        throw OpenSSLException("EC_POINT_point2oct", __func__, __LINE__, true);

    return buf;
}

byte_vector EC::GetPrivateKey() {
    return GetPrivateKey(this->key);
}

void EC::SetPrivateKey(const byte_vector &srckey) {
    SecureMemoryBio bio;
    bio.Write(srckey);
    this->key = d2i_ECPrivateKey_bio(bio.bio, nullptr);
    if (this->key == nullptr)
        throw OpenSSLException("d2i_ECPrivateKey_bio", __func__, __LINE__,
                               true);

    this->group = EC_KEY_get0_group(this->key);
    if (this->group == nullptr)
        throw OpenSSLException("EC_KEY_get0_group", __func__, __LINE__, true);
}

EC::EC(const byte_vector &key)
    : EC() {
    SetPrivateKey(key);
}

EC::EC(EC_KEY *key)
    : EC() {
    SetPrivateKey(GetPrivateKey(key));
}

void EC::NewFromCurveName(const string &curveName, bool compress) {
    EC_GROUP *group;
    int nid;

    this->key = EC_KEY_new();
    if (this->key == nullptr)
        throw OpenSSLException("EC_KEY_new", __func__, __LINE__, true);

    Random::Seed(512);

    nid = OBJ_sn2nid(curveName.c_str());
    if (nid == 0)
        nid = EC_curve_nist2nid(curveName.c_str());

    if (nid == 0)
        throw OpenSSLException("Unknown curve: " + curveName, __func__,
                               __LINE__, true);

    group = EC_GROUP_new_by_curve_name(nid);
    if (group == nullptr)
        throw OpenSSLException("EC_GROUP_new_by_curve_name", __func__, __LINE__,
                               true);

    EC_GROUP_set_asn1_flag(group, OPENSSL_EC_NAMED_CURVE);
    if (compress)
        EC_GROUP_set_point_conversion_form(group, POINT_CONVERSION_COMPRESSED);
    else
        EC_GROUP_set_point_conversion_form(group,
                                           POINT_CONVERSION_UNCOMPRESSED);

    if (EC_KEY_set_group(this->key, group) == 0)
        throw OpenSSLException("EC_KEY_set_group", __func__, __LINE__, true);

    if (EC_KEY_generate_key(this->key) == 0)
        throw OpenSSLException("EC_KEY_generate_key", __func__, __LINE__, true);

    if (compress)
        EC_KEY_set_conv_form(this->key, POINT_CONVERSION_COMPRESSED);
    else
        EC_KEY_set_conv_form(this->key, POINT_CONVERSION_UNCOMPRESSED);

    this->group = group;
}

EC::EC(const string &name)
    : EC() {
    NewFromCurveName(name);
}

EC::EC(const EC &key)
    : EC(key.key) {}

EC::~EC() {
    EC_KEY_free(this->key);
    this->key = nullptr;
    // group gets freed in EC_KEY_free() call
    this->group = nullptr;
}

/*
 * Raw EC public keys are not ASN.1 SubjectPublicKeyInfo structure. Use
 * EVP::PKey to get this done.
 */
byte_vector EC::GetPublicKey() {
    EVP::PKey pkey(*this);
    return pkey.GetPublicKey();
}
}  // namespace Crypto
}  // namespace Qtoken
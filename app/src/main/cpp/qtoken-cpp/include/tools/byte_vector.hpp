#ifndef _BYTE_VECTOR_H
#define _BYTE_VECTOR_H

#include <string>
#include <vector>

// typedef unsigned char byte;

namespace Qtoken {
class byte_vector : private std::vector<std::byte> {
private:
    typedef std::vector<std::byte> vector;

public:
    // Begin copying fields / methods of vector
    using vector::vector;  // copy constructor
    using vector::operator[];
    using vector::operator=;

    using vector::at;
    using vector::begin;
    using vector::data;
    using vector::end;
    using vector::insert;
    using vector::push_back;
    using vector::resize;
    using vector::size;
    // End copying fields / methods of vector

    // Begin templates
    template <typename T>
    void SetFromInt(const T value) {
        size_t shft = (sizeof(T) * 8) - 8;

        this->resize(sizeof(T));
        for (size_t i = 0; i < this->size(); i++, shft -= 8)
            this->at(i) = static_cast<std::byte>((value >> shft) & 0xff);
    }

    template <typename T>
    T GetInt() {
        T ret = 0;

        if (this->size() == 0)
            return ret;

        ret = static_cast<T>(this->at(0));
        for (size_t i = 1; i < this->size(); i++) {
            ret <<= 8;
            ret |= static_cast<T>(this->at(i));
        }

        return ret;
    }
    // End templates

    // Begin extra constructors
    byte_vector(const unsigned char *, size_t);
    // End extra constructors

    // Begin extra methods
    unsigned char *c_data() noexcept;
    const unsigned char *c_data() const noexcept;
    std::string ToHexString(bool upper = false);
    void FromHexString(const std::string &);
    std::string ToBase64();
    void FromBase64(const std::string &);
    void FromString(const std::string &);
    byte_vector SubArray(size_t);
    byte_vector SubArray(size_t, size_t);
    // End extra methods

    // Begin extra operators
    byte_vector operator+=(const byte_vector &);
    byte_vector operator^=(const byte_vector &);
    bool operator==(const byte_vector &);
    bool operator!=(const byte_vector &);
    // End extra operators
};

byte_vector operator+(const byte_vector &, const byte_vector &);
byte_vector operator^(const byte_vector &, const byte_vector &);
}  // namespace Qtoken

#endif /* !_BYTE_VECTOR_H */

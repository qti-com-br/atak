#ifndef TYPES_H
#define TYPES_H

#include <functional>
#include <string>
#include <vector>

namespace Qtoken {

using ull = unsigned long long;

class Chunker;
class CryptoReceipt;
// NOTE: std::vector<bool> is not a container and does not support
// every vector operation in the ways we might expect
using Bitlist = std::vector<bool>;
using Bytelist = std::vector<unsigned char>;
using Writer = std::pair<Chunker*, CryptoReceipt*>;
using ChunkOperator = std::function<void(Writer&)>;

using Key = std::string;
using Val = std::string;
using IP = std::string;
using Port = std::string;

// type for signed/unsigned bits default
using Byte_8 = int8_t;
using Byte = Byte_8;
using UByte_8 = uint8_t;
using UByte = UByte_8;

struct Addr {
    IP ip;
    Port port;

    Addr(const IP& ip, const Port& port)
        : ip(ip)
        , port(port) {}
};

}  // namespace Qtoken

#endif  // TYPES_H

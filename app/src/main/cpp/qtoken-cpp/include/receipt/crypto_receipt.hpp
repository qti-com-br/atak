#ifndef CRYPTO_RECEIPT_H
#define CRYPTO_RECEIPT_H

#include <fstream>
#include <sstream>
#include <unordered_map>

#include "tools/types.hpp"
#include "tools/utils.hpp"
#include "tools/uuid.hpp"

namespace Qtoken {

class CryptoReceipt {
public:
    CryptoReceipt() {}

    explicit CryptoReceipt(const std::string& file_path) { load(file_path); }
    explicit CryptoReceipt(Bytelist receipt_bytes);
    explicit CryptoReceipt(const std::unordered_map<Key, Val>& data)
        : data(data) {}
    Val get(Key k) const {
        return (data.find(k) != data.end()) ? data.at(k) : "";
    }
    void put(const Key& k, const Val& v) { data[k] = v; }
    void save(std::string dir_path);
    void load(std::string file_path);
    std::stringstream serialize();
    bool operator==(CryptoReceipt& rhs);

private:
    std::unordered_map<Key, Val> data;
};

}  // namespace Qtoken

#endif  // CRYPTO_RECEIPT_H

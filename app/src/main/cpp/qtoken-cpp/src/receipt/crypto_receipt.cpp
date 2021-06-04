#include "receipt/crypto_receipt.hpp"

using namespace Qtoken;

/**
 * Constructs crypto receipt from byte list.
 * @param receipt_bytes bytes to build receipt from.
 */
CryptoReceipt::CryptoReceipt(Bytelist receipt_bytes) {
    std::string receipt_str(receipt_bytes.begin(), receipt_bytes.end());
    std::stringstream ss;
    ss.str(receipt_str);

    std::string k, v;
    while (ss >> k >> v) {
        data[Key(k)] = Val(v);
    }
}

/**
 * Serializes receipt and saves resulting text to disc.
 * @param dir_path file save path.
 */
void CryptoReceipt::save(std::string dir_path) {
    std::ofstream output(dir_path);
    auto ss = this->serialize();
    output << ss.str();
}

/**
 * Reads serialized key value pairs from disc and adds to receipt.
 * @param file_path serialized key value pairs path.
 */
void CryptoReceipt::load(std::string file_path) {
    std::ifstream input(file_path);
    std::string k, v;
    while (input >> k >> v) {
        data[Key(k)] = Val(v);
    }
}

/**
 * Compares all values of left hand side receipt
 * with values of right hand side.
 * Note: Only values present in the left hand side receipt
 * are used for the comparison operation.
 * @param rhs expression right hand side receipt.
 * @return crypto receipt equality.
 */
bool CryptoReceipt::operator==(CryptoReceipt& rhs) {
    for (const auto& [key, value] : data) {
        if (rhs.get(key) != value)
            return false;
    }
    return true;
}

/**
 * Serializes receipt key value pairs into a string stream.
 * @return serialized string stream.
 */
std::stringstream CryptoReceipt::serialize() {
    std::stringstream ss;
    for (const auto& [key, value] : data) {
        ss << key << " " << value << '\n';
    }
    return ss;
}

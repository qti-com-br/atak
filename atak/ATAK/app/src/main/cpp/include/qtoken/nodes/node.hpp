#ifndef NODE_H
#define NODE_H

#include "Poco/Event.h"
#include "Poco/Net/ParallelSocketAcceptor.h"
#include "Poco/Net/ServerSocket.h"
#include "Poco/Net/SocketAcceptor.h"
#include "Poco/Net/SocketReactor.h"
#include "Poco/Util/HelpFormatter.h"
#include "Poco/Util/Option.h"
#include "Poco/Util/OptionSet.h"
#include "Poco/Util/ServerApplication.h"

#include "coders/concurrent_decoder.hpp"
#include "coders/concurrent_encoder.hpp"
#include "coders/decoder.hpp"
#include "coders/encoder.hpp"
#include "coders/entanglement_decoder.hpp"
#include "coders/entanglement_encoder.hpp"
#include "coders/pipeline.cpp"
#include "coders/pipeline.hpp"
#include "coders/polar_decoder.hpp"
#include "coders/polar_encoder.hpp"
#include "nodes/host.hpp"
#include "receipt/connection_handler.hpp"
#include "receipt/crypto_receipt.hpp"
#include "receipt/receipt_session.hpp"

#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

namespace P = Poco;
namespace P_N = P::Net;
namespace P_U = P::Util;

namespace Qtoken {

class ReceiptServer;
class ReceiptConnectionHandler;

class Node : public Host {
private:
    bool is_lib;
    std::shared_ptr<kademlia::session> node;
    int node_port;
    int node_receipt_port;
    libconfig::Config& cfg;
    P_N::SocketReactor reactor;
    P_N::ServerSocket svs;
    P_N::ParallelSocketAcceptor<ReceiptConnectionHandler, SocketReactor>
        acceptor;
    P::Thread receipt_thread;

public:
    // Command parsing
    void processInput() override;
    void printCommands() override;
    int handleCommands(std::stringstream& input);

    // Network functions
    std::vector<char> doGet(const std::string& key);
    void doPut(const std::string& key, const std::vector<char>& value);
    CryptoReceipt doSpread(const std::string& file_path);
    CryptoReceipt doShare(const std::string& file_path,
                          const std::string& peer_ip,
                          const std::string& peer_port);

    void rebuild(std::vector<std::string> keys, std::string output_file);
    void runReceiptEndpoint();
    void encryptReceipt(std::ifstream& receipt_path, unsigned char* public_key,
                        unsigned char* ciphertext);
    const std::string decryptReceipt(std::ifstream& enc_receipt,
                                     unsigned char* public_key,
                                     unsigned char* private_key);

public:
    // overload Host constructor
    Node(const std::string& node_port, const std::string& node_receipt_port,
         const std::string& bootstrap_addr, bool is_lib,
         libconfig::Config& cfg);
    ~Node() {}
    Chunker doGather(const std::string& receipt_file_path);
    Chunker doGather(CryptoReceipt cr);
    // Override run
    int run() override;
};

}  // namespace Qtoken

#endif  // NODE_H

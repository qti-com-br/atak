#ifndef NODE_H
#define NODE_H

#ifdef __ANDROID__
#include <dlfcn.h>
#include <jni.h>
#endif

#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <unistd.h>
#include <future>
#include <iterator>
#include <kademlia/first_session.hpp>
#include <kademlia/session.hpp>

#include "Poco/Event.h"
#include "Poco/Net/ParallelSocketAcceptor.h"
#include "Poco/Net/ServerSocket.h"
#include "Poco/Net/SocketAcceptor.h"
#include "Poco/Net/SocketReactor.h"
#include "Poco/Util/HelpFormatter.h"
#include "Poco/Util/Option.h"
#include "Poco/Util/OptionSet.h"

#include "coders/concurrent_decoder.hpp"
#include "coders/concurrent_encoder.hpp"
#include "coders/decoder.hpp"
#include "coders/encoder.hpp"
#include "coders/entanglement_decoder.hpp"
#include "coders/entanglement_encoder.hpp"
#include "coders/pipeline.hpp"
#include "coders/polar_decoder.hpp"
#include "coders/polar_encoder.hpp"
#include "globals/config.hpp"
#include "globals/globals.hpp"
#include "globals/logger.hpp"
#include "globals/strings.hpp"
#include "receipt/crypto_receipt.hpp"
#include "receipt/receipt_service.hpp"
#include "receipt/receipt_session.hpp"
#include "receipt/share_connection_handler.hpp"
#include "server/handlers/form_request_handler_factory.hpp"
#include "server/http_form_server.hpp"
#include "server/http_server_thread.hpp"
#include "tools/chunker.hpp"
#include "tools/types.hpp"

namespace Qtoken {

class ShareConnectionHandler;

class Node {
public:
    Node(const std::string& node_port, const std::string& node_receipt_port,
         const std::string& node_server_port, const std::string& bootstrap_addr,
         bool is_lib);
    ~Node() {}

    int run();

    // DHT Functionality
    std::vector<kademlia::kad_peer> getPeers();
    void doPut(const std::string& key, const std::vector<unsigned char>& value);
    std::vector<unsigned char> doGet(const std::string& key);
    CryptoReceipt doSpread(const std::string& file_path);
    CryptoReceipt doSpread(std::istream& file_istream);
    void doSpread(Writer& w);
    Chunker doGather(const std::vector<unsigned char>& receipt_bytes);
    Chunker doGather(const std::string& receipt_file_path);
    Chunker doGather(const CryptoReceipt& cr);

    // Receipt passing
    std::vector<CryptoReceipt> doShare(std::istream& file_istream,
                                       const std::string& peer_ip,
                                       const std::string& peer_port);
    std::vector<CryptoReceipt> doShare(std::istream& file_istream,
                                       const std::string& peer_ip,
                                       const std::string& peer_port,
                                       bool continue_stream);
    std::vector<CryptoReceipt> doShare(const std::string& file_path,
                                       const std::string& peer_ip,
                                       const std::string& peer_port);
    std::vector<CryptoReceipt> doShare(Writer w, const std::string& peer_ip,
                                       const std::string& peer_port);
    std::vector<CryptoReceipt> doShare(Writer w, const std::string& peer_ip,
                                       const std::string& peer_port,
                                       bool continue_stream);

    // Streaming
    inline bool is_active_stream_session() {
        // return active_streams.find(session_token) != active_streams.end();
        return active_stream != "";
    }

    void add_stream_session(std::string session_token);
    void update_stream_session(CryptoReceipt cr);
    // void update_stream_session(std::string session_token, CryptoReceipt cr);
    void end_stream_session();

    std::string get_current_stream() { return active_stream; }

private:
    std::string boot_address;
    std::string boot_port;
    std::istream* input;
    int chunk_size;

    bool is_lib;

    int node_port;
    int node_receipt_port;
    int node_server_port;
    Poco::Net::SocketReactor reactor;
    Poco::Net::ServerSocket svs;
    Poco::Net::ParallelSocketAcceptor<ShareConnectionHandler, SocketReactor>
        acceptor;
    Poco::Thread receipt_thread;
    std::shared_ptr<kademlia::session> node;

    // std::unordered_map<Bytelist, std::unique_ptr<ReceiptService>>
    std::string active_stream;
    std::unique_ptr<ReceiptService> active_rs;
    std::unique_ptr<ReceiptSession> active_stream_client;
};

}  // namespace Qtoken

#endif  // NODE_H

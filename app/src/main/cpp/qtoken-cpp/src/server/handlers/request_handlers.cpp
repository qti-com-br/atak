#include <iostream>

#include "server/handlers/request_handlers.hpp"
#include "server/handlers/share_part_handler.hpp"
#include "server/handlers/spread_part_handler.hpp"
#include "server/handlers/stream_part_handler.hpp"

using namespace Qtoken;

using Poco::URI;
using Poco::Net::HTTPResponse;
using Poco::Net::NameValueCollection;

void ExitHandler::handleRequest(HTTPServerRequest& req,
                                HTTPServerResponse& resp) {
    Log::message("http", "Server received shutdown signal");

    resp.setStatus(HTTPResponse::HTTP_OK);
    resp.setContentType("text/html");

    std::ostream& out = resp.send();

    out << "<h1>Later Days!<h1>";

    out.flush();
    // This will send a sigint to this process.
    // Poco::Process::requestTermination(Poco::Process::id());
    server_wait_for_shutdown->set();
}

void SpreadHandler::handleRequest(HTTPServerRequest& req,
                                  HTTPServerResponse& resp) {
    Log::message("http", "Server received spread request");

    Poco::JSON::Object obj;

    SpreadPartHandler partHandler(req);
    HTMLForm form(req, req.stream(), partHandler);

    obj.set("receipt", partHandler.cryptoReceipt());
    obj.set("path", partHandler.filePath());

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
    ostr.flush();
}

void ShareHandler::handleRequest(HTTPServerRequest& req,
                                 HTTPServerResponse& resp) {
    Log::message("http", "Server received share request");

    Poco::JSON::Object obj;

    SharePartHandler sharePartHandler(req);
    HTMLForm form(req, req.stream(), sharePartHandler);

    auto cr_vec = sharePartHandler.cryptoReceiptVec();
    int c = 1;
    Poco::JSON::Object::Ptr inner = new Poco::JSON::Object;

    std::string receipt_dir = Config::get("files.receipts.sent");
    std::string save_path(receipt_dir + "CR" +
                          std::to_string(get_rand_seed_uint32_t()));

    for (auto it = cr_vec.begin(); it != cr_vec.end(); ++it) {
        Poco::JSON::Array cr_arr;
        cr_arr.add(it->serialize().str());
        inner->set("cr" + std::to_string(c), cr_arr);
        c++;
    }

    std::ofstream receipt_stream(save_path, std::ios::app | std::ios::binary);
    Poco::JSON::Stringifier::stringify(inner, receipt_stream, 4, -1,
                                       Poco::JSON_PRESERVE_KEY_ORDER);

    receipt_stream.close();

    Log::message("http", "Share complete");

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    obj.set("path", save_path);

    Poco::JSON::Stringifier::stringify(obj, ostr);
    ostr.flush();
}

void StreamHandler::handleRequest(HTTPServerRequest& req,
                                  HTTPServerResponse& resp) {
    Log::message("http", "Server received stream request");

    Poco::JSON::Object obj;

    StreamPartHandler streamPartHandler(req);
    HTMLForm form(req, req.stream(), streamPartHandler);

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
    ostr.flush();
}

void GatherHandler::handleRequest(HTTPServerRequest& req,
                                  HTTPServerResponse& resp) {
    Log::message("http", "Gather request received");

    auto& stream = req.stream();
    const size_t len = req.getContentLength();
    std::string buffer(len, 0);
    stream.read(buffer.data(), len);

    Poco::JSON::Parser parser;
    Poco::JSON::Object obj;
    Poco::Dynamic::Var result = parser.parse(buffer);

    Poco::JSON::Object::Ptr object = result.extract<Poco::JSON::Object::Ptr>();
    Poco::Dynamic::Var receipt_obj = object->get("receipt");
    Poco::Dynamic::Var path_obj = object->get("path");

    auto cr_str = receipt_obj.toString();

    Bytelist cr_vec(cr_str.begin(), cr_str.end());
    auto cr = new CryptoReceipt(cr_vec);

    if (!cr->serialize().str().empty()) {
        Chunker chunker = global_node->doGather(*cr);
        try {
            chunker.rebuild(path_obj.toString());
        } catch (...) {
            Log::message("http", "corrupted receipt");
            resp.setStatusAndReason(HTTPServerResponse::HTTP_BAD_REQUEST,
                                    "corrupted receipt");
        }
        obj.set("path", path_obj.toString());
        Log::message("http", "Gather complete");
    } else {
        Log::message("http", "empty receipt");
        resp.setStatusAndReason(HTTPServerResponse::HTTP_BAD_REQUEST,
                                "empty receipt");
    }

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
}

void GetPeersHandler::handleRequest(HTTPServerRequest& req,
                                    HTTPServerResponse& resp) {
    Log::message("http", "Server received getPeers request.");

    auto ips = global_node->getPeers();
    Poco::JSON::Object obj;

    Log::message("http", "Getting Peers");

    for (auto it = ips.begin(); it != ips.end(); ++it) {
        Poco::JSON::Object::Ptr inner = new Poco::JSON::Object;
        inner->set("ip", it->peer_endpoint.address().c_str());
        inner->set("port", it->peer_endpoint.service().c_str());
        obj.set(it->peer_id.c_str(), inner);
        Log::message("http", " endpoint: " + std::string(it->peer_endpoint.service().c_str()));
    }

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
}

void PutHandler::handleRequest(HTTPServerRequest& req,
                               HTTPServerResponse& resp) {
    Log::message("http", "Put request received");

    Log::message("http", req.getURI());
    auto& stream = req.stream();
    const size_t len = req.getContentLength();
    std::string buffer(len, 0);
    stream.read(buffer.data(), len);

    Poco::JSON::Parser parser;
    Poco::JSON::Object obj;
    Poco::Dynamic::Var result = parser.parse(buffer);

    Poco::JSON::Object::Ptr object = result.extract<Poco::JSON::Object::Ptr>();
    Poco::Dynamic::Var key_obj = object->get("key");
    Poco::Dynamic::Var value_obj = object->get("value");
    std::string key_string = key_obj.toString();
    std::string value_string = value_obj.toString();

    if (key_string == "" || value_string == "") {
        std::cerr << "no k/v specified" << std::endl;
        Log::message("http", "Put failed:no input");
        resp.setStatusAndReason(HTTPServerResponse::HTTP_BAD_REQUEST,
                                "no k/v specified");
    } else {
        Bytelist v(value_string.begin(), value_string.end());

        global_node->doPut(key_string, v);

        Log::message("http", "Put successful.");
        Log::message("http", "Key: " + key_string);
        Log::message("http", "Value: " + value_string);

        resp.setStatusAndReason(HTTPResponse::HTTP_OK, "put successful");
    }
    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
    ostr.flush();
}

void GetHandler::handleRequest(HTTPServerRequest& req,
                               HTTPServerResponse& resp) {
    Log::message("http", "Get request received");

    Log::message("http", req.getURI());

    auto& stream = req.stream();
    const size_t len = req.getContentLength();
    std::string buffer(len, 0);
    stream.read(buffer.data(), len);

    Poco::JSON::Parser parser;
    Poco::JSON::Object obj;
    Poco::Dynamic::Var result = parser.parse(buffer);

    Poco::JSON::Object::Ptr object = result.extract<Poco::JSON::Object::Ptr>();
    Poco::Dynamic::Var key_obj = object->get("key");

    Bytelist buff_channel = std::move(global_node->doGet(key_obj.toString()));

    if (buff_channel.size() > 0) {
        std::string value_string =
            std::string(buff_channel.begin(), buff_channel.end());

        obj.set("value", value_string);
        Log::message("http", "Get successful.");
        Log::message("http", "Key: " + key_obj.toString());
        Log::message("http", "Value: " + std::string(buff_channel.begin(), buff_channel.end()));
    } else {
        Log::message("http", "Get failed:shard empty");

        resp.setStatusAndReason(HTTPServerResponse::HTTP_BAD_REQUEST,
                                "Get failed:shard empty");
    }

    resp.setChunkedTransferEncoding(true);
    resp.setContentType("text/html");
    resp.set("Access-Control-Allow-Origin", "*");
    resp.set("Access-Control-Allow-Headers", "*");
    resp.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

    std::ostream& ostr = resp.send();

    Poco::JSON::Stringifier::stringify(obj, ostr);
    ostr.flush();
}

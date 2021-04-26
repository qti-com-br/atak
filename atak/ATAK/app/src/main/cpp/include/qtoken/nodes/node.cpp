#include "node.hpp"

using namespace Qtoken;

Node::Node(const std::string& n_p, const std::string& n_rp,
           const std::string& add, bool is_lib, libconfig::Config& cfg)
    : Host(add)
    , svs(std::atoi(n_rp.c_str()))
    , acceptor(svs, reactor)
    , is_lib(is_lib)
    , cfg(cfg) {
    // If bootstrap port not given, use default
    if (boot_port.empty()) {
        boot_port = std::to_string(kademlia::session_base::DEFAULT_PORT);
    }

    /* Logging */
    log_message("VIN Node created on " + n_p + " with receipt server on port " +
                n_rp);
    log_message("Node bootstrap located at " + add);
    /* Logging */

    node_port = stoi(n_p);
    node_receipt_port = stoi(n_rp);
    const libconfig::Setting& settings = cfg.getRoot();
    const libconfig::Setting& keys = settings["file_system"]["keys"];

    // create keyfiles for node's priv and public keys
    std::string key_path, pub_key_path, priv_key_path;
    keys.lookupValue("keys_dir", key_path);
    keys.lookupValue("public_key_name", pub_key_path);
    keys.lookupValue("private_key_name", priv_key_path);

    // using C code here as std::filesystem breaks android cross-compile
    struct stat st = {0};
    if (stat(key_path.data(), &st) == -1) {
        mkdir(key_path.data(), 0700);
    }

    std::ofstream pub_key_out(pub_key_path, std::ios::out | std::ios::trunc);
    std::ofstream priv_key_out(priv_key_path, std::ios::out | std::ios::trunc);

    pub_key_out.close();
    priv_key_out.close();

    log_message("Created keyfiles");
}

/**
 * Fires up main loop. Also starts LVM socket, HTTP socket and receipt socket.
 */
int Node::run() {
    // setup kademlia endpoint
    kademlia::endpoint endpoint{boot_address, boot_port};

    node = std::make_shared<kademlia::session>(
        endpoint,
        kademlia::endpoint{
            "0.0.0.0",
            static_cast<kademlia::endpoint::service_numeric_type>(node_port)},
        kademlia::endpoint{
            "::",
            static_cast<kademlia::endpoint::service_numeric_type>(node_port)});

    log_message("Starting kademlia thread");

    // run kademlia endpoint
    std::future<std::error_code> main_loop_result =
        async(std::launch::async, &kademlia::session::run, node);

    log_message("kademlia thread started");

    // FIXME: Race condition with bootstrap connection
    // Kademlia needs to respond with a success code
    // otherwise we can only wait on failure and assume success otherwise
    std::chrono::seconds wait_ms(2);
    std::future_status kad_status = main_loop_result.wait_for(wait_ms);

    if (kad_status == std::future_status::timeout) {
        log_message("No kademlia errors reported");
    } else if (kad_status == std::future_status::deferred) {
        log_message("Kademlia startup deferred");
    } else if (kad_status == std::future_status::ready) {
        try {
            main_loop_result.get();
        } catch (const std::system_error& e) {
            log_message("Kademlia startup failed with error " +
                        std::string(e.what()));
            return EXIT_FAILURE;
        }
    }

    // run the receipt server on a different thread
    try {
        receipt_thread.start(reactor);
        std::cout << "Receipt server running on " << node_receipt_port
                  << std::endl;
    } catch (...) {
        std::cerr << "Couldn't start receipt server!" << std::endl;
        throw("");
    }

    // start main loop
    log_message("Running node at port " + std::to_string(node_port) +
                ". Connecting to " + endpoint.address());
    // FIXME: try{}catch a std::system error here for handling failed
    // connections

    log_message(std::string("Is lib: ") + (is_lib ? "true" : "false"));
    if (!is_lib) {
        printCommands();
        processInput();

        // close all servers
        node->abort();
        reactor.stop();

        std::cout << "main DHT loop exited!" << std::endl;
    } else
        waitForTerminationRequest();

    return EXIT_SUCCESS;
}

/**
 * Processes inputs to the VIN CLI.
 */
void Node::processInput() {
    std::string line;
    while (true) {
        std::cout << std::endl << "[port " << node_port << "]$ ";
        getline(*input, line);
        std::stringstream command(line);
        if (!handleCommands(command))
            break;
        (*input).clear();
    }
}

/**
 * Handles commands that are entered into the VIN CLI.
 */
int Node::handleCommands(std::stringstream& input) {
    std::string command, key, value, receipt_file_path, file_path;
    std::string peer_ip, peer_port;
    // use C as std::fs breaks android cross compile
    struct stat st = {0};
    input >> command;

    transform(command.begin(), command.end(), command.begin(), ::tolower);

    try {
        if (command == "") {
            std::cout << "\r";
            return 1;
        }

        log_message("Received command: " + command);

        if (command == "get") {
            key = readArg(input);
            std::vector<char> buff_channel = std::move(doGet(key));

            if (buff_channel.size() > 0) {
                int print_size = 50;
                if (buff_channel.size() < print_size) {
                    print_size = buff_channel.size();
                }

                for (int i = 0; i < print_size; i++) {
                    std::cout << buff_channel.at(i);
                }
                std::cout << std::endl;

                log_message("Get successful.");
                log_message("Key: " + key);  // debug
                log_message("Value: " +
                            std::string(buff_channel.begin(),
                                        buff_channel.end()));  // debug

            } else {
                std::cout << "nothing to print" << std::endl;

                log_message("Get failed:shard empty");
            }

            return 1;
        }

        if (command == "put") {
            key = readArg(input);
            value = readArg(input);
            if (key == "" || value == "") {
                std::cerr << "no k/v specified" << std::endl;
                log_message("Put failed:no input");
                return 1;
            }
            std::vector<char> v(value.begin(), value.end());
            doPut(key, v);

            log_message("Put successful.");
            log_message("Key: " + key);      // debug
            log_message("Value: " + value);  // debug

            return 1;
        }

        if (command == "share") {
            std::string receipt_dir;
            cfg.lookupValue("file_system.receipts.receipts_sent_dir",
                            receipt_dir);

            file_path = readArg(input);
            log_message("Share with input:" + file_path);
            peer_ip = readArg(input);
            log_message("Share with peer_ip:" + peer_ip);
            peer_port = readArg(input);
            log_message("Share with peer_port:" + peer_port);

            if (stat(file_path.data(), &st) == -1) {
                log_message("Cannot find file: " + file_path);
                return 1;
            }

            auto cr = doShare(file_path, peer_ip, peer_port);

            // Save receipt to disc with randomly generated name
            std::string save_path(receipt_dir + "CR" +
                                  std::to_string(get_rand_seed_uint32_t()));
            cr.save(save_path);

            return 1;
        }

        if (command == "spread") {
            file_path = readArg(input);

            if (stat(file_path.data(), &st) == -1) {
                std::cout << "Cannot find file: " << file_path << std::endl;
                log_message("Spread failed:bad file path");
                return 1;
            }

            CryptoReceipt cr = doSpread(file_path);

            // initialize receipt vars
            std::string receipt_dir;
            cfg.lookupValue("file_system.receipts.receipts_sent_dir",
                            receipt_dir);

            // Save receipt to disc with randomly generated name
            std::string save_path(receipt_dir + "CR" +
                                  std::to_string(get_rand_seed_uint32_t()));
            cr.save(save_path);
            log_message("Receipt saved to:" + save_path);
            std::cout << "Receipt path: " << save_path << std::endl;

            std::cout << "Spread successfull!" << std::endl;
            log_message("Spread successful.");

            return 1;
        }

        if (command == "gather") {
            receipt_file_path = readArg(input);

            if (stat(receipt_file_path.data(), &st) == -1) {
                std::cout << "Cannot find receipt: " << receipt_file_path
                          << std::endl;
                log_message("Gather failed:bad file path");
                return 1;
            }

            Writer w;
            std::string output_file;
            cfg.lookupValue("file_system.general.rebuilt_fd", output_file);
            std::cout << "Loading crypto receipt..." << std::endl;
            w.second = new CryptoReceipt(receipt_file_path);
            std::cout << "Fetching shards..." << std::endl;
            auto ch = doGather(*w.second);
            if (ch.get_file_size() == 0) {
                std::cout << "Bad receipt" << std::endl;
                log_message("Gather failed:file empty");
                delete w.second;
                return 1;
            }

            w.first = &ch;

            // std::cout << "Decoding shards..." << std::endl;
            // Pipeline<Decoder> p_decode;
            // p_decode.add(3, EntanglementDecoder(), ConcurrentDecoder(),
            //              PolarDecoder());
            // p_decode.run(w);

            std::cout << "Building file..." << std::endl;
            w.first->rebuild(output_file +
                             std::to_string(get_rand_seed_uint32_t()));

            log_message("Gather successful.");

            delete w.second;
            return 1;
        }

        if (command == "exit" || std::cin.eof()) {
            std::cout << "exiting...";
            return 0;
        }

        if (command != "help") {
            std::cout << "unknown command '" << command << "'" << std::endl;
        }
        printCommands();

    } catch (const std::exception& e) {
        std::cerr << e.what() << '\n';
    }
    return 1;
}

/**
 * Displays all available commands in VIN CLI.
 */
void Node::printCommands() {
    std::cout << "Commands:" << std::endl;
    std::cout << "\tget <key>                                   : try get "
                 "(asynchronously) the given key value"
              << std::endl;
    std::cout << "\tput <key> <value>                           : put a value "
                 "with the given key"
              << std::endl;
    std::cout << "\tspread <filepath>                           : save a file "
                 "to the network"
              << std::endl;
    std::cout
        << "\tgather <receipt-filepath>                   : gather a file "
           "using its receipt"
        << std::endl;
    std::cout << "\tshare <filepath> <peer-ip> <peer-port>      : share a file "
                 "with a peer"
              << std::endl;
    std::cout << "\thelp                                        : print this "
                 "instruction"
              << std::endl;
    std::cout << "Type \"exit\" to quit." << std::endl;
}

/**
 * Perform the get operation to retrieve a chunk given its key.
 * @param key The key that the chunk is indexed by.
 */
std::vector<char> Node::doGet(const std::string& key) {
    std::vector<char> buff_channel;

    P::Event e;

    node->async_load(
        asBinary(key),
        [key, &buff_channel, &e](std::error_code const& failure,
                                 kademlia::session::data_type const& data) {
            if (failure) {
                std::cerr << failure.message() << std::endl;
            } else {
                buff_channel = std::vector<char>(data.begin(), data.end());
            }
            e.set();
            log_message("Get succeeded");
        });
    e.wait();

    // wait until thread is done getting
    return buff_channel;
}

/**
 * Perform the put operation to store a chunk on the DHT.
 * @param key The key that the chunk is indexed by.
 * @param value The chunk to store. FIXME: make this a vector<char>
 */
void Node::doPut(const std::string& key, const std::vector<char>& value) {
    P::Event e;
    kademlia::session::data_type kad_key(key.begin(), key.end());
    kademlia::session::data_type kad_val(value.begin(), value.end());
    node->async_save(kad_key, kad_val,
                     [key, value, &e](std::error_code const& failure) {
                         if (failure) {
                             std::cerr << failure.message() << std::endl;
                         }
                         e.set();
                         log_message("put succeeded");
                     });
    log_message("try put");
    e.tryWait(5000);
}

/**
 * Spread a file onto the network by chunking and creating tokenized holograms
 * Holograms exist on the network and can be retrieved with the crypto receipt
 * @param file_path File to spread.
 */
CryptoReceipt Node::doSpread(const std::string& file_path) {
    std::string chk_sz_str = cfg.lookup("chunk_size");
    if (!is_number(chk_sz_str)) {
        std::cerr << "invalid chunk size, check config file" << std::endl;
        throw("");
    }
    int chk_sz = std::stoi(chk_sz_str);

    // FIXME: Determine constant or variable chunk size to be used
    // Create writer
    Writer w;
    std::ifstream fs(file_path, std::ios::in | std::ios::binary);
    Chunker ch = Chunker(fs, chk_sz);

    w.first = &ch;
    w.second = new CryptoReceipt();

    // Add original file size to receipt before we pad
    w.second->put("file_size", std::to_string(w.first->get_file_size()));

    // // Encoding pipeline
    // Pipeline<Encoder> p_encode;
    // p_encode.add(3, PolarEncoder(), ConcurrentEncoder(),
    // EntanglementEncoder()); p_encode.run(w);

    // Add shard keys to writer and upload to kademlia
    // iterate through chunks and upload to kademlia
    for (int i = 0; i < w.first->size(); i++) {
        // Hash the shard to get key
        std::vector<char> chk = w.first->get(i);
        std::string chk_str(chk.begin(), chk.end());
        std::string key = sha256(chk_str.substr(0, 32));

        // Store shard on kademlia network
        doPut(key, chk);

        // Record shard key in receipt
        w.second->put(std::to_string(i), key);
    }

    // FIXME: Writer can hold smart pointers so we can return *w.second
    CryptoReceipt cr(*w.second);

    delete w.second;
    return cr;
}

/**
 * Send a cryptographic receipt to a peer on the network.
 * @param receipt_path Path to receipt file.
 * @param peer_ip Ip of recipient peer.
 * @param peer_port Port of recipient peer receipt server.
 */
CryptoReceipt Node::doShare(const std::string& file_path,
                            const std::string& peer_ip,
                            const std::string& peer_port) {
    if (peer_ip == "" || peer_port == "") {
        std::cerr << "No IP/port specified!" << std::endl;
        throw("");
    }
    // spread file first
    auto cr = std::move(doSpread(file_path));

    log_message("Receipt: " + cr.serialize().str());

    // add receiver
    Addr receiver(peer_ip, peer_port);
    std::vector<Addr> addrs;
    addrs.push_back(receiver);

    // start receipt session
    ReceiptSession rs(cr);
    rs.send_receipt(addrs);
    return cr;
}

/**
 * Gathers the encrypted file shards specified by a receipt
 * If the operation fails a nullptr is returned instead
 * @param cr associated crypto receipt
 * @return gathered chunker allocated on pointer
 */
Chunker Node::doGather(CryptoReceipt cr) {
    int i = 0;
    int file_size;
    std::vector<std::vector<char>> chunks;
    std::vector<char> chunk;

    // Invalid receipt, send back nullptr
    if (cr.get("file_size") == "" || !is_number(cr.get("file_size"))) {
        // FIXME VIN-246
        Chunker ch(chunks, 0);
        return ch;
    }

    file_size = std::stoi(cr.get("file_size"));

    while (cr.get(std::to_string(i)) != "") {
        chunk = std::move(doGet(cr.get(std::to_string(i))));

        // Invalid receipt, send back nullptr
        if (chunk.empty()) {
            // FIXME VIN-246
            Chunker ch(chunks, 0);
            return ch;
        }

        chunks.push_back(chunk);
        i++;
    }

    // Truncate tail padding by setting file_size
    Chunker ch(chunks, file_size);
    return ch;
}

/**
 * Gathers the encrypted file shards specified by a receipt file
 * @param receipt_file_path Path to the receipt file
 * @return gathered chunker allocated on pointer
 */
Chunker Node::doGather(const std::string& receipt_file_path) {
    CryptoReceipt cr(receipt_file_path);
    return doGather(cr);
}

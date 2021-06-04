#include "../nodes/node.hpp"

using namespace Qtoken;

#ifdef __ANDROID__
Node::Node(const std::string& n_p, const std::string& n_rp,
           const std::string& n_sp, const std::string& add, bool is_lib,
           JNIEnv* env)
#else
Node::Node(const std::string& n_p, const std::string& n_rp,
           const std::string& n_sp, const std::string& add, bool is_lib)
#endif
    :
    svs(std::atoi(n_rp.c_str()))
    , acceptor(svs, reactor)
    , is_lib(is_lib) {
    // If bootstrap port not given, use default
    auto separatorIdx = add.find(':');
    boot_address = add.substr(0, separatorIdx);
    boot_port = add.substr(separatorIdx + 1);
    if (boot_port.empty()) {
        boot_port = std::to_string(kademlia::session_base::DEFAULT_PORT);
    }

    Log::message("root", "VIN Node created on " + n_p +
                             " with receipt server on port " + n_rp);
    Log::message("root", "Node bootstrap located at " + add);

    node_port = stoi(n_p);
    node_receipt_port = stoi(n_rp);
    node_server_port = stoi(n_sp);

    chunk_size = std::stoi(Config::get("chunk_size"));

#ifdef __ANDROID__
    env->GetJavaVM(&jvm);
    //replace with one of your classes in the line below
    auto randomClass = env->FindClass("com/virgilsystems/qtoken/QToken");
    jclass classClass = env->GetObjectClass(randomClass);
    auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
    auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
                                                 "()Ljava/lang/ClassLoader;");
    gClassLoader = env->CallObjectMethod(randomClass, getClassLoaderMethod);
    gFindClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                        "(Ljava/lang/String;)Ljava/lang/Class;");

    /******/


    // Find thread's context class loader.
    jclass javaLangThread = env->FindClass("java/lang/Thread");
    assert(javaLangThread != NULL);

    jclass javaLangClassLoader = env->FindClass("java/lang/ClassLoader");
    assert(javaLangClassLoader != NULL);

    jmethodID currentThread = env->GetStaticMethodID(
            javaLangThread, "currentThread", "()Ljava/lang/Thread;");
    assert(currentThread != NULL);

    jmethodID getContextClassLoader = env->GetMethodID(
            javaLangThread, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
    assert(getContextClassLoader != NULL);

    jobject thread = env->CallStaticObjectMethod(javaLangThread, currentThread);
    assert(thread != NULL);

    jobject classLoader = env->CallObjectMethod(thread, getContextClassLoader);

    if (classLoader != NULL) {
        cotClsLdr = env->NewWeakGlobalRef(classLoader);
    }

#endif
}

const kademlia::session_base::data_type asBinary(const std::string& value) {
    return kademlia::session::data_type{value.begin(), value.end()};
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

    // run kademlia endpoint
    std::future<std::error_code> main_loop_result =
        async(std::launch::async, &kademlia::session::run, node);

    Log::message("root", "kademlia thread started");

    // FIXME: Race condition with bootstrap connection
    // Kademlia needs to respond with a success code
    // otherwise we can only wait on failure and assume success otherwise
    std::chrono::seconds wait_ms(2);
    std::future_status kad_status = main_loop_result.wait_for(wait_ms);

    if (kad_status == std::future_status::timeout) {
        Log::message("root", "No kademlia errors reported");
    } else if (kad_status == std::future_status::deferred) {
        Log::message("root", "Kademlia startup deferred");
    } else if (kad_status == std::future_status::ready) {
        try {
            main_loop_result.get();
        } catch (const std::system_error& e) {
            Log::message("root", "Kademlia startup failed with error " +
                                     std::string(e.what()));
            return EXIT_FAILURE;
        }
    }

    Poco::Thread thread;
    std::vector<std::string> args;
    args.push_back(std::to_string(node_server_port));
    HTTPServerThread server_thread(args);
    thread.start(server_thread);
    Log::message("root", "HTTP server socket started on port: " +
                             std::to_string(node_server_port));

    // run the receipt server on a different thread
    try {
        receipt_thread.start(reactor);
        Log::message("root", "Receipt server running on " +
                                 std::to_string(node_receipt_port));
    } catch (...) {
        std::cerr << "Couldn't start receipt server!" << std::endl;
        throw("");
    }

    // start main loop
    Log::message("root", "Running node at port " + std::to_string(node_port) +
                             ". Connecting to " + endpoint.address());
    // FIXME: try{}catch a std::system error here for handling failed
    // connections
    if (!is_lib) {
        thread.join();

        // close all servers
        node->abort();
        reactor.stop();

        Log::message("root", "Main DHT loop exited. Shutting down node.");
    } else {
        server_wait_for_shutdown->wait();
    }

    return EXIT_SUCCESS;
}

/**
 * Perform the get operation to retrieve a chunk given its key.
 * @param key The key that the chunk is indexed by.
 */
Bytelist Node::doGet(const std::string& key) {
    Bytelist buff_channel;

    Poco::Event e;

    node->async_load(
        asBinary(key),
        [key, &buff_channel, &e](std::error_code const& failure,
                                 kademlia::session::data_type const& data) {
            if (failure) {
                std::cerr << failure.message() << std::endl;
            } else {
                buff_channel = Bytelist(data.begin(), data.end());
            }
            e.set();
        });
    e.wait();

    // wait until thread is done getting
    return buff_channel;
}

std::vector<kademlia::kad_peer> Node::getPeers() {
    return node->list_known_peers();
}

/**
 * Perform the put operation to store a chunk on the DHT.
 * @param key The key that the chunk is indexed by.
 * @param value The chunk to store.
 */
void Node::doPut(const std::string& key, const Bytelist& value) {
    Poco::Event e;
    kademlia::session::data_type kad_key(key.begin(), key.end());
    kademlia::session::data_type kad_val(value.begin(), value.end());
    node->async_save(kad_key, kad_val,
                     [key, value, &e](std::error_code const& failure) {
                         if (failure) {
                             std::cerr << failure.message() << std::endl;
                         }
                         e.set();
                     });
    e.wait();
}

/**
 * Spread all writer chunks over the network.
 * Record information needed for retrieval and rebuilding in writer receipt.
 * @param w writer containing chunker and crypto receipt.
 */
void Node::doSpread(Writer& w) {
    // Add original file size to receipt before we pad
    w.second->put("file_size", std::to_string(w.first->get_file_size()));
    w.second->put("chunk_size", std::to_string(w.first->get_chunk_size()));

    // Add shard keys to writer and upload to kademlia
    // iterate through chunks and upload to kademlia
    for (ull i = 0; i < w.first->size(); i++) {
        // Hash the shard to get key
        Bytelist chk = w.first->get(i);
        std::string chk_str(chk.begin(), chk.end());
        std::string key = sha256(chk_str.substr(0, 32)).substr(15);

        // Store shard on kademlia network
        doPut(key, chk);

        // Record shard key in receipt
        w.second->put(std::to_string(i), key);
    }
}

/**
 * Read the istream into a writer's chunker and allocate a crypto receipt.
 * Send writer to doSpread.
 * @param file_istream file to turn into writer and spread.
 */
CryptoReceipt Node::doSpread(std::istream& file_istream) {
    Writer w;
    w.first = new Chunker(file_istream, chunk_size);
    w.second = new CryptoReceipt();

    doSpread(w);

    CryptoReceipt cr(*w.second);

    delete w.first;
    delete w.second;
    return cr;
}

/**
 * Read file into istream and send to doSpread.
 * @param file_path file to read to istream and spread.
 */
CryptoReceipt Node::doSpread(const std::string& file_path) {
    std::ifstream fs(file_path, std::ios::in | std::ios::binary);
    return doSpread(fs);
}

/**
 * Spread a writer and send the crypto receipt to a peer on the network.
 * @param w Writer to share across secure channel.
 * @param peer_ip Ip of recipient peer.
 * @param peer_port Port of recipient peer receipt server.
 */
std::vector<CryptoReceipt> Node::doShare(Writer w, const std::string& peer_ip,
                                         const std::string& peer_port) {
    return doShare(w, peer_ip, peer_port, false);
}

std::vector<CryptoReceipt> Node::doShare(Bytelist& data,
                                         const std::string& peer_ip,
                                         const std::string& peer_port) {

    std::string s(data.begin(), data.end());
    Writer wr;


    Chunker chs(data, chunk_size);
    wr.first = &chs;
    wr.second = new CryptoReceipt();

    auto receipts = doShare(wr, peer_ip, peer_port);

    delete wr.first;
    delete wr.second;

    return receipts;
}

std::vector<CryptoReceipt> Node::doShare(Writer w, const std::string& peer_ip,
                                         const std::string& peer_port,
                                         bool continue_stream) {
    if (peer_ip == "" || peer_port == "") {
        std::cerr << "No IP/port specified!" << std::endl;
        throw("");
    }

    if (!active_stream_client) {
        active_stream_client.reset(
            new ReceiptSession(Addr(peer_ip, peer_port)));
        active_stream_client->start_session();
    }

    std::vector<CryptoReceipt> crs;
    std::vector<Writer> ws;

    // Split chunker into vector of chunkers
    std::vector<Chunker> chs = w.first->split(200);

    for (int i = 0; i < chs.size(); i++) {
        Writer wr;
        wr.first = &chs.at(i);
        wr.second = new CryptoReceipt();
        ws.push_back(wr);
    }

    for (auto wr : ws) {
        doSpread(wr);
        active_stream_client->send_receipt(*wr.second);
        crs.push_back(std::move(*wr.second));
        // delete wr.first;
        delete wr.second;
    }

    if (!continue_stream)
        active_stream_client.reset();

    return crs;
}

/**
 * Turn a istream into a writer and share the writer.
 * @param file_istream istream to share across secure channel.
 * @param peer_ip Ip of recipient peer.
 * @param peer_port Port of recipient peer receipt server.
 */
std::vector<CryptoReceipt> Node::doShare(std::istream& file_istream,
                                         const std::string& peer_ip,
                                         const std::string& peer_port) {
    Writer w;
    w.first = new Chunker(file_istream, chunk_size);
    w.second = new CryptoReceipt();
    return doShare(w, peer_ip, peer_port);
}

/**
 * Turn a istream into a writer and share the writer.
 * @param file_istream istream to share across secure channel.
 * @param peer_ip Ip of recipient peer.
 * @param peer_port Port of recipient peer receipt server.
 */
std::vector<CryptoReceipt> Node::doShare(std::istream& file_istream,
                                         const std::string& peer_ip,
                                         const std::string& peer_port,
                                         bool continue_stream) {
    Writer w;
    w.first = new Chunker(file_istream, chunk_size);
    w.second = new CryptoReceipt();
    return doShare(w, peer_ip, peer_port, continue_stream);
}

/**
 * Read a file into an istream and share the istream.
 * @param file_path istream to share across secure channel.
 * @param peer_ip Ip of recipient peer.
 * @param peer_port Port of recipient peer receipt server.
 */
std::vector<CryptoReceipt> Node::doShare(const std::string& file_path,
                                         const std::string& peer_ip,
                                         const std::string& peer_port) {
    std::ifstream fs(file_path, std::ios::in | std::ios::binary);
    return doShare(fs, peer_ip, peer_port);
}

/**
 * Gathers the encrypted file shards specified by a receipt.
 * @param cr associated crypto receipt.
 * @return chunker resulting from gathered shards.
 */
Chunker Node::doGather(const CryptoReceipt& cr) {
    ull i = 0;
    std::vector<Bytelist> chunks;
    Bytelist chunk;

    ull file_size = std::stoull(cr.get("file_size"));

    while (cr.get(std::to_string(i)) != "") {
        chunk = std::move(doGet(cr.get(std::to_string(i))));

        // Retry get
        if (chunk.empty()) {
            chunk = std::move(doGet(cr.get(std::to_string(i))));
        }

        if (!chunk.empty()) {
            chunks.push_back(chunk);
        } else {
            Log::message("root", "Failed to find shard " + std::to_string(i));
        }

        i++;
    }

    // If chunks were missed
    if (i != chunks.size()) {
        Chunker ch(chunks, 0);
        return ch;
    }

    Chunker ch(chunks, file_size);
    return ch;
}

/**
 * Loads a receipt file and gathers the crypto receipt.
 * @param receipt_file_path path to receipt.
 * @return chunker resulting from gathered shards.
 */
Chunker Node::doGather(const std::string& receipt_file_path) {
    CryptoReceipt cr(receipt_file_path);
    return doGather(cr);
}

/**
 * Starts a new streaming session identified by session token.
 * @param session_token Identifier for streaming session.
 */
void Node::add_stream_session(std::string session_token) {
    this->active_stream = session_token;
    active_rs.reset(new ReceiptService());
}

/**
 * Update an existing stream session with a CryptoReceipt.
 * @param cr CryptoReceipt to add to stream
 */
void Node::update_stream_session(CryptoReceipt cr) {
    active_rs->push(cr);
}

/**
 * Ends an existing streaming session identified by session token. Do
 * nothing if token does not exist.
 * @param session_token Identifier for streaming session to end.
 */
#ifdef __ANDROID__
void Node::end_stream_session(std::string ip_addr) {
    Log::message("root: COT recvd!!", "1");

    JNIEnv * g_env;
    // double check it's all ok
    int getEnvStat = jvm->GetEnv((void **)&g_env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        Log::message("jvm", "GetEnv: not attached");
        if (jvm->AttachCurrentThread(&g_env, NULL) != 0) {
            Log::message("jvm", "GetEnv: failed");
        }
    } else if (getEnvStat == JNI_OK) {
        //
    } else if (getEnvStat == JNI_EVERSION) {
        Log::message("jvm", "GetEnv: bad version");
    }

    Log::message("root: COT recvd!!", "3");

    Chunker ch = active_rs->build();

    // export data to JNI
    auto ch_vec = ch.join();
    std::string cot(ch_vec.begin(), ch_vec.end());

    Log::message("root: COT recvd!!", cot);
    // Getting CommsMapComponent.cotMessageReceived

//    jclass cotCls = static_cast<jclass>(g_env->CallObjectMethod(gClassLoader, gFindClassMethod,
//            g_env->NewStringUTF("com/virgilsystems/qtoken/QToken")));


    jclass javaLangClassLoader = g_env->FindClass("java/lang/ClassLoader");
    assert(javaLangClassLoader != NULL);
    jmethodID loadClass =
            g_env->GetMethodID(javaLangClassLoader,
                             "loadClass",
                             "(Ljava/lang/String;)Ljava/lang/Class;");
    assert(loadClass != NULL);

    // Create an object for the class name string; alloc could fail.
    jstring strClassName = g_env->NewStringUTF("com/virgilsystems/qtoken/QToken");

    // Try to find the named class.
    jclass cotCls = (jclass) g_env->CallObjectMethod(cotClsLdr,
                                                loadClass,
                                                strClassName);

    if (g_env->ExceptionCheck()) {
        g_env->ExceptionDescribe();
        Log::message("ERROR: ", "unable to load class from com/virgilsystems/qtoken/QToken");
        return;
    }

    jmethodID cotMethod =
            g_env->GetStaticMethodID(cotCls, "shareHandler", "(Ljava/lang/String;)V");
    jstring jcot = g_env->NewStringUTF(cot.c_str());

    // Call CommsMapComponent.cotMessageReceived with the Bundle received
    // through VIN bridge
    g_env->CallStaticVoidMethod(cotCls, cotMethod, jcot);

#else
void Node::end_stream_session() {
    Chunker ch = active_rs->build();

    std::string output_file_path =
        Config::get("files.rebuilt") + std::to_string(get_rand_seed_uint32_t());

    Log::message("root", "File rebuilt at " + output_file_path);

    ch.rebuild(output_file_path);
#endif
    active_rs.reset();
    active_stream = "";
}

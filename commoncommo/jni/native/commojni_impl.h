#ifndef COMMOJNI_IMPL_H
#define COMMOJNI_IMPL_H

#include "commojni_common.h"
#include "commo.h"

#include <Mutex.h>

#include <set>
#include <map>

#define COMMO_THROW(...)

#define PASTE(a, b) a ## b

#define LOOKUP_CLASS(clazz_obj, clazz_name, local)                          \
    clazz_obj = env->FindClass(clazz_name);                                 \
    if (!clazz_obj)                                                         \
        goto cleanup;                                                       \
    if (!(local)) {                                                         \
        clazz_obj = (jclass)env->NewGlobalRef(clazz_obj);                   \
        if (!clazz_obj)                                                     \
            goto cleanup;                                                   \
    }                                                                       \

#define LOOKUP_METHOD(methodid_obj, clazz_obj, method_name, method_sig)     \
    methodid_obj = env->GetMethodID(clazz_obj, method_name, method_sig);    \
    if (!methodid_obj)                                                      \
        goto cleanup;                                                       \

#define LOOKUP_STATIC_METHOD(methodid_obj, clazz_obj, method_name, method_sig)\
    methodid_obj = env->GetStaticMethodID(clazz_obj, method_name, method_sig);\
    if (!methodid_obj)                                                        \
        goto cleanup;                                                         \
    
#define LOOKUP_FIELD(fieldid_obj, clazz_obj, field_name, field_sig)           \
    fieldid_obj = env->GetFieldID(clazz_obj, field_name, field_sig);          \
    if (!fieldid_obj)                                                         \
        goto cleanup;                                                         \




using namespace atakmap::commoncommo;

namespace atakmap {
namespace jni {
namespace commoncommo {

    bool checkEnum(JNIEnv *env, jmethodID method_enumName,
                   jmethodID method_enumVal,
                   jobjectArray jenumValues,
                   jsize numLevels, const char *enumName,
                   jglobalobjectref *nativeToJObjMap,
                   int enumIdx, int enumVal);

    class JNIObjWrapper
    {
    public:
        virtual jglobalobjectref getWrappedRef() const = 0;
    
    protected:
        JNIObjWrapper() {};
        virtual ~JNIObjWrapper() {};
    };


    typedef std::map<NetInterface *, jglobalobjectref> NetInterfaceMap;



    class CommoLoggerJNI : public CommoLogger
    {
    public:
        CommoLoggerJNI(JNIEnv *env, jobject jlogger) COMMO_THROW (int);
        static void destroy(JNIEnv *env, CommoLoggerJNI *logger);

        virtual void log(Level level, const char *message);
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);
        
    private:
        ~CommoLoggerJNI();

        jglobalobjectref jlogger;
        
        static jmethodID jmethod_log;
        static const int NUM_LOGGER_LEVELS = 5;
        static jglobalobjectref nativeLevelsToJava[NUM_LOGGER_LEVELS];
    };



    class MissionPackageIOJNI : public MissionPackageIO
    {
    public:
        MissionPackageIOJNI(JNIEnv *env, jobject jmpio) COMMO_THROW (int);
        static void destroy(JNIEnv *env, MissionPackageIOJNI *mpio);

        virtual MissionPackageTransferStatus missionPackageReceiveInit(
                    char *destFile, size_t destFileSize,
                    const char *transferName, const char *sha256hash,
                    uint64_t sizeInBytes,
                    const char *senderCallsign);
        virtual void missionPackageReceiveStatusUpdate(
                    const MissionPackageReceiveStatusUpdate *update);
        virtual void missionPackageSendStatusUpdate(
                    const MissionPackageSendStatusUpdate *update);
        virtual CoTPointData getCurrentPoint();
        virtual void createUUID(char *uuidString);

        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~MissionPackageIOJNI();
        jglobalobjectref jmpio;

        static jclass jclass_file;
        static jmethodID jmethod_fileCtor;
        static jmethodID jmethod_fileGetAbsPath;

        static jclass jclass_mptransferex;
        static jfieldID jfield_mptransferexStatus;

        static jmethodID jmethod_mptransferstatGetNative;

        static jmethodID jmethod_mpReceiveInit;
        static jmethodID jmethod_mpReceiveStatus;
        static jmethodID jmethod_mpSendStatus;
        static jmethodID jmethod_getCurrentPoint;
        static jmethodID jmethod_createUUID;
        
        static jclass jclass_mpReceiveUpdate;
        static jmethodID jmethod_mpReceiveUpdateCtor;

        static jclass jclass_mpSendUpdate;
        static jmethodID jmethod_mpSendUpdateCtor;

        static jfieldID jfield_cotPointLat;
        static jfieldID jfield_cotPointLon;
        static jfieldID jfield_cotPointHae;
        static jfieldID jfield_cotPointCe;
        static jfieldID jfield_cotPointLe;

        static const int NUM_MP_STATUS = 12;
        static jglobalobjectref nativeMPStatusToJava[NUM_MP_STATUS];

    };

    class SimpleFileIOJNI : public SimpleFileIO
    {
    public:
        SimpleFileIOJNI(JNIEnv *env, jobject jfileio) COMMO_THROW (int);
        static void destroy(JNIEnv *env, SimpleFileIOJNI *fileio);

        virtual void fileTransferUpdate(const SimpleFileIOUpdate *update);

        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);
        
        static jglobalobjectref getJavaFileStatus(SimpleFileIOStatus stat);

    private:
        ~SimpleFileIOJNI();
        jglobalobjectref jfileio;

        static jmethodID jmethod_fileTransferUpdate;
        
        static jclass jclass_fileIOUpdate;
        static jmethodID jmethod_fileIOUpdateCtor;

        static const int NUM_FILE_STATUS = 15;
        static jglobalobjectref nativeFileStatusToJava[NUM_FILE_STATUS];

    };
    
    class CloudIOJNI : public CloudIO
    {
    public:
        CloudIOJNI(JNIEnv *env, jobject jfileio) COMMO_THROW (int);
        static void destroy(JNIEnv *env, CloudIOJNI *fileio);

        virtual void cloudOperationUpdate(const CloudIOUpdate *update);

        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~CloudIOJNI();
        jglobalobjectref jcloudio;

        static jmethodID jmethod_cloudOperationUpdate;
        
        static jclass jclass_cloudIOUpdate;
        static jmethodID jmethod_cloudIOUpdateCtor;
        static jclass jclass_cloudEntry;
        static jmethodID jmethod_cloudEntryCtor;

        static const int NUM_ENT_TYPES = 2;
        static jglobalobjectref nativeEntryTypesToJava[NUM_ENT_TYPES];
        static const int NUM_CLOUD_OPS = 6;
        static jglobalobjectref nativeCloudOpsToJava[NUM_CLOUD_OPS];
    };

    class InterfaceStatusListenerJNI : public JNIObjWrapper,
                                       public InterfaceStatusListener
    {
    public:
        InterfaceStatusListenerJNI(JNIEnv *env, jobject jifaceListener,
                                   PGSC::Thread::Mutex *ifaceMapMutex,
                                   NetInterfaceMap *ifaceMap) COMMO_THROW (int);
        static void destroy(JNIEnv *env, InterfaceStatusListenerJNI *iface);
        
        virtual void interfaceUp(NetInterface *iface);
        virtual void interfaceDown(NetInterface *iface);
        virtual void interfaceError(NetInterface *iface,
                            netinterfaceenums::NetInterfaceErrorCode errCode);
        virtual jglobalobjectref getWrappedRef() const;
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~InterfaceStatusListenerJNI();

        void interfaceChange(NetInterface *iface, jmethodID jmethodUpDown);

        jglobalobjectref jifaceListener;
        PGSC::Thread::Mutex *ifaceMapMutex;
        NetInterfaceMap *ifaceMap;

        static jmethodID jmethod_ifaceUp;
        static jmethodID jmethod_ifaceDown;
        static jmethodID jmethod_ifaceError;

        static const int NUM_IFACE_ERRCODES = 12;
        static jglobalobjectref nativeErrCodesToJava[NUM_IFACE_ERRCODES];
    };


    class ContactListenerJNI : public JNIObjWrapper,
                               public ContactPresenceListener
    {
    public:
        ContactListenerJNI(JNIEnv *env, jobject jcontactListener) COMMO_THROW (int);
        static void destroy(JNIEnv *env, ContactListenerJNI *listener);
        
        virtual void contactAdded(const ContactUID *contact);
        virtual void contactRemoved(const ContactUID *contact);
        virtual jglobalobjectref getWrappedRef() const;
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~ContactListenerJNI();

        void contactChanged(const ContactUID *contact,
                            jmethodID jmethodAddRem);

        jglobalobjectref jcontactListener;

        static jclass jclass_contact;
        static jmethodID jmethod_contactCtor;
        static jmethodID jmethod_contactAdded;
        static jmethodID jmethod_contactRemoved;
    };


    class CoTListenerJNI : public JNIObjWrapper,
                           public CoTMessageListener
    {
    public:
        CoTListenerJNI(JNIEnv *env, jobject jcotListener) COMMO_THROW (int);
        static void destroy(JNIEnv *env, CoTListenerJNI *listener);
        
        virtual void cotMessageReceived(const char *cotMessage, const char *rxEndpointId);
        virtual jglobalobjectref getWrappedRef() const;
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~CoTListenerJNI();

        jglobalobjectref jcotListener;

        static jmethodID jmethod_cotReceived;
    };


    class GenericDataListenerJNI : public JNIObjWrapper,
                           public GenericDataListener
    {
    public:
        GenericDataListenerJNI(JNIEnv *env, jobject jgenericListener) COMMO_THROW (int);
        static void destroy(JNIEnv *env, GenericDataListenerJNI *listener);
        
        virtual void genericDataReceived(const uint8_t *data, size_t len,
                                        const char *rxEndpointId);
        virtual jglobalobjectref getWrappedRef() const;
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~GenericDataListenerJNI();

        jglobalobjectref jgenericListener;

        static jmethodID jmethod_genericDataReceived;
    };


    class CoTFailListenerJNI : public JNIObjWrapper,
                               public CoTSendFailureListener
    {
    public:
        CoTFailListenerJNI(JNIEnv *env, jobject jcotFailListener) COMMO_THROW (int);
        static void destroy(JNIEnv *env, CoTFailListenerJNI *listener);
        
        virtual void sendCoTFailure(const char *host, int port,
                                    const char *errorReason);
        virtual jglobalobjectref getWrappedRef() const;
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);

    private:
        ~CoTFailListenerJNI();

        jglobalobjectref jcotFailListener;

        static jmethodID jmethod_sendCoTFailure;
    };


    class CommoJNI
    {
    public:
        CommoJNI(JNIEnv *env, jobject jlogger,
                 jstring jourUID, jstring jourCallsign) COMMO_THROW (int);
        static void destroy(JNIEnv *env, CommoJNI *commoJNI);

        void setupMPIO(JNIEnv *env, jobject jmpio) COMMO_THROW (int);
        void enableFileIO(JNIEnv *env, jobject jfileio) COMMO_THROW (int);
        
        bool addIfaceStatusListener(JNIEnv *env, jobject jifaceListener);
        bool removeIfaceStatusListener(JNIEnv *env, jobject jifaceListener);

        bool addContactListener(JNIEnv *env, jobject jcontactListener);
        bool removeContactListener(JNIEnv *env, jobject jcontactListener);

        bool addCoTListener(JNIEnv *env, jobject jcotListener);
        bool removeCoTListener(JNIEnv *env, jobject jcotListener);

        bool addGenericListener(JNIEnv *env, jobject jgenericListener);
        bool removeGenericListener(JNIEnv *env, jobject jgenericListener);

        bool addCoTSendFailureListener(JNIEnv *env, jobject jcotFailListener);
        bool removeCoTSendFailureListener(JNIEnv *env,
                                          jobject jcotFailListener);

        jobject wrapPhysicalNetInterface(JNIEnv *env,
                                         PhysicalNetInterface *iface,
                                         jbyteArray jhwAddr);
        jobject wrapTcpNetInterface(JNIEnv *env,
                                    TcpInboundNetInterface *iface,
                                    jint port);
        jobject wrapStreamingNetInterface(JNIEnv *env, 
                                          StreamingNetInterface *iface);
        void unwrapNetInterface(JNIEnv *env, NetInterface *iface);

        jobject wrapCloudClient(JNIEnv *env, 
                                CloudClient *client, CloudIOJNI *cloudiojni);
        void unwrapCloudClient(JNIEnv *env, CloudClient *client);
        
        static bool reflectionInit(JNIEnv *env);
        static void reflectionRelease(JNIEnv *env);


        Commo *commo;
    private:
        ~CommoJNI();
        
        MissionPackageIOJNI *mpio;
        SimpleFileIOJNI *fileio;
public:
        CommoLoggerJNI *logger;

        PGSC::Thread::Mutex ifaceListenersMutex;
        std::set<InterfaceStatusListenerJNI *> ifaceListeners;

        PGSC::Thread::Mutex contactListenersMutex;
        std::set<ContactListenerJNI *> contactListeners;

        PGSC::Thread::Mutex cotListenersMutex;
        std::set<CoTListenerJNI *> cotListeners;

        PGSC::Thread::Mutex genericListenersMutex;
        std::set<GenericDataListenerJNI *> genericListeners;

        PGSC::Thread::Mutex cotFailListenersMutex;
        std::set<CoTFailListenerJNI *> cotFailListeners;

        PGSC::Thread::Mutex netInterfaceMapMutex;
        NetInterfaceMap netInterfaceMap;
        
        typedef std::map<CloudClient *, std::pair<CloudIOJNI *, jglobalobjectref> > CloudclientMap;
        PGSC::Thread::Mutex cloudclientMapMutex;
        CloudclientMap cloudclientMap;
        
        static jclass jclass_physnetiface;
        static jmethodID jmethod_physnetifaceCtor;
        static jclass jclass_tcpnetiface;
        static jmethodID jmethod_tcpnetifaceCtor;
        static jclass jclass_streamnetiface;
        static jmethodID jmethod_streamnetifaceCtor;
        static jclass jclass_cloudclient;
        static jmethodID jmethod_cloudclientCtor;

    public:
        static jclass jclass_commoexception;
        static jclass jclass_mpnativeresult;
        static jmethodID jmethod_mpnativeresultCtor;
    };

}
}
}

#endif

#include "common.h"

#include <cstring>
#include <map>

#include <sqlite3.h>
#include <spatialite.h>

#include <android/log.h>

#include "thread/Lock.h"
#include "thread/Mutex.h"
#include "util/Logging2.h"
#include "util/Memory.h"

#include "interop/Pointer.h"

using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace {

class LogcatLogger : public Logger2
{
public :
    /** returns 0 on success, non-zero on error */
    virtual int print(const LogLevel lvl, const char *fmt, va_list arg) NOTHROWS;
};

#define PGSC_ENV_SET_METHOD_FN_DECL(type) \
    void envSetReturns ## type (JNIEnv *env, jobject object, jmethodID methodID, ...);

PGSC_ENV_SET_METHOD_FN_DECL(Void);
PGSC_ENV_SET_METHOD_FN_DECL(Byte);
PGSC_ENV_SET_METHOD_FN_DECL(Boolean);
PGSC_ENV_SET_METHOD_FN_DECL(Char);
PGSC_ENV_SET_METHOD_FN_DECL(Short);
PGSC_ENV_SET_METHOD_FN_DECL(Int);
PGSC_ENV_SET_METHOD_FN_DECL(Long);
PGSC_ENV_SET_METHOD_FN_DECL(Float);
PGSC_ENV_SET_METHOD_FN_DECL(Double);
PGSC_ENV_SET_METHOD_FN_DECL(Object);

#undef PGSC_ENV_SET_METHOD_FN_DECL

int indexOf(const char *str, const char c, const int from);

std::map<std::string, jclass> &classRefMap();
Logger2 &getAndroidLogger();
Mutex &mutex();

void sqliteLogCallback(void *pArg, int iErrCode, const char *zMsg);

JavaVM *vm;

} // unnamed namespace

JavaIntFieldAccess *nioBuffer_position;
JavaIntFieldAccess *nioBuffer_limit;
jclass nioBufferClass;

jmethodID glLineBatch_flush;
jclass glLineBatchClass;

jclass pointDClass;
jmethodID pointD_ctor__DDD;
jfieldID pointD_x;
jfieldID pointD_y;
jfieldID pointD_z;

jclass ProgressCallback_class;
jmethodID ProgressCallback_progress;
jmethodID ProgressCallback_error;

jclass java_lang_RuntimeException_class;
jclass java_lang_IllegalArgumentException_class;
jclass java_lang_IndexOutOfBoundsException_class;
jclass java_lang_InterruptedException_class;
jclass java_lang_UnsupportedOperationException_class;
jclass java_io_IOException_class;
jclass java_lang_OutOfMemoryError_class;
jclass java_lang_IllegalStateException_class;
jclass java_util_ConcurrentModificationException_class;
jclass java_io_EOFException_class;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm_, void *reserved)
{
    vm = vm_;

    void *env_vp;
    JNIEnv *env;
    if (vm->GetEnv(&env_vp, JNI_VERSION_1_6))
        return JNI_ERR;

    env = (JNIEnv *)env_vp;

    nioBufferClass = ATAKMapEngineJNI_findClass(env, "java/nio/Buffer");
    if (!nioBufferClass)
        return JNI_ERR;

    nioBuffer_position = new JavaIntFieldAccess(env,
                                                nioBufferClass,
                                                "position",
                                                "position",
                                                "()I",
                                                "position",
                                                "(I)Ljava/nio/Buffer;");

    if (!nioBuffer_position->isValid(true, true))
        return JNI_ERR;

    nioBuffer_limit = new JavaIntFieldAccess(env,
                                             nioBufferClass,
                                             "limit",
                                             "limit",
                                             "()I",
                                             "limit",
                                             "(I)Ljava/nio/Buffer;");

    if (!nioBuffer_limit->isValid(true, true))
        return JNI_ERR;

    glLineBatchClass = ATAKMapEngineJNI_findClass(env, ATAKMAP_GL_PKG "/GLLineBatch");
    if (!glLineBatchClass)
        return JNI_ERR;

    glLineBatch_flush = env->GetMethodID(glLineBatchClass, "flush", "()V");
    if (!glLineBatch_flush)
        return JNI_ERR;

    pointDClass = ATAKMapEngineJNI_findClass(env, "com/atakmap/math/PointD");
    if(!pointDClass)
        return JNI_ERR;
    pointD_ctor__DDD = env->GetMethodID(pointDClass, "<init>", "(DDD)V");
    if(!pointD_ctor__DDD)
        return JNI_ERR;
    pointD_x = env->GetFieldID(pointDClass, "x", "D");
    if(!pointD_x)
        return JNI_ERR;
    pointD_y = env->GetFieldID(pointDClass, "y", "D");
    if(!pointD_y)
        return JNI_ERR;
    pointD_z = env->GetFieldID(pointDClass, "z", "D");
    if(!pointD_z)
        return JNI_ERR;

    Pointer_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/interop/Pointer");
    if(!Pointer_class.id)
        return JNI_ERR;
    Pointer_class.ctor__JJI = env->GetMethodID(Pointer_class.id, "<init>", "(JJI)V");
    if(!Pointer_class.ctor__JJI)
        return JNI_ERR;
    Pointer_class.value = env->GetFieldID(Pointer_class.id, "value", "J");
    if(!Pointer_class.value)
        return JNI_ERR;
    Pointer_class.raw = env->GetFieldID(Pointer_class.id, "raw", "J");
    if(!Pointer_class.raw)
        return JNI_ERR;
    Pointer_class.type = env->GetFieldID(Pointer_class.id, "type", "I");
    if(!Pointer_class.type)
        return JNI_ERR;

    ProgressCallback_class = ATAKMapEngineJNI_findClass(env, "com/atakmap/interop/ProgressCallback");
    if(!ProgressCallback_class)
        return JNI_ERR;
    ProgressCallback_progress = env->GetMethodID(ProgressCallback_class, "progress", "(I)V");
    if(!ProgressCallback_progress)
        return JNI_ERR;
    ProgressCallback_error = env->GetMethodID(ProgressCallback_class, "error", "(Ljava/lang/String;)V");
    if(!ProgressCallback_error)
        return JNI_ERR;

#define LOAD_EXCEPTION_CLASS(cn, p) \
    java_##p##_##cn##_class = (jclass)ATAKMapEngineJNI_findClass(env, "java/"#p"/"#cn); \
    if(!java_##p##_##cn##_class) \
        return JNI_ERR;

    LOAD_EXCEPTION_CLASS(RuntimeException, lang);
    LOAD_EXCEPTION_CLASS(IllegalArgumentException, lang);
    LOAD_EXCEPTION_CLASS(IndexOutOfBoundsException, lang);
    LOAD_EXCEPTION_CLASS(InterruptedException, lang);
    LOAD_EXCEPTION_CLASS(UnsupportedOperationException, lang);
    LOAD_EXCEPTION_CLASS(IOException, io);
    LOAD_EXCEPTION_CLASS(OutOfMemoryError, lang);
    LOAD_EXCEPTION_CLASS(IllegalStateException, lang);
    LOAD_EXCEPTION_CLASS(ConcurrentModificationException, util);
    LOAD_EXCEPTION_CLASS(EOFException, io);
#undef LOAD_EXCEPTION_CLASS

    LoggerPtr androidLogger(&getAndroidLogger(), Memory_leaker_const<Logger2>);
    Logger_setLogger(std::move(androidLogger));

    sqlite3_config(SQLITE_CONFIG_LOG, sqliteLogCallback, NULL);
    spatialite_init(1);

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    void *env_vp;
    JNIEnv *env;
    if (vm->GetEnv(&env_vp, JNI_VERSION_1_6))
        return; // really shouldn't happen, if we got through JNI_OnLoad
    env = (JNIEnv *)env_vp;

    LockPtr lock(NULL, NULL);
    Lock_create(lock, mutex());

    std::map<std::string, jclass>::iterator entry;
    for(entry = classRefMap().begin(); entry != classRefMap().end(); entry++)
        env->DeleteGlobalRef(entry->second);
    classRefMap().clear();

    vm = NULL;
}


/******************************************************************************/

JavaIntFieldAccess::JavaIntFieldAccess(JNIEnv *env,
                       jclass clazz,
                       const char *fieldName,
                       const char *getMethodName,
                       const char *getMethodSig,
                       const char *setMethodName,
                       const char *setMethodSig) :

        fieldId(0),
        setMethodId(0),
        getMethodId(0),
        getImpl(0),
        setImpl(0),
        envSetMethodImpl(0)
{

    if(fieldName) {
        fieldId = env->GetFieldID(clazz, fieldName, "I");
        if(fieldId) {
            getImpl = &getDirect;

            // XXX - need to check final ???
            setImpl = &setDirect;
        } else {
            // fields are probably part of the private or protected API and may
            // be subject to change between versions -- try to handle this
            // gracefully by allow method lookup to occur
            if(env->ExceptionCheck())
                env->ExceptionClear();

            jclass langClassClass = env->FindClass("java/lang/Class");
            jmethodID langClass_getName = env->GetMethodID(langClassClass, "getName", "()Ljava/lang/String;");
            jstring jclassName = (jstring)env->CallObjectMethod(clazz, langClass_getName);
            const char *className = env->GetStringUTFChars(jclassName, 0);
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Field lookup failed. Class: %s Field: %s\n", className, fieldName);
            env->ReleaseStringUTFChars(jclassName, className);
        }
    }

    if(!getImpl && getMethodName && getMethodSig) {
        getMethodId = env->GetMethodID(clazz, getMethodName, getMethodSig);
        if(getMethodId) {
            getImpl = &getMethod;
        } else {
            // log the method lookup failure -- don't clear the exception as
            // there is no appropriate error handling at this point
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Method lookup failed. Method: %s%s\n", getMethodName, getMethodSig);
        }
    }

    if(!setImpl && setMethodName && setMethodSig) {
        setMethodId = env->GetMethodID(clazz, setMethodName, setMethodSig);
        if(setMethodId) {
            int idx = indexOf(setMethodSig, ')', 0);
            if(idx > 0 && idx < (strlen(setMethodSig)-1)) {
                switch(setMethodSig[idx+1]) {
                    case 'Z' : // boolean
                        envSetMethodImpl = &envSetReturnsBoolean;
                        break;
                    case 'B' : // byte
                        envSetMethodImpl = &envSetReturnsByte;
                        break;
                    case 'C' : // char
                        envSetMethodImpl = &envSetReturnsChar;
                        break;
                    case 'S' : // short
                        envSetMethodImpl = &envSetReturnsShort;
                        break;
                    case 'I' : // int
                        envSetMethodImpl = &envSetReturnsInt;
                        break;
                    case 'J' : // long
                        envSetMethodImpl = &envSetReturnsLong;
                        break;
                    case 'F' : // float
                        envSetMethodImpl = &envSetReturnsDouble;
                        break;
                    case 'D' : // double
                        envSetMethodImpl = &envSetReturnsFloat;
                        break;
                    case 'L' : // object
                    case '[' : // array
                        envSetMethodImpl = &envSetReturnsObject;
                        break;
                    default :
                        break;
                }
            }

            if(envSetMethodImpl)
                setImpl = &setMethod;
        } else {
            // log the method lookup failure -- don't clear the exception as
            // there is no appropriate error handling at this point
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Method lookup failed. Method: %s%s\n", setMethodName, setMethodSig);
        }
    }
}

JavaIntFieldAccess::~JavaIntFieldAccess()
{
}

jint JavaIntFieldAccess::get(JNIEnv *env, jobject object)
{
    return getImpl(this, env, object);
}

void JavaIntFieldAccess::set(JNIEnv *env, jobject object, jint value)
{
    setImpl(this, env, object, value);
}

bool JavaIntFieldAccess::isValid(bool get, bool set)
{
    return (!get || getImpl) && (!set || setImpl);
}

jint JavaIntFieldAccess::getDirect(JavaIntFieldAccess *self, JNIEnv *env, jobject object)
{
    return env->GetIntField(object, self->fieldId);
}

void JavaIntFieldAccess::setDirect(JavaIntFieldAccess *self, JNIEnv *env, jobject object, jint value)
{
    env->SetIntField(object, self->fieldId, value);
}

jint JavaIntFieldAccess::getMethod(JavaIntFieldAccess *self, JNIEnv *env, jobject object)
{
    return env->CallIntMethod(object, self->getMethodId);
}

void JavaIntFieldAccess::setMethod(JavaIntFieldAccess *self, JNIEnv *env, jobject object, jint value)
{
    self->envSetMethodImpl(env, object, self->setMethodId, value);
}


LocalJNIEnv::LocalJNIEnv() NOTHROWS :
    env(NULL),
    detach(false)
{
    if(vm) {
        int code = vm->GetEnv((void **)&env, JNI_VERSION_1_6);
        if(code == JNI_EDETACHED) {
            if(vm->AttachCurrentThread(&env, NULL) == 0) {
                detach = true;
            } else {
                env = NULL;
            }
        } else if(code != JNI_OK) {
            env = NULL;
        }
    }
}
LocalJNIEnv::~LocalJNIEnv() NOTHROWS
{
    if(detach && vm)
        vm->DetachCurrentThread();
}
bool LocalJNIEnv::valid() NOTHROWS
{
    return !!env;
}
JNIEnv *LocalJNIEnv::operator->() const NOTHROWS
{
    return env;
}
JNIEnv &LocalJNIEnv::operator*() const NOTHROWS
{
    return *env;
}
LocalJNIEnv::operator JNIEnv *() const NOTHROWS
{
    return env;
}

bool ATAKMapEngineJNI_checkOrThrow(JNIEnv *env, const TAKErr code) NOTHROWS
{
    switch(code) {
    case TE_Ok :
    case TE_Done : // XXX - throws?
    case TE_Canceled : // XXX - throws
    case TE_Busy : // XXX -
        return false;
    case TE_InvalidArg :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_IllegalArgumentException_class, "");
        return true;
    case TE_Interrupted :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_InterruptedException_class, "");
        return true;
    case TE_BadIndex :
        if(!env->ExceptionCheck())
             env->ThrowNew(java_lang_IndexOutOfBoundsException_class, "");
        return true;
    case TE_Unsupported :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_UnsupportedOperationException_class, "");
        return true;
    case TE_IO :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_io_IOException_class, "");
        return true;
    case TE_OutOfMemory :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_OutOfMemoryError_class, "");
        return true;
    case TE_IllegalState :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_IllegalStateException_class, "");
        return true;
    case TE_ConcurrentModification :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_util_ConcurrentModificationException_class, "");
        return true;
    case TE_EOF :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_io_EOFException_class, "");
        return true;
    case TE_Err :
    default :
       if(!env->ExceptionCheck())
           env->ThrowNew(java_lang_RuntimeException_class, "An error occurred");
        return true;
    }
}

jclass ATAKMapEngineJNI_findClass(JNIEnv *env, const char *name) NOTHROWS
{
    if(!name)
        return NULL;

    Mutex &m = mutex();
    LockPtr lock(NULL, NULL);
    if(Lock_create(lock, m) != TE_Ok)
        return NULL;

    std::map<std::string, jclass> &classMap = classRefMap();
    std::map<std::string, jclass>::iterator entry;
    entry = classMap.find(name);
    if(entry != classMap.end())
        return entry->second;
    if(!env)
        return NULL;
    jclass localClassRef = env->FindClass(name);
    if(!localClassRef) {
        Logger_log(TELL_Warning, "ATAKMapEngineJNI_findClass: failed to find %s", name);
        return NULL;
    }
    jclass clazz = (jclass)env->NewGlobalRef(localClassRef);
    env->DeleteLocalRef(localClassRef);
    classMap[name] = clazz;
    return clazz;
}

bool ATAKMapEngineJNI_equals(JNIEnv *env, jobject a, jobject b) NOTHROWS
{
    if(!a && !b)
        return true;
    else if(!a)
        return false;
    else if(!b)
        return false;
    // both are non-null
    if(env->IsSameObject(a, b))
        return true;
    jclass Object_class = ATAKMapEngineJNI_findClass(env, "java/lang/Object");
    jmethodID Object_equals = env->GetMethodID(Object_class, "equals", "(Ljava/lang/Object;)Z");
    return env->CallBooleanMethod(a, Object_equals, b);
}

TAKErr ProgressCallback_dispatchProgress(jobject jcallback, jint value) NOTHROWS
{
    LocalJNIEnv env;
    if(!env.valid())
        return TE_Err;
    if(env->ExceptionCheck())
        return TE_Err;
    env->CallVoidMethod(jcallback, ProgressCallback_progress, value);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ProgressCallback_dispatchError(jobject jcallback, const char *value) NOTHROWS
{
    LocalJNIEnv env;
    if(!env.valid())
        return TE_Err;
    if(env->ExceptionCheck())
        return TE_Err;
    env->CallVoidMethod(jcallback, ProgressCallback_error, value ? env->NewStringUTF(value) : NULL);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}

namespace {

int LogcatLogger::print(const LogLevel lvl, const char *fmt, va_list arg) NOTHROWS
{
    android_LogPriority prio;
    switch(lvl) {
    case TELL_All :
        prio = ANDROID_LOG_VERBOSE;
        break;
    case TELL_Debug :
        prio =  ANDROID_LOG_DEBUG;
        break;
    case TELL_Info :
        prio = ANDROID_LOG_INFO;
        break;
    case TELL_Warning :
        prio = ANDROID_LOG_WARN;
        break;
    case TELL_Error :
        prio = ANDROID_LOG_ERROR;
        break;
    case TELL_Severe :
        prio = ANDROID_LOG_FATAL;
        break;
    case TELL_None :
        prio = ANDROID_LOG_SILENT;
        break;
    default :
        prio = ANDROID_LOG_DEFAULT;
        break;
    }
    __android_log_vprint(prio, "ATAKMapEngineJNI", fmt, arg);
    return 0;
}

#define PGSC_ENV_SET_METHOD_FN_DEFN(type)                                      \
        void envSetReturns ## type (JNIEnv *env,                               \
                                    jobject object,                            \
                                    jmethodID methodId,                        \
                                    ...)                                       \
        {                                                                      \
            va_list args;                                                      \
            va_start(args, methodId);                                          \
                                                                               \
            env->Call ## type ## MethodV(object, methodId, args);              \
        }                                                                      \

PGSC_ENV_SET_METHOD_FN_DEFN(Void)
PGSC_ENV_SET_METHOD_FN_DEFN(Byte)
PGSC_ENV_SET_METHOD_FN_DEFN(Boolean)
PGSC_ENV_SET_METHOD_FN_DEFN(Char)
PGSC_ENV_SET_METHOD_FN_DEFN(Short)
PGSC_ENV_SET_METHOD_FN_DEFN(Int)
PGSC_ENV_SET_METHOD_FN_DEFN(Long)
PGSC_ENV_SET_METHOD_FN_DEFN(Float)
PGSC_ENV_SET_METHOD_FN_DEFN(Double)
PGSC_ENV_SET_METHOD_FN_DEFN(Object)

#undef PGSC_ENV_SET_METHOD_FN_DEFN

int indexOf(const char *str, const char c, const int from)
{
    const int l = strlen(str);
    for(int i = from; i < l; i++)
        if(str[i] == c)
            return i;
    return -1;
}

Logger2 &getAndroidLogger()
{
    static LogcatLogger logcatLogger;
    return logcatLogger;
}

std::map<std::string, jclass> &classRefMap()
{
    static std::map<std::string, jclass> m;
    return m;
}

Mutex &mutex()
{
    static Mutex m;
    return m;
}

void sqliteLogCallback(void *pArg, int iErrCode, const char *zMsg)
{
    const char *errStr = sqlite3_errstr(iErrCode);
    if(errStr)
        Logger_log(TELL_Debug, "SQLITE [%s] %s", errStr, zMsg ? zMsg : "");
    else
        Logger_log(TELL_Debug, "SQLITE [code=%d] %s", zMsg ? zMsg : "");
}

};

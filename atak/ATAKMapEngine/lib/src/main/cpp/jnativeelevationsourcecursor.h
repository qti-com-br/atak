/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_atakmap_map_elevation_NativeElevationSourceCursor */

#ifndef _Included_com_atakmap_map_elevation_NativeElevationSourceCursor
#define _Included_com_atakmap_map_elevation_NativeElevationSourceCursor
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_destruct
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    moveToNext
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_moveToNext
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    get
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_get
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getResolution
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getResolution
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    isAuthoritative
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_isAuthoritative
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getCE
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getCE
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getLE
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getLE
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getUri
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getUri
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getType
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getType
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getBounds
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getBounds
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_elevation_NativeElevationSourceCursor
 * Method:    getFlags
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_elevation_NativeElevationSourceCursor_getFlags
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
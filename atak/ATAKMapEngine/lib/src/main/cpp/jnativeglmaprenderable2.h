/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_atakmap_map_opengl_NativeGLMapRenderable2 */

#ifndef _Included_com_atakmap_map_opengl_NativeGLMapRenderable2
#define _Included_com_atakmap_map_opengl_NativeGLMapRenderable2
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_atakmap_map_opengl_NativeGLMapRenderable2
 * Method:    draw
 * Signature: (JLcom/atakmap/map/opengl/GLMapView;I)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeGLMapRenderable2_draw
  (JNIEnv *, jclass, jlong, jobject, jint);

/*
 * Class:     com_atakmap_map_opengl_NativeGLMapRenderable2
 * Method:    release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeGLMapRenderable2_release
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_opengl_NativeGLMapRenderable2
 * Method:    getRenderPass
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_opengl_NativeGLMapRenderable2_getRenderPass
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_opengl_NativeGLMapRenderable2
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeGLMapRenderable2_destruct
  (JNIEnv *, jclass, jobject);

#ifdef __cplusplus
}
#endif
#endif
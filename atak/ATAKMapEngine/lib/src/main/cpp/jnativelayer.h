/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_atakmap_map_layer_NativeLayer */

#ifndef _Included_com_atakmap_map_layer_NativeLayer
#define _Included_com_atakmap_map_layer_NativeLayer
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    getPointer
 * Signature: (Lcom/atakmap/map/layer/Layer;)J
 */
JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_NativeLayer_getPointer
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    wrap
 * Signature: (Lcom/atakmap/map/layer/Layer;)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_NativeLayer_wrap
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    hasPointer
 * Signature: (Lcom/atakmap/map/layer/Layer;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_NativeLayer_hasPointer
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    create
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)Lcom/atakmap/map/layer/Layer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_NativeLayer_create
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    hasObject
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_NativeLayer_hasObject
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    getObject
 * Signature: (J)Lcom/atakmap/map/layer/Layer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_NativeLayer_getObject
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_NativeLayer_destruct
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    setVisible
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_NativeLayer_setVisible
  (JNIEnv *, jclass, jlong, jboolean);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    isVisible
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_NativeLayer_isVisible
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    addOnLayerVisibleChangedListener
 * Signature: (JLcom/atakmap/map/layer/Layer/OnLayerVisibleChangedListener;)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_NativeLayer_addOnLayerVisibleChangedListener
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    removeOnLayerVisibleChangedListener
 * Signature: (JLcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_NativeLayer_removeOnLayerVisibleChangedListener
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    getName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_NativeLayer_getName
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    VisibilityListener_destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_NativeLayer_VisibilityListener_1destruct
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_NativeLayer
 * Method:    VisibilityListener_visibilityChanged
 * Signature: (JLcom/atakmap/map/layer/Layer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_NativeLayer_VisibilityListener_1visibilityChanged
  (JNIEnv *, jclass, jlong, jobject);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class com_atakmap_map_layer_NativeLayer_NativeOnLayerVisibilityChangedListener */

#ifndef _Included_com_atakmap_map_layer_NativeLayer_NativeOnLayerVisibilityChangedListener
#define _Included_com_atakmap_map_layer_NativeLayer_NativeOnLayerVisibilityChangedListener
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif

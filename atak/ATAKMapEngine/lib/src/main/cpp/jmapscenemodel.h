/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_atakmap_map_MapSceneModel */

#ifndef _Included_com_atakmap_map_MapSceneModel
#define _Included_com_atakmap_map_MapSceneModel
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    gsd
 * Signature: (DDI)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_gsd
  (JNIEnv *, jclass, jdouble, jdouble, jint);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    range
 * Signature: (DDI)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_range
  (JNIEnv *, jclass, jdouble, jdouble, jint);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    clone
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_clone
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_destruct
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    create
 * Signature: (DIIIDDDZFFDDD)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_create
  (JNIEnv *, jclass, jdouble, jint, jint, jint, jdouble, jdouble, jdouble, jboolean, jfloat, jfloat, jdouble, jdouble, jdouble);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    set
 * Signature: (JDIIIDDDZFFDDD)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_set__JDIIIDDDZFFDDD
  (JNIEnv *, jclass, jlong, jdouble, jint, jint, jint, jdouble, jdouble, jdouble, jboolean, jfloat, jfloat, jdouble, jdouble, jdouble);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    set
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_set__JJ
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getEarth
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getEarth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getProjection
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_MapSceneModel_getProjection
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getForward
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getForward
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getInverse
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getInverse
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_MapSceneModel_getWidth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_MapSceneModel_getHeight
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getFocusX
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_MapSceneModel_getFocusX
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getFocusY
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_MapSceneModel_getFocusY
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getGsd
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getGsd
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getDisplayModel
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getDisplayModel
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getDpi
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getDpi
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraProjection
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getCameraProjection
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraModelView
 * Signature: (J)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_MapSceneModel_getCameraModelView
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraLocation
 * Signature: (JLcom/atakmap/math/PointD;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_getCameraLocation
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraTarget
 * Signature: (JLcom/atakmap/math/PointD;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_getCameraTarget
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraRoll
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraRoll
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraElevation
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraElevation
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraAzimuth
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraAzimuth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraFov
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraFov
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraAspectRatio
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraAspectRatio
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraNear
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraNear
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraFar
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraFar
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraNearMeters
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraNearMeters
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    getCameraFarMeters
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_MapSceneModel_getCameraFarMeters
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    isCameraPerspective
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_isCameraPerspective
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    forward
 * Signature: (JDDDZLcom/atakmap/math/PointD;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_forward
  (JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble, jboolean, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    inverse
 * Signature: (JDDDZLcom/atakmap/coremap/maps/coords/GeoPoint;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_inverse__JDDDZLcom_atakmap_coremap_maps_coords_GeoPoint_2
  (JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble, jboolean, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    inverse
 * Signature: (JDDDJLcom/atakmap/coremap/maps/coords/GeoPoint;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_inverse__JDDDJLcom_atakmap_coremap_maps_coords_GeoPoint_2
  (JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble, jlong, jobject);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    setPerspectiveCameraEnabled
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_MapSceneModel_setPerspectiveCameraEnabled
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    isPerspectiveCameraEnabled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_isPerspectiveCameraEnabled
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    intersectsAAbbWgs84
 * Signature: (JDDDDDD)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_intersectsAAbbWgs84
  (JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);

/*
 * Class:     com_atakmap_map_MapSceneModel
 * Method:    intersectsSphereWgs84
 * Signature: (JDDDD)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_MapSceneModel_intersectsSphereWgs84
  (JNIEnv *, jclass, jlong, jdouble, jdouble, jdouble, jdouble);

#ifdef __cplusplus
}
#endif
#endif
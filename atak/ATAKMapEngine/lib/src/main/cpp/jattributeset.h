/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_atakmap_map_layer_feature_AttributeSet */

#ifndef _Included_com_atakmap_map_layer_feature_AttributeSet
#define _Included_com_atakmap_map_layer_feature_AttributeSet
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    create
 * Signature: ()Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_create__
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    create
 * Signature: (Lcom/atakmap/interop/Pointer;)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_create__Lcom_atakmap_interop_Pointer_2
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_destruct
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getIntAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getIntAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getLongAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getLongAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getDoubleAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)D
 */
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getDoubleAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getStringAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getStringAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getBinaryAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getBinaryAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getIntArrayAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[I
 */
JNIEXPORT jintArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getIntArrayAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getLongArrayAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[J
 */
JNIEXPORT jlongArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getLongArrayAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getDoubleArrayAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getDoubleArrayAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getStringArrayAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getStringArrayAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getBinaryArrayAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)[[B
 */
JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getBinaryArrayAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getAttributeSetAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)Lcom/atakmap/interop/Pointer;
 */
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getAttributeSetAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getAttributeType
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getAttributeType
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2I
  (JNIEnv *, jclass, jobject, jstring, jint);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2J
  (JNIEnv *, jclass, jobject, jstring, jlong);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;D)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2D
  (JNIEnv *, jclass, jobject, jstring, jdouble);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *, jclass, jobject, jstring, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[B)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3B
  (JNIEnv *, jclass, jobject, jstring, jbyteArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[I)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3I
  (JNIEnv *, jclass, jobject, jstring, jintArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[J)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3J
  (JNIEnv *, jclass, jobject, jstring, jlongArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[D)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3D
  (JNIEnv *, jclass, jobject, jstring, jdoubleArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3Ljava_lang_String_2
  (JNIEnv *, jclass, jobject, jstring, jobjectArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;[[B)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2_3_3B
  (JNIEnv *, jclass, jobject, jstring, jobjectArray);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    setAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_setAttribute__Lcom_atakmap_interop_Pointer_2Ljava_lang_String_2Lcom_atakmap_interop_Pointer_2
  (JNIEnv *, jclass, jobject, jstring, jobject);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    removeAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_removeAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getAttributeNames
 * Signature: (Lcom/atakmap/interop/Pointer;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getAttributeNames
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    containsAttribute
 * Signature: (Lcom/atakmap/interop/Pointer;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_containsAttribute
  (JNIEnv *, jclass, jobject, jstring);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    clear
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_clear
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getINT
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getINT
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getLONG
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getLONG
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getDOUBLE
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getDOUBLE
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getSTRING
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getSTRING
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getBLOB
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getBLOB
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getATTRIBUTE_SET
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getATTRIBUTE_1SET
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getINT_ARRAY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getINT_1ARRAY
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getLONG_ARRAY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getLONG_1ARRAY
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getDOUBLE_ARRAY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getDOUBLE_1ARRAY
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getBLOB_ARRAY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getBLOB_1ARRAY
  (JNIEnv *, jclass);

/*
 * Class:     com_atakmap_map_layer_feature_AttributeSet
 * Method:    getSTRING_ARRAY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_AttributeSet_getSTRING_1ARRAY
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class com_atakmap_map_layer_feature_AttributeSet_Types */

#ifndef _Included_com_atakmap_map_layer_feature_AttributeSet_Types
#define _Included_com_atakmap_map_layer_feature_AttributeSet_Types
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
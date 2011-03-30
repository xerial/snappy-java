#include <snappy.h>
#include "SnappyNative.h"

JNIEXPORT jstring JNICALL Java_org_xerial_snappy_Snappy_nativeLibraryVersion
  (JNIEnv * env, jclass self)
{
	return env->NewStringUTF("1.0.1");
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    compress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_compress
  (JNIEnv* env, jclass self, jobject uncompressed, jobject compressed)
{
	void* uncompressedBuffer = env->GetDirectBufferAddress(uncompressed);


	return (jlong) 0;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    uncompress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_xerial_snappy_Snappy_uncompress
  (JNIEnv *, jclass, jobject, jobject)
{

	return (jboolean) true;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    maxCompressedLength
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_maxCompressedLength
  (JNIEnv *, jclass, jlong)
{

	return (jlong) 0;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    getUncompressedLength
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_getUncompressedLength
  (JNIEnv *, jclass, jobject)
{

	return (jlong) 0;
}



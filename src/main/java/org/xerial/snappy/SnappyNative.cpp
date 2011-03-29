#include <snappy.h>
#include "SnappyNative.h"

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    compress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_compress
  (JNIEnv *, jobject, jobject, jobject)
{
	return (jlong) 0;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    uncompress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_xerial_snappy_Snappy_uncompress
  (JNIEnv *, jobject, jobject, jobject)
{

	return (jboolean) true;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    maxCompressedLength
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_maxCompressedLength
  (JNIEnv *, jobject, jlong)
{

	return (jlong) 0;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    getUncompressedLength
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_org_xerial_snappy_Snappy_getUncompressedLength
  (JNIEnv *, jobject, jobject)
{

	return (jlong) 0;
}



#include <string>
#include <snappy.h>
#include "SnappyNative.h"

JNIEXPORT jstring JNICALL Java_org_xerial_snappy_SnappyNative_nativeLibraryVersion
  (JNIEnv * env, jclass self)
{
	return env->NewStringUTF("1.0.1");
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    compress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawCompress
  (JNIEnv* env, jclass self, jobject uncompressed, jint upos, jint ulen, jobject compressed, jint cpos)
{
	char* uncompressedBuffer = (char*) env->GetDirectBufferAddress(uncompressed);
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	size_t compressedLength;

	snappy::RawCompress(uncompressedBuffer, (size_t) ulen, compressedBuffer, &compressedLength);
	return (jint) compressedLength;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    uncompress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_xerial_snappy_SnappyNative_rawDecompress
  (JNIEnv * env, jclass self, jobject compressed, jint cpos, jint clen, jobject decompressed, jint dpos)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed) + cpos;
	char* decompressedBuffer = (char*) env->GetDirectBufferAddress(decompressed) + dpos;

	size_t decompressedLength;
	snappy::GetUncompressedLength(compressedBuffer, (size_t) clen, &decompressedLength);
	bool ret = snappy::RawUncompress(compressedBuffer, (size_t) clen, decompressedBuffer);

	return (jboolean) ret;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    maxCompressedLength
 * Signature: (J)J
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_maxCompressedLength
  (JNIEnv *, jclass, jint size)
{
	size_t l = snappy::MaxCompressedLength((size_t) size);
	return (jint) l;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    getUncompressedLength
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_getUncompressedLength
  (JNIEnv * env, jclass self, jobject compressed, jint cpos, jint clen)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	size_t result;
	std::string s = "hello world";
	//snappy::GetUncompressedLength(compressedBuffer, (size_t) clen, &result);
	snappy::GetUncompressedLength(s.c_str(), s.length(), &result);

	return (jint) result;
}



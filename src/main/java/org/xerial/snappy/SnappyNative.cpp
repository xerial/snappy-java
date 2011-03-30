#include <string>
#include <snappy.h>
#include "SnappyNative.h"

void throw_exception(JNIEnv *env, jclass self, int errorCode)
{
    jmethodID mth_throwex = 0;

    if (!mth_throwex)
        mth_throwex = env->GetMethodID(self, "throw_error", "(I)V");

    env->CallVoidMethod(self, mth_throwex, (jint) errorCode);
}


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
	char* uncompressedBuffer = (char*) env->GetDirectBufferAddress(uncompressed) + upos;
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed) + cpos;
	size_t compressedLength;

	snappy::RawCompress(uncompressedBuffer, (size_t) ulen, compressedBuffer, &compressedLength);
	return (jint) compressedLength;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    uncompress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawUncompress
  (JNIEnv * env, jclass self, jobject compressed, jint cpos, jint clen, jobject decompressed, jint dpos)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed) + cpos;
	char* decompressedBuffer = (char*) env->GetDirectBufferAddress(decompressed) + dpos;

	size_t decompressedLength;
	snappy::GetUncompressedLength(compressedBuffer, (size_t) clen, &decompressedLength);
	bool ret = snappy::RawUncompress(compressedBuffer, (size_t) clen, decompressedBuffer);
	if(!ret) {
		throw_exception(env, self, 2);
		return 0;
	}

	return (jint) decompressedLength;
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
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed) + cpos;
	size_t result;
	bool ret = snappy::GetUncompressedLength(compressedBuffer, (size_t) clen, &result);
	if(!ret) {
		throw_exception(env, self, 2);
		return 0;
	}
	return (jint) result;
}



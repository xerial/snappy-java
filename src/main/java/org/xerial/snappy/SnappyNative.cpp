/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
#include <string>
#include <cstring>
#include <snappy.h>
#include "SnappyNative.h"

void throw_exception(JNIEnv *env, jobject self, int errorCode)
{
	jclass c = env->FindClass("Lorg/xerial/snappy/SnappyNative;");
	if(c==0)
		return;
    jmethodID mth_throwex = env->GetMethodID(c, "throw_error", "(I)V");
    if(mth_throwex == 0)
    	return;
    env->CallVoidMethod(self, mth_throwex, (jint) errorCode);
}


JNIEXPORT jstring JNICALL Java_org_xerial_snappy_SnappyNative_nativeLibraryVersion
  (JNIEnv * env, jobject self)
{
	return env->NewStringUTF("1.0.4");
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    compress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawCompress__Ljava_nio_ByteBuffer_2IILjava_nio_ByteBuffer_2I
  (JNIEnv* env, jobject self, jobject uncompressed, jint upos, jint ulen, jobject compressed, jint cpos)
{
	char* uncompressedBuffer = (char*) env->GetDirectBufferAddress(uncompressed);
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	if(uncompressedBuffer == 0 || compressedBuffer == 0) {
		throw_exception(env, self, 3);
		return (jint) 0;
	}

	size_t compressedLength;
	snappy::RawCompress(uncompressedBuffer + upos, (size_t) ulen, compressedBuffer + cpos, &compressedLength);
	return (jint) compressedLength;
}


JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawCompress__Ljava_lang_Object_2IILjava_lang_Object_2I
  (JNIEnv * env, jobject self, jobject input, jint inputOffset, jint inputLen, jobject output, jint outputOffset)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	char* out = (char*) env->GetPrimitiveArrayCritical((jarray) output, 0);
	if(in == 0 || out == 0) {
		// out of memory
		throw_exception(env, self, 4);
		return 0;
	}

	size_t compressedLength;
	snappy::RawCompress(in + inputOffset, (size_t) inputLen, out + outputOffset, &compressedLength);

	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
	env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);

	return (jint) compressedLength;
}

JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawUncompress__Ljava_lang_Object_2IILjava_lang_Object_2I
(JNIEnv * env, jobject self, jobject input, jint inputOffset, jint inputLength, jobject output, jint outputOffset)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	char* out = (char*) env->GetPrimitiveArrayCritical((jarray) output, 0);
	if(in == 0 || out == 0) {
		// out of memory
		throw_exception(env, self, 4);
		return 0;
	}

	size_t uncompressedLength;
	snappy::GetUncompressedLength(in + inputOffset, (size_t) inputLength, &uncompressedLength);
	bool ret = snappy::RawUncompress(in + inputOffset, (size_t) inputLength, out + outputOffset);

	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
	env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);

	if(!ret) {
		throw_exception(env, self, 5);
		return 0;
	}

	return (jint) uncompressedLength;
}


/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    uncompress
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_rawUncompress__Ljava_nio_ByteBuffer_2IILjava_nio_ByteBuffer_2I
  (JNIEnv * env, jobject self, jobject compressed, jint cpos, jint clen, jobject decompressed, jint dpos)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	char* decompressedBuffer = (char*) env->GetDirectBufferAddress(decompressed);
	if(compressedBuffer == 0 || decompressedBuffer == 0) {
		throw_exception(env, self, 3);
		return (jint) 0;
	}

	size_t decompressedLength;
	snappy::GetUncompressedLength(compressedBuffer + cpos, (size_t) clen, &decompressedLength);
	bool ret = snappy::RawUncompress(compressedBuffer + cpos, (size_t) clen, decompressedBuffer + dpos);
	if(!ret) {
		throw_exception(env, self, 5);
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
  (JNIEnv *, jobject, jint size)
{
	size_t l = snappy::MaxCompressedLength((size_t) size);
	return (jint) l;
}

/*
 * Class:     org_xerial_snappy_Snappy
 * Method:    getUncompressedLength
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_uncompressedLength__Ljava_nio_ByteBuffer_2II
  (JNIEnv * env, jobject self, jobject compressed, jint cpos, jint clen)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	if(compressedBuffer == 0) {
		throw_exception(env, self, 3);
		return (jint) 0;
	}

	size_t result;
	bool ret = snappy::GetUncompressedLength(compressedBuffer + cpos, (size_t) clen, &result);
	if(!ret) {
		throw_exception(env, self, 2);
		return 0;
	}
	return (jint) result;
}

JNIEXPORT jint JNICALL Java_org_xerial_snappy_SnappyNative_uncompressedLength__Ljava_lang_Object_2II
  (JNIEnv * env, jobject self, jobject input, jint offset, jint length)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	if(in == 0) {
		// out of memory
		throw_exception(env, self, 4);
		return 0;
	}

	size_t result;
	bool ret = snappy::GetUncompressedLength(in + offset, (size_t) length, &result);
	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);

	if(!ret) {
		throw_exception(env, self, 2);
		return 0;
	}

	return (jint) result;
}

JNIEXPORT jboolean JNICALL Java_org_xerial_snappy_SnappyNative_isValidCompressedBuffer__Ljava_nio_ByteBuffer_2II
  (JNIEnv * env, jobject self, jobject compressed, jint cpos, jint clen)
{
	char* compressedBuffer = (char*) env->GetDirectBufferAddress(compressed);
	if(compressedBuffer == 0) {
		throw_exception(env, self, 3);
		return (jint) 0;
	}
	bool ret = snappy::IsValidCompressedBuffer(compressedBuffer + cpos, (size_t) clen);
	return ret;
}


JNIEXPORT jboolean JNICALL Java_org_xerial_snappy_SnappyNative_isValidCompressedBuffer__Ljava_lang_Object_2II
  (JNIEnv * env, jobject self, jobject input, jint offset, jint length)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	if(in == 0) {
		// out of memory
		throw_exception(env, self, 4);
		return 0;
	}
	bool ret = snappy::IsValidCompressedBuffer(in + offset, (size_t) length);
	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
	return ret;
}

JNIEXPORT void JNICALL Java_org_xerial_snappy_SnappyNative_arrayCopy
  (JNIEnv * env, jobject self, jobject input, jint offset, jint length, jobject output, jint output_offset)
{
	char* src = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	char* dest = (char*) env->GetPrimitiveArrayCritical((jarray) output, 0);
	if(src == 0 || dest == 0) {
		// out of memory
		throw_exception(env, self, 4);
		return;
	}

	memcpy(dest+output_offset, src+offset, (size_t) length);

	env->ReleasePrimitiveArrayCritical((jarray) input, src, 0);
	env->ReleasePrimitiveArrayCritical((jarray) output, dest, 0);
}



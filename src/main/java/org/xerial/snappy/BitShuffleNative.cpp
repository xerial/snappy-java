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
#include <bitshuffle.h>
#include "BitShuffleNative.h"

inline void throw_exception(JNIEnv *env, jobject self, int errorCode)
{
	jclass c = env->FindClass("org/xerial/snappy/SnappyNative");
	if(c==0)
		return;
	jmethodID mth_throwex = env->GetMethodID(c, "throw_error", "(I)V");
	if(mth_throwex == 0)
		return;
	env->CallVoidMethod(self, mth_throwex, (jint) errorCode);
}

/*
 * Class:     org_xerial_snappy_SnappyNative
 * Method:    bitShuffle
 * Signature: (Ljava/lang/Object;IIILjava/lang/Object;I)I
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_BitShuffleNative_bitShuffle
  (JNIEnv * env, jobject self, jobject input, jint inputOffset, jint typeSize, jint length, jobject output, jint outputOffset)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	char* out = (char*) env->GetPrimitiveArrayCritical((jarray) output, 0);
	if(in == 0 || out == 0) {
		// out of memory
		if(in != 0) {
			env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
		}
		if(out != 0) {
			env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);
		}
		throw_exception(env, self, 4);
		return 0;
	}

        int64_t processedBytes = bshuf_bitshuffle(
                        in + inputOffset, out + outputOffset, (size_t) (length / typeSize), (size_t) typeSize, 0);

	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
	env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);

	return (jint) processedBytes;
}

/*
 * Class:     org_xerial_snappy_SnappyNative
 * Method:    bitUnShuffle
 * Signature: (Ljava/lang/Object;IIILjava/lang/Object;I)I
 */
JNIEXPORT jint JNICALL Java_org_xerial_snappy_BitShuffleNative_bitUnShuffle
  (JNIEnv * env, jobject self, jobject input, jint inputOffset, jint typeSize, jint length, jobject output, jint outputOffset)
{
	char* in = (char*) env->GetPrimitiveArrayCritical((jarray) input, 0);
	char* out = (char*) env->GetPrimitiveArrayCritical((jarray) output, 0);
	if(in == 0 || out == 0) {
		// out of memory
		if(in != 0) {
			env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
		}
		if(out != 0) {
			env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);
		}
		throw_exception(env, self, 4);
		return 0;
	}

        int64_t processedBytes = bshuf_bitunshuffle(
                        in + inputOffset, out + outputOffset, (size_t) (length / typeSize), (size_t) typeSize, 0);

	env->ReleasePrimitiveArrayCritical((jarray) input, in, 0);
	env->ReleasePrimitiveArrayCritical((jarray) output, out, 0);

	return (jint) processedBytes;
}


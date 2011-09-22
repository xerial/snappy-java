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

/**
 * Snappy API for compressing/decompressing data.
 * 
 * Usage
 * First, import {@link org.xerial.snappy.Snappy} in your Java code:
 * <code>
 * <pre>
 * import org.xerial.snappy.Snappy;
 * </pre>
 * </code>
 * Then use {@link org.xerial.snappy.Snappy#compress(byte[])} and {@link org.xerial.snappy.Snappy#uncompress(byte[])}:
 * <code>
 * <pre>
 * String input = "Hello snappy-java! Snappy-java is a JNI-based wrapper of Snappy, a fast compresser/decompresser.";
 * byte[] compressed = Snappy.compress(input.getBytes("UTF-8"));
 * byte[] uncompressed = Snappy.uncompress(compressed);
 * String result = new String(uncompressed, "UTF-8");
 * System.out.println(result);
 * </pre>
 * </code>
 * 
 * <p>In addition, high-level methods (Snappy.compress(String), Snappy.compress(float[] ..) etc. ) and low-level ones (e.g. Snappy.rawCompress(.. ), Snappy.rawUncompress(..), etc.), which minimize memory copies, can be used. </p>
 * 
 * <h3>Stream-based API</h3>
 * Stream-based compressor/decompressor SnappyOutputStream, SnappyInputStream are also available for reading/writing large data sets.
 */
package org.xerial.snappy;


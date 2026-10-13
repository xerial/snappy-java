// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package org.xerial.snappy.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Fuzzer for Snappy's block-based stream format, as implemented by
 * {@link SnappyOutputStream} and {@link SnappyInputStream}.
 * This is different from the "x-snappy-framed" format.
 */
public class SnappyStreamFuzzer {
    
    private static void runFuzz(FuzzBlock block) {
        try {
            block.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @FuzzTest
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 1);
        
        if (selector == 0) {
            testRoundtrip(data);
        } else {
            testMalformedInput(data);
        }
    }
    
    private static void testRoundtrip(FuzzedDataProvider data) {
        byte[] original = data.consumeRemainingAsBytes();
        int bufferSize = data.consumeInt(1, 8192);
        
        runFuzz(() -> {
            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            try (SnappyOutputStream snappyOut = new SnappyOutputStream(compressedBuf)) {
                snappyOut.write(original);
            }
            byte[] compressed = compressedBuf.toByteArray();

            byte[] uncompressed;
            try (SnappyInputStream snappyIn = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[bufferSize];
                int readBytes;
                while ((readBytes = snappyIn.read(buf)) != -1) {
                    out.write(buf, 0, readBytes);
                }
                uncompressed = out.toByteArray();
            }

            if (!Arrays.equals(original, uncompressed)) {
                throw new IllegalStateException("Original and uncompressed data are different");
            }
        });
    }
    
    private static void testMalformedInput(FuzzedDataProvider data) {
        runFuzz(() -> {
            try (SnappyInputStream in = new SnappyInputStream(
                new ByteArrayInputStream(data.consumeBytes(100)))) {
                while (in.read() != -1) {}
            } catch (IOException e) {
                // Expected for malformed input during fuzzing
            }
        });
    }
}

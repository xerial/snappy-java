/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xerial.snappy.pure;

final class SnappyConstants
{
    static final int SIZE_OF_SHORT = 2;
    static final int SIZE_OF_INT = 4;
    static final int SIZE_OF_LONG = 8;

    static final int LITERAL = 0;
    static final int COPY_1_BYTE_OFFSET = 1;  // 3 bit length + 3 bits of offset in opcode
    static final int COPY_2_BYTE_OFFSET = 2;

    private SnappyConstants()
    {
    }
}

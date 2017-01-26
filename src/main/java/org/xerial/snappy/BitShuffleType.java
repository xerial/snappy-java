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
//--------------------------------------
// XerialJ
//
// SnappyErrorCode.java
// Since: 2011/03/30 14:56:50
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

/**
 * Type codes used in ByteBuffer based BitShuffle APIs
 *
 * @author leo
 */
public enum BitShuffleType
{

    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8),
    FLOAT(4),
    DOUBLE(8);

    public final int id;

    private BitShuffleType(int id)
    {
        this.id = id;
    }

    public int getTypeSize()
    {
        return id;
    }
}

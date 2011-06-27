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
 * Error codes of snappy-java
 * 
 * @author leo
 * 
 */
public enum SnappyErrorCode {

    // DO NOT change these error code IDs because these numbers are used inside SnappyNative.cpp
    UNKNOWN(0),
    FAILED_TO_LOAD_NATIVE_LIBRARY(1),
    PARSING_ERROR(2),
    NOT_A_DIRECT_BUFFER(3),
    OUT_OF_MEMORY(4),
    FAILED_TO_UNCOMPRESS(5);

    public final int id;

    private SnappyErrorCode(int id) {
        this.id = id;
    }

    public static SnappyErrorCode getErrorCode(int id) {
        for (SnappyErrorCode code : SnappyErrorCode.values()) {
            if (code.id == id)
                return code;
        }
        return UNKNOWN;
    }

    public static String getErrorMessage(int id) {
        return getErrorCode(id).name();
    }
}

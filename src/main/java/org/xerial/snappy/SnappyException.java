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
// SnappyException.java
// Since: 2011/03/30 14:56:14
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;

/**
 * Exception in snappy-java
 * 
 * @deprecated Snappy-java now uses {@link IOException}
 * @author leo
 * 
 */
@Deprecated
public class SnappyException extends Exception
{
    private static final long    serialVersionUID = 1L;

    public final SnappyErrorCode errorCode;

    public SnappyException(int code) {
        this(SnappyErrorCode.getErrorCode(code));
    }

    public SnappyException(SnappyErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public SnappyException(SnappyErrorCode errorCode, Exception e) {
        super(e);
        this.errorCode = errorCode;
    }

    public SnappyException(SnappyErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SnappyErrorCode getErrorCode() {
        return errorCode;
    }

    public static void throwException(int errorCode) throws SnappyException {
        throw new SnappyException(errorCode);
    }

    @Override
    public String getMessage() {
        return String.format("[%s] %s", errorCode.name(), super.getMessage());
    }
}

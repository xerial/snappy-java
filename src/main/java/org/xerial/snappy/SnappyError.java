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
// SnappyError.java
// Since: 2011/03/30 15:22:43
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

/**
 * Used when serious errors (unchecked exception) are observed.
 * 
 * @author leo
 * 
 */
public class SnappyError extends Error
{
    /**
     * 
     */
    private static final long    serialVersionUID = 1L;

    public final SnappyErrorCode errorCode;

    public SnappyError(SnappyErrorCode code) {
        super();
        this.errorCode = code;
    }

    public SnappyError(SnappyErrorCode code, Error e) {
        super(e);
        this.errorCode = code;
    }

    public SnappyError(SnappyErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] %s", errorCode.name(), super.getMessage());
    }

}

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
// SnappyNativeLoader.java
// Since: 2011/07/04 12:10:28
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.util.HashMap;

public class SnappyNativeLoader
{
    private static HashMap<String, Boolean> loadedLibFiles = new HashMap<String, Boolean>();
    private static HashMap<String, Boolean> loadedLib      = new HashMap<String, Boolean>();

    public static synchronized void load(String lib) {
        if (loadedLibFiles.containsKey(lib) && loadedLibFiles.get(lib) == true)
            return;

        try {
            System.load(lib);
            loadedLibFiles.put(lib, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void loadLibrary(String libname) {
        if (loadedLib.containsKey(libname) && loadedLib.get(libname) == true)
            return;

        try {
            System.loadLibrary(libname);
            loadedLib.put(libname, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

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
// SnappyBundleActivator.java
// Since: 2011/06/22 10:01:46
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.jar.Manifest;

/**
 * OSGi bundle entry point
 *
 * @author leo
 */
public class SnappyBundleActivator
        implements BundleActivator
{
    /**
     * Name of the Snappy native library
     */
    public static final String LIBRARY_NAME = "snappyjava";

    /**
     * Make a call to {@link System#loadLibrary(String)} to load the native library which assumes
     * that the library is available on the path based on this {@link Bundle}'s {@link Manifest}.
     */
    public void start(BundleContext context)
            throws Exception
    {
        String library = System.mapLibraryName(LIBRARY_NAME);
        String osArch = System.getProperty("os.arch");

        if (library.toLowerCase().endsWith(".dylib") && "x86".equals(osArch)) {
            // some MacOS JDK7+ vendors map to dylib instead of jnilib
            library = library.replace(".dylib", ".jnilib");
        }
        System.loadLibrary(library);
        SnappyLoader.setSnappyApi(new SnappyNative());
    }

    public void stop(BundleContext context)
            throws Exception
    {
        SnappyLoader.setSnappyApi(null);
        SnappyLoader.cleanUpExtractedNativeLib();
    }
}

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
// SnappyLoaderTest.java
// Since: 2011/06/22 23:59:47
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Test;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

public class SnappyLoaderTest
{
    private static Logger _logger = Logger.getLogger(SnappyLoaderTest.class);

    public static BufferedInputStream openByteStream(Class< ? > referenceClass, String resourceFileName)
            throws IOException {
        URL url = FileResource.find(referenceClass, resourceFileName);
        if (url != null) {
            return new BufferedInputStream(url.openStream());
        }
        else
            return null;
    }

    public static <T> String loadIntoString(Class<T> referenceClass, String path) throws IOException {
        BufferedInputStream in = openByteStream(referenceClass, path);
        if (in == null)
            throw new FileNotFoundException(
                    String.format("reference class:%s, path:%s", referenceClass.getName(), path));

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            byte[] tmp = new byte[4028];
            for (int readBytes = 0; (readBytes = in.read(tmp)) != -1;) {
                buf.write(tmp, 0, readBytes);
            }
            buf.flush();
            return buf.toString();
        }
        finally {
            in.close();
        }
    }

    @Test
    public void loadSnappyByDiffentClassloadersInTheSameJVM() throws Exception {

        // Parent class loader cannot see Snappy.class
        ClassLoader parent = this.getClass().getClassLoader().getParent();
        ClassWorld cw = new ClassWorld();
        ClassRealm P = cw.newRealm("P", parent);
        try {
            P.loadClass("org.xerial.snappy.Snappy");
            fail("org.xerial.snappy.Snappy is found in the parent");
        }
        catch (ClassNotFoundException e) {
            // OK
        }

        // Prepare the child class loaders which can load Snappy.class
        URL classPath = new File("target/classes").toURI().toURL();
        ClassRealm L1 = cw.newRealm("l1", URLClassLoader.newInstance(new URL[] { classPath }, parent));
        ClassRealm L2 = cw.newRealm("l2", URLClassLoader.newInstance(new URL[] { classPath }, parent));

        // Actually load Snappy.class in a child class loader

        Class< ? > S1 = L1.loadClass("org.xerial.snappy.Snappy");
        Method m = S1.getMethod("compress", String.class);
        byte[] v = (byte[]) m.invoke(null, "hello world");

        // Load Snappy.class from another child class loader
        Class< ? > S2 = L2.loadClass("org.xerial.snappy.Snappy");
        Method m2 = S2.getMethod("compress", String.class);
        byte[] v2 = (byte[]) m2.invoke(null, "hello world");

        assertArrayEquals(v, v2);
    }

    @Test
    public void load() throws Exception {
        SnappyLoader.load();
        _logger.debug(Snappy.maxCompressedLength(1024));
    }

    @Test
    public void autoLoad() throws Exception {
        _logger.debug(Snappy.maxCompressedLength(1024));
    }

    public static void main(String[] args) {
        // Test for loading native library specified in -Djava.library.path
        System.setProperty(SnappyLoader.KEY_SNAPPY_USE_SYSTEMLIB, "true");
        _logger.debug(Snappy.maxCompressedLength(1024));
    }
}

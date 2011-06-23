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

import java.io.File;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Ignore;
import org.junit.Test;
import org.xerial.util.log.Logger;

public class SnappyLoaderTest
{
    private static Logger _logger = Logger.getLogger(SnappyLoaderTest.class);

    @Ignore
    @Test
    public void loadFromSytemClassLoader() throws Exception {

        ClassLoader parent = this.getClass().getClassLoader().getParent();
        ClassWorld cw = new ClassWorld();
        ClassRealm L1 = cw.newRealm("l1", parent);
        ClassRealm L2 = cw.newRealm("l2", parent);

        File nativeLib = SnappyLoader.findNativeLibrary();
        assertNotNull(nativeLib);

        ClassPool pool = ClassPool.getDefault();
        CtClass cl = pool.makeClass("org.xerial.snappy.SnappyNativeLoader");
        cl.addField(CtField.make("static boolean isLoaded = false;", cl));
        String body = "public static void load(String lib) {if(!isLoaded) { try { System.load(lib); isLoaded=true; } catch(Exception e) { e.printStackTrace(); } }}";
        cl.addMethod(CtNewMethod.make(body, cl));

        ProtectionDomain systemPD = System.class.getProtectionDomain();
        byte[] bytecode = cl.toBytecode();
        //        FileOutputStream f = new FileOutputStream("src/main/java/org/xerial/snappy/SnappyNativeLoader.bytecode");
        //        f.write(bytecode);
        //        f.close();

        //Class< ? > loaderClass = cl.toClass(parent, System.class.getProtectionDomain());
        //_logger.info(cl.getName());
        //Class< ? > loaderClass = cl.toClass();

        Class< ? > classLoader = Class.forName("java.lang.ClassLoader");
        java.lang.reflect.Method defineClass = classLoader.getDeclaredMethod("defineClass", new Class[] { String.class,
                byte[].class, int.class, int.class, ProtectionDomain.class });

        defineClass.setAccessible(true);
        defineClass.invoke(parent, cl.getName(), bytecode, 0, bytecode.length, System.class.getProtectionDomain());

        Class< ? > forName = parent.loadClass("org.xerial.snappy.SnappyNativeLoader");
        _logger.info(forName.toString());

        //Class< ? > snappyClass = L1.loadClass("org.xerial.snappy.Snappy");    // not found
        //_logger.info(snappyClass.getName());

    }

    @Test
    public void load() throws Exception {
        SnappyLoader.load();
        _logger.debug(Snappy.getNativeLibraryVersion());
    }
}

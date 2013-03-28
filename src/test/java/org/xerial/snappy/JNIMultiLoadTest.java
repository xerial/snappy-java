package org.xerial.snappy;

import org.codehaus.classworlds.ClassRealmAdapter;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Taro L. Saito
 */
public class JNIMultiLoadTest {

    @Test
    public void jniMultiLoadTest() throws Exception{

        if(!(OSInfo.getOSName().equals("Mac") && OSInfo.getArchName().equals("x86_64"))) {
            return;
        }

        // Create a parent class loader
        ClassLoader parent = this.getClass().getClassLoader().getParent();
        ClassWorld cw = new ClassWorld();
        ClassRealm P = cw.newRealm("P", parent);

        // Prepare the child class loaders which can load Snappy.class
        URL classPath = new File("target/classes").toURI().toURL();
        ClassRealmAdapter L1 = ClassRealmAdapter.getInstance(cw.newRealm("l1", URLClassLoader.newInstance(new URL[]{classPath}, parent)));
        ClassRealmAdapter L2 = ClassRealmAdapter.getInstance(cw.newRealm("l2", URLClassLoader.newInstance(new URL[] { classPath }, parent)));

        // Actually load Snappy.class in a child class loader
        File lib = new File("target/test-classes/org/xerial/snappy/libsnappyjava-1.0.4-mac.jnilib");
        File lib2 = new File("target/test-classes/org/xerial/snappy/libsnappyjava-1.1.0-mac.jnilib");
        loadSnappy(L1, lib);
        loadSnappy(L2, lib2);

    }

    void loadSnappy(ClassRealmAdapter c, File nativeLib) throws Exception {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(c.getClassLoader());
            Class runTimeClass = c.loadClass("java.lang.ClassLoader");
            Method loadM = runTimeClass.getDeclaredMethod("loadLibrary", Class.class, String.class, Boolean.TYPE);
            loadM.setAccessible(true);
            Class loaderClass = c.loadClass("org.xerial.snappy.SnappyLoader");
            loadM.invoke(Runtime.getRuntime(), loaderClass, nativeLib.getAbsolutePath(), true);

            Class nativeClass = c.loadClass("org.xerial.snappy.SnappyNative");
            Object nc = nativeClass.newInstance();
            Method versionGetter = nativeClass.getDeclaredMethod("nativeLibraryVersion");
            versionGetter.setAccessible(true);
            System.out.println(versionGetter.invoke(nc));
        }
        catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        }
        finally {
            Thread.currentThread().setContextClassLoader(current);
        }

    }

}

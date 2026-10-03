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
// snappy-java Project
//
// OSInfoTest.java
// Since: May 20, 2008
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;

import org.junit.Test;

public class OSInfoTest
{
    @Test
    public void osName()
    {
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows XP"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 2000"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows Vista"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 98"));
        assertEquals("Windows", OSInfo.translateOSNameToFolderName("Windows 95"));

        assertEquals("Mac", OSInfo.translateOSNameToFolderName("Mac OS"));
        assertEquals("Mac", OSInfo.translateOSNameToFolderName("Mac OS X"));

        assertEquals("AIX", OSInfo.translateOSNameToFolderName("AIX"));

        assertEquals("Linux", OSInfo.translateOSNameToFolderName("Linux"));
        assertEquals("OS2", OSInfo.translateOSNameToFolderName("OS2"));

        assertEquals("HPUX", OSInfo.translateOSNameToFolderName("HP UX"));
    }

    @Test
    public void archName()
    {
        assertEquals("i386", OSInfo.translateArchNameToFolderName("i386"));
        assertEquals("x86", OSInfo.translateArchNameToFolderName("x86"));
        assertEquals("ppc", OSInfo.translateArchNameToFolderName("ppc"));
        assertEquals("amd64", OSInfo.translateArchNameToFolderName("amd64"));
    }

    @Test
    public void folderPath()
    {
        String[] component = OSInfo.getNativeLibFolderPathForCurrentOS().split("/");
        assertEquals(2, component.length);
        assertEquals(OSInfo.getOSName(), component[0]);
        assertEquals(OSInfo.getArchName(), component[1]);
    }

    @Test
    public void testMainForOSName()
            throws Exception
    {

        // preserve the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--os"});
            assertEquals(OSInfo.getOSName(), buf.toString());
        }
        finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    @Test
    public void testMainForArchName()
            throws Exception
    {

        // preserver the current System.out
        PrintStream out = System.out;
        try {
            // switch STDOUT
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream tmpOut = new PrintStream(buf);
            System.setOut(tmpOut);
            OSInfo.main(new String[] {"--arch"});
            assertEquals(OSInfo.getArchName(), buf.toString());
        }
        finally {
            // reset STDOUT
            System.setOut(out);
        }
    }

    private static File writeMaps(String content)
            throws Exception
    {
        File f = File.createTempFile("snappy-maps", ".txt");
        f.deleteOnExit();
        FileWriter w = new FileWriter(f);
        try {
            w.write(content);
        }
        finally {
            w.close();
        }
        return f;
    }

    // https://github.com/xerial/snappy-java/issues/692
    @Test
    public void detectMuslFromProcessMaps()
            throws Exception
    {
        File musl = writeMaps("7f0000000000-7f0000001000 r-xp 00000000 08:01 123 /lib/ld-musl-x86_64.so.1\n");
        assertTrue(OSInfo.isMuslLoaded(musl.getPath()));

        // glibc process on a system that also has musl installed
        File glibc = writeMaps("7f0000000000-7f0000001000 r-xp 00000000 08:01 123 /usr/lib/x86_64-linux-gnu/libc.so.6\n"
                + "7f0000002000-7f0000003000 r-xp 00000000 08:01 456 /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2\n");
        assertFalse(OSInfo.isMuslLoaded(glibc.getPath()));

        assertFalse(OSInfo.isMuslLoaded("/non-existent/maps"));
    }
}

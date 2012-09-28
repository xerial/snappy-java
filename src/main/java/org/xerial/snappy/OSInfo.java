/*--------------------------------------------------------------------------
 *  Copyright 2008 Taro L. Saito
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
// OSInfo.java
// Since: May 20, 2008
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;

/**
 * Provides OS name and architecture name.
 * 
 * @author leo
 * 
 */
public class OSInfo
{
    public static void main(String[] args) {
        if (args.length >= 1) {
            if ("--os".equals(args[0])) {
                System.out.print(getOSName());
                return;
            }
            else if ("--arch".equals(args[0])) {
                System.out.print(getArchName());
                return;
            }
        }

        System.out.print(getNativeLibFolderPathForCurrentOS());
    }

    public static String getNativeLibFolderPathForCurrentOS() {
        return getOSName() + "/" + getArchName();
    }

    public static String getOSName() {
        return translateOSNameToFolderName(System.getProperty("os.name"));
    }

    public static String getArchName() {
        // if running Linux on ARM, need to determine ABI of JVM
        String osArch = System.getProperty("os.arch");
        if (osArch.startsWith("arm") && System.getProperty("os.name").contains("Linux")) {
            String javaHome = System.getProperty("java.home");
            try {
                // determine if first JVM found uses ARM hard-float ABI
                String[] cmdarray = { "/bin/sh", "-c", "find '" + javaHome +
                    "' -name 'libjvm.so' | head -1 | xargs readelf -A | " +
                    "grep 'Tag_ABI_VFP_args: VFP registers'" };
                int exitCode = Runtime.getRuntime().exec(cmdarray).waitFor();
                if (exitCode == 0)
                    return "armhf";
            }
            catch (IOException e) {
                // ignored: fall back to "arm" arch (soft-float ABI)
            }
            catch (InterruptedException e) {
                // ignored: fall back to "arm" arch (soft-float ABI)
            }
        } 
        else if(getOSName().equals("Mac") && (osArch.equals("universal") || osArch.equals("amd64"))) {
            return "x86_64"; // Fix for openjdk7
        }
        return translateArchNameToFolderName(osArch);
    }

    static String translateOSNameToFolderName(String osName) {
        if (osName.contains("Windows")) {
            return "Windows";
        }
        else if (osName.contains("Mac")) {
            return "Mac";
        }
        else if (osName.contains("Linux")) {
            return "Linux";
        }
        else {
            return osName.replaceAll("\\W", "");
        }
    }

    static String translateArchNameToFolderName(String archName) {
        return archName.replaceAll("\\W", "");
    }
}

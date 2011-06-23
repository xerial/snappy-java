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

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Test;
import org.xerial.util.log.Logger;

public class SnappyLoaderTest
{
    private static Logger _logger = Logger.getLogger(SnappyLoaderTest.class);

    @Test
    public void loadFromSytemClassLoader() throws Exception {

        ClassLoader parent = this.getClass().getClassLoader().getParent();
        ClassWorld cw = new ClassWorld();
        ClassRealm L1 = cw.newRealm("l1", parent);
        ClassRealm L2 = cw.newRealm("l2", parent);

        //Class< ? > snappyClass = L1.loadClass("org.xerial.snappy.Snappy");    // not found
        //_logger.info(snappyClass.getName());

    }
}

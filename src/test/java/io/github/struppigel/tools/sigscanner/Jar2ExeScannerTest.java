/*******************************************************************************
 * Copyright 2014 Katja Hahn
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.github.struppigel.tools.sigscanner;

import io.github.struppigel.TestreportsReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class Jar2ExeScannerTest {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager
            .getLogger(Jar2ExeScannerTest.class.getName());

    @Test
    public void scanResultTest() {
        Jar2ExeScanner scanner = new Jar2ExeScanner(new File(
                TestreportsReader.RESOURCE_DIR + "/testfiles/launch4jwrapped.exe"));
        List<MatchedSignature> result = scanner.scan();
        assertTrue(contains(result, "[Launch4j]"));
    }

    private boolean contains(List<MatchedSignature> siglist, String name) {
        for (MatchedSignature sig : siglist) {
            if (sig.getName().equals(name))
                return true;
        }
        return false;
    }
}

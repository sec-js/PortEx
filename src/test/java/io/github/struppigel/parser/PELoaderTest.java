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
package io.github.struppigel.parser;

import io.github.struppigel.TestreportsReader;
import io.github.struppigel.TestreportsReader.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PELoaderTest {
    private static final Logger logger = LogManager.getLogger(PELoaderTest.class
            .getName());
    private static List<TestData> testData;
    private static final Map<String, PEData> peData = new HashMap<>();

    @BeforeSuite(alwaysRun = true)
    public static void loadPE() throws IOException {
        File[] testfiles = TestreportsReader.getTestiles();
        for (File file : testfiles) {
            logger.info("testfile " + file.getAbsolutePath());
            peData.put(file.getName(), PELoader.loadPE(file));
        }
        testData = TestreportsReader.readTestDataList();
        logger.info("Testdata size: " + testData.size());
    }

    public static List<TestData> getTestData() throws IOException {
        if (testData == null) {
            loadPE();
        }
        return testData;
    }

    public static Map<String, PEData> getPEData() throws IOException {
        if (peData.size() == 0) {
            loadPE();
        }
        return peData;
    }

    @Test
    public void ableToParse() throws IOException {
        String[] filenames = {"d_tiny.dll", "d_resource.dll", "d_nonnull.dll"};
        for (String filename : filenames) {
            File testfile = new File(TestreportsReader.RESOURCE_DIR + "/corkami/" + filename);
            PELoader.loadPE(testfile);
        }
    }
}

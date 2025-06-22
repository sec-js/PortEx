package io.github.struppigel.parser.sections.debug;

import io.github.struppigel.parser.PEData;
import io.github.struppigel.parser.PELoaderTest;
import io.github.struppigel.parser.sections.SectionLoader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.struppigel.parser.sections.debug.DebugDirectoryKey.*;
import static org.testng.Assert.*;

public class DebugSectionTest {

    private Map<String, PEData> pedata;

    @BeforeClass
    public void prepare() throws IOException {
        pedata = PELoaderTest.getPEData();
    }

    @Test
    public void basicWorkingTest() throws IOException {
        PEData datum = pedata.get("strings.exe");
        DebugSection debugSec = new SectionLoader(datum).loadDebugSection();
        List<DebugDirectoryEntry> filteredEntries = debugSec.getEntries().stream().filter(d -> d.getDebugType() == DebugType.CODEVIEW).collect(Collectors.toList());
        DebugDirectoryEntry debug = filteredEntries.get(0);
        assertEquals((long) debug.get(MAJOR_VERSION), 0L);
        assertEquals((long) debug.get(MINOR_VERSION), 0L);
        assertEquals((long) debug.get(ADDR_OF_RAW_DATA), 323836L);
        assertEquals((long) debug.get(SIZE_OF_DATA), 71L);
        assertEquals((long) debug.get(POINTER_TO_RAW_DATA), 317180L);
        assertEquals((long) debug.get(CHARACTERISTICS), 0L);
        assertEquals(debug.getTypeDescription(), "Visual C++ debug information");
        assertEquals(debug.getDebugType(), DebugType.CODEVIEW);
    }
    @Test
    public void extendedDllCharacteristicsTest() {
        // has extended DLL characteristics
        PEData datum = pedata.get("TestCetCompatAndEhCont.exe");
        Optional<ExtendedDLLCharacteristics> exDll = datum.loadExtendedDllCharacteristics();
        assertTrue(exDll.isPresent());
        assertTrue(exDll.get().getCETCompat());
        assertFalse(exDll.get().getForwardCFICompat());
        // has no extended DLL characteristics
        PEData noExDll = pedata.get("upx.exe");
        assertFalse(noExDll.loadExtendedDllCharacteristics().isPresent());
    }

    @Test
    public void invalidCodeView() {
        // this file led to a crash
        PEData pe = pedata.get("invalidCodeView");
        Optional<CodeviewInfo> cv = pe.loadCodeViewInfo();
        assertFalse(cv.isPresent());
    }
}

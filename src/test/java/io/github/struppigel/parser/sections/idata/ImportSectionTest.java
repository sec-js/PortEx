package io.github.struppigel.parser.sections.idata;

import io.github.struppigel.TestreportsReader;
import io.github.struppigel.parser.FileFormatException;
import io.github.struppigel.parser.PEData;
import io.github.struppigel.parser.PELoader;
import io.github.struppigel.parser.PELoaderTest;
import io.github.struppigel.parser.optheader.WindowsEntryKey;
import io.github.struppigel.parser.sections.SectionLoader;
import io.github.struppigel.parser.sections.SectionLoaderTest;
import com.google.common.base.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ImportSectionTest {

    private static final Logger logger = LogManager
            .getLogger(SectionLoaderTest.class.getName());
    private Map<String, PEData> pedata = new HashMap<>();
    private Map<File, List<ImportDLL>> imports;

    @BeforeClass
    public void prepare() throws IOException {
        imports = TestreportsReader.readImportEntries();
        pedata = PELoaderTest.getPEData();
    }

    @Test
    public void virtualImportDescriptor() throws IOException {
        PEData data = PELoader.loadPE(new File(TestreportsReader.RESOURCE_DIR
                + "/corkami/imports_virtdesc.exe"));
        logger.info(data.getInfo());
        ImportSection idata = new SectionLoader(data).loadImportSection();
        List<ImportDLL> imports = idata.getImports();
        assertEquals(imports.size(), 2);
    }

    @Test
    public void getImports() throws FileFormatException, IOException {
        for (Entry<File, List<ImportDLL>> list : imports.entrySet()) {
            PEData pedatum = pedata.get(list.getKey().getName()
                    .replace(".txt", ""));
            if (pedatum != null) {
                long imageBase = pedatum.getOptionalHeader().get(
                        WindowsEntryKey.IMAGE_BASE);
                SectionLoader loader = new SectionLoader(pedatum);
                Optional<ImportSection> idata = loader.maybeLoadImportSection();
                if (!idata.isPresent()) {
                    for (ImportDLL im : list.getValue()) {
                        logger.info("Import test on " + im);
                    }
                    assertEquals(list.getValue().size(), 0);
                } else {
                    List<ImportDLL> readImports = idata.get().getImports();
                    List<ImportDLL> pefileImports = substractImageBase(
                            list.getValue(), pedatum);
                    for (ImportDLL readDLL : readImports) {
                        ImportDLL peFileDLL = find(pefileImports, readDLL);
                        assertEqualImportDLL(peFileDLL, readDLL, imageBase);
                    }
                }
            }
        }
    }

    // Patches the expected list to match our RVA that has not the image base
    // added
    private List<ImportDLL> substractImageBase(List<ImportDLL> expected,
            PEData datum) {
        List<ImportDLL> list = new ArrayList<>();
        long imageBase = datum.getOptionalHeader().get(
                WindowsEntryKey.IMAGE_BASE);
        for (ImportDLL dll : expected) {
            ImportDLL newDLL = new ImportDLL(dll.getName(), dll.getTimeDateStamp());
            for (NameImport nameImport : dll.getNameImports()) {
                long rva = nameImport.getRVA() - imageBase;
                NameImport newImport = new NameImport(rva, 0L, nameImport.getName(),
                        nameImport.getHint(), nameImport.getNameRVA(), null,
                        nameImport.getLocations());
                newDLL.add(newImport);
            }
            for (OrdinalImport ordImport : dll.getOrdinalImports()) {
                long rva = ordImport.getRVA() - imageBase;
                OrdinalImport newImport = new OrdinalImport(ordImport.getOrdinal(),
                        rva, 0L, null, ordImport.getLocations());
                newDLL.add(newImport);
            }
            list.add(newDLL);
        }
        return list;
    }

    private void assertEqualImportDLL(ImportDLL peFileDLL, ImportDLL actualDLL,
            long imageBase) {
        if (peFileDLL != null) {
            assertNotNull(actualDLL);
        }
        assertEquals(peFileDLL.getName(), actualDLL.getName());
        for (NameImport readImport : actualDLL.getNameImports()) {
            NameImport found = find(peFileDLL.getNameImports(), readImport);
            if (found == null) {
                logger.error("unable to find import " + readImport);
                logger.error("image base used: " + imageBase);
                logger.info("dll list: " + peFileDLL);
            }
            assertNotNull(found);
        }
        for (OrdinalImport readImport : actualDLL.getOrdinalImports()) {
            OrdinalImport found = find(peFileDLL.getOrdinalImports(),
                    readImport);
            if (found == null) {
                logger.error("unable to find import " + readImport);
                logger.error("image base used: " + imageBase);
                logger.error("pefile size: "
                        + peFileDLL.getOrdinalImports().size());
                logger.error("actual size: "
                        + actualDLL.getOrdinalImports().size());
                logger.info("pefile dll list: " + peFileDLL);
                logger.info("portex dll list: " + actualDLL);
            }
            assertNotNull(found);
        }

        int actualSize = actualDLL.getNameImports().size()
                + actualDLL.getOrdinalImports().size();
        int expectedSize = peFileDLL.getNameImports().size()
                + peFileDLL.getOrdinalImports().size();
        assertEquals(actualSize, expectedSize);
    }

    private OrdinalImport find(List<OrdinalImport> ordinalImports,
            OrdinalImport readImport) {
        long iat = readImport
                .getDirEntryValue(DirectoryEntryKey.I_ADDR_TABLE_RVA);
        long ilt = readImport
                .getDirEntryValue(DirectoryEntryKey.I_LOOKUP_TABLE_RVA);
        if (ilt == 0)
            ilt = iat;
        for (OrdinalImport pefileImport : ordinalImports) {
            long rva1 = pefileImport.getRVA() - iat;
            long rva2 = readImport.getRVA() - ilt;
            logger.info("rva1: " + rva1 + " rva2: " + rva2);
            logger.info("pefileImport ord: " + pefileImport.getOrdinal());
            logger.info("readImport ord: " + readImport.getOrdinal());
            // second condition to work with lowalignment mode
            if (pefileImport.getOrdinal() == readImport.getOrdinal() && rva1 == rva2
                    || pefileImport.toString().equals(readImport.toString())) {
                return pefileImport;
            }
        }
        return null;
    }

    private NameImport find(List<NameImport> pefileList, NameImport readImport) {
        long iat = readImport
                .getDirEntryValue(DirectoryEntryKey.I_ADDR_TABLE_RVA);
        long ilt = readImport
                .getDirEntryValue(DirectoryEntryKey.I_LOOKUP_TABLE_RVA);
        if (ilt == 0) {
            ilt = iat;
        }
        for (NameImport pefileImport : pefileList) {
            long rva1 = pefileImport.getRVA() - iat;
            long rva2 = readImport.getRVA() - ilt;
            if (pefileImport.getName().equals(readImport.getName()) && rva1 == rva2) {
                return pefileImport;
            }
        }
        return null;
    }

    private ImportDLL find(List<ImportDLL> imports, ImportDLL dll) {
        for (ImportDLL idll : imports) {
            if (dll.getName().equals(idll.getName())) {
                return idll;
            }
        }
        return null;
    }
}

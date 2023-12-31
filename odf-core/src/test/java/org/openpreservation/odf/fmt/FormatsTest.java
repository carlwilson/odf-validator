package org.openpreservation.odf.fmt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class FormatsTest {
    private static final String MIME_ZIP = "application/zip";
    private static final String MIME_XML = "text/xml";
    private static final String MIME_ODS = "application/vnd.oasis.opendocument.spreadsheet";
    private static final String MIME_OTS = "application/vnd.oasis.opendocument.spreadsheet-template";
    private static final String MIME_UNK = "application/octet-stream";
    private static final String EXT_ZIP = "zip";
    private static final String EXT_XML = "xml";
    private static final String EXT_ODS = "ods";
    private static final String EXT_OTS = "ots";
    private static final String EXT_UNK = "";

    @Test
    public void testFromMimeNull() {
        assertThrows("NullPointerException expected",
                NullPointerException.class,
                () -> {
                    Formats.fromMime(null);
                });
    }

    @Test
    public void testIsOdf() {
        assertFalse(Formats.fromMime(MIME_ZIP).isOdf());
        assertTrue(Formats.fromMime(MIME_XML).isOdf());
        assertTrue(Formats.fromMime(MIME_ODS).isOdf());
        assertTrue(Formats.fromMime(MIME_OTS).isOdf());
        assertFalse(Formats.fromMime(MIME_UNK).isOdf());
    }

    @Test
    public void testFromMimeEmpty() {
        assertEquals(String.format("%s IS the MIME identifier for %s", "", Formats.UNKNOWN), Formats.UNKNOWN,
                Formats.fromMime(""));
    }

    @Test
    public void testFromMimeZip() {
        assertEquals(String.format("%s IS the MIME identifier for %s", MIME_ZIP, Formats.ZIP), Formats.ZIP,
                Formats.fromMime(MIME_ZIP));
    }

    @Test
    public void testFromMimeXml() {
        assertEquals(String.format("%s IS the MIME identifier for %s", MIME_XML, Formats.XML), Formats.XML,
                Formats.fromMime(MIME_XML));
    }

    @Test
    public void testFromMimeOds() {
        assertEquals(String.format("%s IS the MIME identifier for %s", MIME_ODS, Formats.ODS), Formats.ODS,
                Formats.fromMime(MIME_ODS));
    }

    @Test
    public void testFromMimeOts() {
        assertEquals(String.format("%s IS the MIME identifier for %s", MIME_OTS, Formats.OTS),
                Formats.OTS,
                Formats.fromMime(MIME_OTS));
    }

    @Test
    public void testFromMimeUnk() {
        assertEquals(String.format("%s IS the MIME identifier for %s", MIME_UNK, Formats.UNKNOWN),
                Formats.UNKNOWN,
                Formats.fromMime(MIME_UNK));
    }

    @Test
    public void testFromExtNull() {
        assertThrows("NullPointerException expected",
                NullPointerException.class,
                () -> {
                    Formats.fromExtension(null);
                });
    }

    @Test
    public void testFromExtEmpty() {
        assertEquals(String.format("%s IS the extension for %s", "", Formats.UNKNOWN), Formats.UNKNOWN,
                Formats.fromExtension(""));
    }

    @Test
    public void testFromExtNotKey() {
        assertEquals(String.format("%s IS the extension for %s", "notkey", Formats.UNKNOWN), Formats.UNKNOWN,
                Formats.fromExtension("notkey"));
    }

    @Test
    public void testFromExtZip() {
        assertEquals(String.format("%s IS the MIME identifier for %s", EXT_ZIP, Formats.ZIP), Formats.ZIP,
                Formats.fromExtension(EXT_ZIP));
    }

    @Test
    public void testFromExtXml() {
        assertEquals(String.format("%s IS the MIME identifier for %s", EXT_XML, Formats.XML), Formats.XML,
                Formats.fromExtension(EXT_XML));
    }

    @Test
    public void testFromExtOds() {
        assertEquals(String.format("%s IS the MIME identifier for %s", EXT_ODS, Formats.ODS), Formats.ODS,
                Formats.fromExtension(EXT_ODS));
    }

    @Test
    public void testFromExtOts() {
        assertEquals(String.format("%s IS the MIME identifier for %s", EXT_OTS, Formats.OTS),
                Formats.OTS,
                Formats.fromExtension(EXT_OTS));
    }

    @Test
    public void testFromExtUnk() {
        assertEquals(String.format("%s IS the MIME identifier for %s", EXT_UNK, Formats.UNKNOWN),
                Formats.UNKNOWN,
                Formats.fromMime(EXT_UNK));
    }

    @Test
    public void testIsDocument() {
        assertFalse(Formats.ZIP.isOdf());
        assertTrue(Formats.XML.isOdf());
        assertTrue(Formats.OTS.isOdf());
        assertTrue(Formats.ODS.isOdf());
    }

    @Test
    public void testMimeGetBytes() {
        for (final Formats fmt : Formats.values()) {
            assertArrayEquals(
                    String.format("MimeTypes %s should return %s", fmt.mime,
                            fmt.mime.getBytes(StandardCharsets.US_ASCII)),
                    fmt.mime.getBytes(StandardCharsets.US_ASCII), fmt.getBytes());
            assertArrayEquals(
                    String.format("MimeTypes %s should return %s", fmt.mime,
                            fmt.mime.getBytes(StandardCharsets.UTF_8)),
                    fmt.mime.getBytes(StandardCharsets.UTF_8), fmt.getBytes());
        }
    }
}

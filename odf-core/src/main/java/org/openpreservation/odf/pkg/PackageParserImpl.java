package org.openpreservation.odf.pkg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.openpreservation.format.xml.ParseResult;
import org.openpreservation.format.xml.XmlParser;
import org.openpreservation.format.zip.ZipArchiveCache;
import org.openpreservation.format.zip.ZipEntry;
import org.openpreservation.format.zip.Zips;
import org.openpreservation.odf.fmt.FormatSniffer;
import org.openpreservation.odf.fmt.Formats;
import org.openpreservation.utils.Checks;
import org.xml.sax.SAXException;

final class PackageParserImpl implements PackageParser {
    private static String toParseConst = "toParse";
    private final XmlParser checker;
    private String mimetype = "";
    private ZipArchiveCache cache;
    private final Map<String, ParseResult> parseResults = new HashMap<>();
    private Manifest manifest = null;
    private Metadata metadata = null;

    private PackageParserImpl()
            throws ParserConfigurationException, SAXException {
        super();
        this.checker = new XmlParser();
    }

    public static final PackageParser getInstance()
            throws ParserConfigurationException, SAXException {
        return new PackageParserImpl();
    }

    @Override
    public OdfPackage parsePackage(final Path toParse) throws IOException {
        Objects.requireNonNull(toParse, String.format(Checks.NOT_NULL, toParseConst, "Path"));
        return parsePackage(toParse, toParse.getFileName().toString());
    }

    private final Formats sniff(Path toSniff) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(toSniff))) {
            return FormatSniffer.sniff(bis);
        }
    }

    private final OdfPackage parsePackage(final Path toParse, final String name) throws IOException {
        this.mimetype = "";
        this.parseResults.clear();
        this.manifest = null;
        this.metadata = null;
        final Formats format = sniff(toParse);
        this.cache = Zips.zipArchiveCacheInstance(toParse);
        for (final ZipEntry entry : this.cache.getZipEntries()) {
            try {
                processEntry(entry);
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException(e);
            }
        }
        return OdfPackageImpl.Builder.builder().name(name).archive(this.cache).format(format).mimetype(mimetype)
                .parseResults(parseResults).manifest(manifest).metadata(metadata).build();
    }

    @Override
    public OdfPackage parsePackage(final File toParse) throws IOException {
        Objects.requireNonNull(toParse, String.format(Checks.NOT_NULL, toParseConst, "File"));
        return parsePackage(toParse.toPath(), toParse.getName());
    }

    @Override
    public OdfPackage parsePackage(final InputStream toParse, final String name) throws IOException {
        Objects.requireNonNull(toParse, String.format(Checks.NOT_NULL, toParseConst, "InputStream"));
        Objects.requireNonNull(name, String.format(Checks.NOT_NULL, name, "String"));
        try (BufferedInputStream bis = new BufferedInputStream(toParse)) {
            Path temp = Files.createTempFile("odf", ".pkg");
            Files.copy(bis, temp, StandardCopyOption.REPLACE_EXISTING);
            return this.parsePackage(temp, name);
        }
    }

    private final void processEntry(final ZipEntry entry)
            throws IOException, ParserConfigurationException, SAXException {
        final String path = entry.getName();
        final String name = new File(path).getName();
        if (entry.isDirectory()) {
            return;
        }
        if (path.equals(Constants.MIMETYPE)) {
            this.mimetype = new String(this.cache.getEntryInputStream(entry.getName()).readAllBytes(),
                    StandardCharsets.UTF_8);
        } else if (path.equals(Constants.PATH_MANIFEST)) {
            try (InputStream is = this.cache.getEntryInputStream(entry.getName())) {
                this.parseResults.put(path, this.checker.parse(is));
            }
            if (this.parseResults.get(path).isWellFormed()
                    && this.parseResults.get(path).isRootName("manifest")) {
                this.manifest = ManifestImpl.from(this.cache.getEntryInputStream(entry.getName()));
            }
        } else if (name.equals(Constants.NAME_META)) {
            try (InputStream is = this.cache.getEntryInputStream(entry.getName())) {
                this.parseResults.put(path, this.checker.parse(is));
            }
            if (this.parseResults.get(path).isWellFormed()
                    && this.parseResults.get(path).isRootName("document-meta")) {
                this.metadata = MetadataImpl.from(this.cache.getEntryInputStream(entry.getName()));
            }
        } else if (name.equals(Constants.NAME_CONTENT) || name.equals(Constants.NAME_STYLES)) {
            try (InputStream is = this.cache.getEntryInputStream(entry.getName())) {
                this.parseResults.put(path, this.checker.parse(is));
            }
        }
    }
}

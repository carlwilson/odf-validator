package org.openpreservation.odf.pkg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.openpreservation.format.xml.ParseResult;
import org.openpreservation.format.xml.ValidationResult;
import org.openpreservation.format.xml.XmlValidator;
import org.openpreservation.format.zip.ZipEntry;
import org.openpreservation.messages.Message;
import org.openpreservation.messages.MessageFactory;
import org.openpreservation.messages.Messages;
import org.openpreservation.odf.validation.ValidationReport;
import org.openpreservation.odf.xml.OdfSchemaFactory.Version;
import org.openpreservation.utils.Checks;
import org.xml.sax.SAXException;

final class ValidatingParserImpl implements ValidatingParser {
    private static final String TO_VALIDATE = "toValidate";
    private static final MessageFactory FACTORY = Messages.getInstance();

    static final ValidatingParserImpl getInstance()
            throws ParserConfigurationException, SAXException {
        return new ValidatingParserImpl();
    }

    private final XmlValidator validator;

    private final PackageParser packageParser;

    private final Map<String, ValidationResult> results = new HashMap<>();

    private ValidatingParserImpl()
            throws ParserConfigurationException, SAXException {
        super();
        this.packageParser = OdfPackages.getPackageParser();
        this.validator = new XmlValidator();
    }

    @Override
    public ValidationReport validatePackage(final OdfPackage toValidate) {
        Objects.requireNonNull(toValidate, String.format(Checks.NOT_NULL, TO_VALIDATE, "OdfPackage"));
        return validate(toValidate);
    }

    @Override
    public OdfPackage parsePackage(Path toParse) throws IOException {
        return this.packageParser.parsePackage(toParse);
    }

    @Override
    public OdfPackage parsePackage(File toParse) throws IOException {
        return this.packageParser.parsePackage(toParse);
    }

    @Override
    public OdfPackage parsePackage(InputStream toParse, String name) throws IOException {
        return this.packageParser.parsePackage(toParse, name);
    }

    private ValidationReport validate(final OdfPackage odfPackage) {
        final ValidationReport report = new ValidationReport(odfPackage.getName());
        if (!odfPackage.isWellFormedZip()) {
            report.add(odfPackage.getName(), FACTORY.getError("PKG-9"));
            return report;
        }
        if (!odfPackage.hasMimeEntry()) {
            if (odfPackage.getManifest().getRootMediaType() != null) {
                report.add(Constants.MIMETYPE, FACTORY.getError("PKG-10"));
            } else {
                report.add(Constants.MIMETYPE, FACTORY.getWarning("PKG-2"));
            }
        } else {
            report.add(Constants.MIMETYPE,
                    this.validateMimeEntry(odfPackage.getZipArchive().getZipEntry(Constants.MIMETYPE),
                            odfPackage.getFormat().isOdf()));
            report.add(Constants.MIMETYPE, FACTORY.getInfo("DOC-3", odfPackage.getMimeType()));
        }
        if (!odfPackage.hasManifest()) {
            report.add(Constants.PATH_MANIFEST, FACTORY.getError("PKG-4"));
        } else {
            report.add(Constants.PATH_MANIFEST, this.validateManifest(odfPackage));
        }
        for (final ZipEntry entry : odfPackage.getZipArchive().getZipEntries()) {
            if ((entry.getMethod() != java.util.zip.ZipEntry.STORED)
                    && (entry.getMethod() != java.util.zip.ZipEntry.DEFLATED)) {
                // Entries SHALL be uncompressesed (Stored) or use deflate compression
                report.add(entry.getName(), FACTORY.getError("PKG-1", entry.getName()));
            }
            if (entry.getName().startsWith(Constants.NAME_META_INF)
                    && (entry.isDirectory() && !Constants.NAME_META_INF.equals(entry.getName()))) {
                report.add(entry.getName(), FACTORY.getError("PKG-3", entry.getName()));
            }
        }
        for (final Entry<String, ValidationResult> entry : this.results.entrySet()) {
            report.add(entry.getKey(), entry.getValue().getMessages());
        }
        return report;
    }

    private List<Message> validateMimeEntry(final ZipEntry mimeEntry, final boolean isFirst) {
        final List<Message> messages = new ArrayList<>();
        if (!isFirst) {
            messages.add(FACTORY.getError("PKG-7"));
        }
        if (mimeEntry.getMethod() != java.util.zip.ZipEntry.STORED) {
            messages.add(FACTORY.getError("PKG-6"));
        }
        if (mimeEntry.getExtra() != null && mimeEntry.getExtra().length > 0) {
            messages.add(FACTORY.getError("PKG-8"));
        }
        return messages;
    }

    private List<Message> validateManifest(final OdfPackage odfPackage) {
        final List<Message> messages = new ArrayList<>();
        Manifest manifest = odfPackage.getManifest();
        if (manifest.getRootMediaType() != null && odfPackage.hasMimeEntry()
                && !manifest.getRootMediaType().equals(odfPackage.getMimeType())) {
            messages.add(FACTORY.getError("PKG-12", manifest.getRootMediaType(), odfPackage.getMimeType()));
        } else if (manifest.getRootMediaType() == null && odfPackage.hasMimeEntry()) {
            messages.add(FACTORY.getWarning("PKG-11"));
        }

        ParseResult result = odfPackage.getEntryXmlParseResult(Constants.PATH_MANIFEST);
        messages.addAll(result.getMessages());
        if (result.isWellFormed()) {
            try {
                String manifestVersion = (odfPackage.getManifest().getVersion() != null) ? odfPackage
                        .getManifest().getVersion() : odfPackage.getManifest().getRootVersion();
                ValidationResult validationResult = this.validator.validate(result,
                        odfPackage.getEntryXmlStream(Constants.PATH_MANIFEST), Version.fromVersion(manifestVersion));
                messages.addAll(validationResult.getMessages());
                if (validationResult.isValid()) {
                    messages.addAll(checkManifestEntries(odfPackage));
                }
            } catch (IOException e) {
                messages.add(FACTORY.getError("CORE-3", e.getMessage(), Constants.PATH_MANIFEST));
            }
        }
        return messages;
    }

    private List<Message> checkManifestEntries(final OdfPackage odfPackage) {
        final List<Message> messages = new ArrayList<>();
        for (FileEntry entry : odfPackage.getManifest().getEntries()) {
            if ("/".equals(entry.getFullPath()) || entry.getFullPath().endsWith("/")) {
                continue;
            } else if (entry.getFullPath().equals(Constants.MIMETYPE)) {
                messages.add(FACTORY.getError("PKG-15", entry.getFullPath()));
                continue;
            } else if (entry.getFullPath().equals(Constants.PATH_MANIFEST)) {
                messages.add(FACTORY.getError("PKG-14", entry.getFullPath()));
                continue;
            } else if (entry.getFullPath().startsWith(Constants.NAME_META_INF)) {
                messages.add(FACTORY.getError("PKG-13", entry.getFullPath()));
                continue;
            }
            ZipEntry zipEntry = odfPackage.getZipArchive().getZipEntry(entry.getFullPath());
            if (zipEntry == null) {
                messages.add(FACTORY.getError("PKG-16", entry.getFullPath()));
            }
        }
        for (ZipEntry zipEntry : odfPackage.getZipArchive().getZipEntries()) {
            if (zipEntry.isDirectory() || zipEntry.getName().equals(Constants.MIMETYPE)
                    || zipEntry.getName().equals(Constants.PATH_MANIFEST)
                    || zipEntry.getName().startsWith(Constants.NAME_META_INF)) {
                continue;
            }
            FileEntry fileEntry = odfPackage.getManifest().getEntry(zipEntry.getName());
            if (fileEntry == null) {
                messages.add(FACTORY.getError("PKG-17", zipEntry.getName()));
            }
        }
        return messages;
    }
}

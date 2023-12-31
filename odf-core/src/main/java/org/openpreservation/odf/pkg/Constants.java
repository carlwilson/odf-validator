package org.openpreservation.odf.pkg;

public final class Constants {
    static final String MIMETYPE = "mimetype";
    static final String NAME_META_INF = "META-INF/";
    static final String NAME_MANIFEST = "manifest.xml";
    static final String PATH_MANIFEST = NAME_META_INF + NAME_MANIFEST;
    static final String NAME_CONTENT = "content.xml";
    static final String NAME_STYLES = "styles.xml";
    static final String NAME_META = "meta.xml";
    static final String TAG_MANIFEST = "manifest";

    private Constants() {
        throw new AssertionError("Constants class is not instantiable");
    }
}

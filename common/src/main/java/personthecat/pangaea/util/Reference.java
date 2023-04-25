package personthecat.pangaea.util;

import personthecat.catlib.versioning.Version;

public final class Reference {

    private Reference() {}

    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final Version MOD_VERSION = Version.parse("@MOD_VERSION@");
}

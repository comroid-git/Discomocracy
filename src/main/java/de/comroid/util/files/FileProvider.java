package de.comroid.util.files;

import java.io.File;
import java.io.IOException;

public class FileProvider {
    private final static String UNIX_PREPATH = "/var/bots/btsc/";

    public static File getFile(String subPath) {
        return getFile(subPath, true);
    }

    public static File getFile(String subPath, boolean provide) {
        File file = new File((OSValidator.isUnix() ? UNIX_PREPATH : "") + subPath);

        if (provide && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return file;
    }
}

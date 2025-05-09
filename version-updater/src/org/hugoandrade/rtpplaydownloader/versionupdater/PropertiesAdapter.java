package org.hugoandrade.rtpplaydownloader.versionupdater;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertiesAdapter {

    private final Properties prop;
    private final boolean exists;

    private static PropertiesAdapter instance;

    public static PropertiesAdapter getInstance() {
        if (instance == null) {
            instance = new PropertiesAdapter();
        }
        return instance;
    }

    private PropertiesAdapter() {
        prop = new Properties();

        try {
            InputStream input = new FileInputStream("azure-sql.properties");
            prop.load(input);
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
            exists = false;
            return;
        }
        exists = true;
    }

    public boolean exists() {
        return exists;
    }

    public String getHostName() {
        return prop.getProperty("hostName");
    }

    public String getDatabase() {
        return prop.getProperty("dbName");
    }

    public String getUserSuffix() {
        return prop.getProperty("userSuffix");
    }
}

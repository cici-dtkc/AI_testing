package vn.edu.hcmuaf.fit.ttltw_nhom6.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FacebookConfig {
    private static final Properties prop = new Properties();
    static {
        try(InputStream is = FacebookConfig.class.getClassLoader().getResourceAsStream("facebook.properties")){
            prop.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Không load được facebook.properties", e);
        }
    }
    public static String getAppId()       { return prop.getProperty("facebook.app.id"); }
    public static String getAppSecret()   { return prop.getProperty("facebook.app.secret"); }
    public static String getRedirectUri() { return prop.getProperty("facebook.redirect.uri"); }
}

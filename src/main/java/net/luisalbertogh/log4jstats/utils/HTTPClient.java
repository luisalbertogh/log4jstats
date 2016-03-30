/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class implements a simple HTTP client.
 * 
 * @author lagarcia
 */
public class HTTPClient {

    /**
     * Create a HTTPS connection to the given URL and passing the given parameter.
     * 
     * @param urlStr
     * @param params
     * @return
     * @throws Exception
     */
    public static URLConnection createHTTPSconnection(String urlStr, Map<String, String> params) throws Exception {
        // Sending request
        URL url = new URL(urlStr);
        URLConnection connection = null;
        String protocol = url.getProtocol();
        if (protocol.equalsIgnoreCase("https")) {
            System.setProperty("java.protocol.handler.pkgs", "javax.net.ssl");
            java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            connection = (HttpsURLConnection) url.openConnection();
        } else {
            connection = url.openConnection();
        }

        connection.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        // Encode param
        Set<String> keys = params.keySet();
        for (String key : keys) {
            String encodedParam = URLEncoder.encode(params.get(key), "UTF-8");
            out.write(key + "=" + encodedParam);
        }
        out.close();

        return connection;
    }
}

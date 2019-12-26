package org.hugoandrade.rtpplaydownloader.versionupdater;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TVIPlayerTest {

    private static String getJWIOL() {

        CookieStore httpCookieStore = new BasicCookieStore();

        try {
            String tokenUrl = "https://services.iol.pt/matrix?userId=";
            HttpClient client = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore).build();
            HttpGet get = new HttpGet(tokenUrl);
            HttpResponse response = client.execute(get);

            InputStream inputStream = response.getEntity().getContent();
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }

            return textBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getM3U8ChunkUrl(String url) {

        try {
            Document doc = Jsoup.connect(url).timeout(10000).get();

            Elements scriptElements = doc.getElementsByTag("script");

            if (scriptElements != null) {

                for (Element element : scriptElements) {
                    for (DataNode dataNode : element.dataNodes()) {
                        String scriptText = dataNode.getWholeData();
                        if (scriptText.contains("$('#player').iolplayer({")) {


                            String scriptTextStart = scriptText.substring(indexOfEx(scriptText, "\"videoUrl\":\""));
                            return scriptTextStart.substring(0, scriptTextStart.indexOf("\""));
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static String getM3U8Playlist(String m3u8) {

        try {
            URL chunkListUrl = new URL(m3u8);
            Scanner s = new Scanner(chunkListUrl.openStream());
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("chunklist")) return line;
            }
        }
        catch (Exception ignored) {}
        return null;
    }

    private static List<String> getTSUrls(String playlistUrl) {

        try {
            List<String> tsUrls = new ArrayList<>();

            URL url = new URL(playlistUrl);
            Scanner s = new Scanner(url.openStream());
            while (s.hasNext()) {
                String line = s.next();
                if (!line.startsWith("media")) continue;
                tsUrls.add(line);
            }

            return tsUrls;
        }
        catch (Exception ignored) {}
        return null;
    }

    private static int indexOfEx(String string, String subString) {
        if (string.contains(subString)) {
            return string.indexOf(subString) + subString.length();
        }
        return 0;
    }

    private static void doDownload(List<String> tsUrls) {

        try {
            File f = new File("./test-output-resources/", "tmp.ts");
            FileOutputStream fos = new FileOutputStream(f);

            for (String tsUrl : tsUrls) {
                System.err.println("reading = " + tsUrl);

                URL u = new URL(tsUrl);

                InputStream inputStream = u.openStream();

                byte[] buffer = new byte[1024];
                if (inputStream != null) {
                    int len = inputStream.read(buffer);
                    while (len > 0) {

                        fos.write(buffer, 0, len);
                        len = inputStream.read(buffer);
                    }
                }
            }

            fos.close();

            System.err.println("path = " + f.getAbsolutePath());
            System.err.println("path = " + f.getCanonicalPath());
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static void main(String... args) {

        String url = "https://tviplayer.iol.pt/" +
                "programa/governo-sombra/53c6b3a33004dc006243d5fb/" +
                "video/5dfd75b60cf2853f0740adcb";

        String jwiol = getJWIOL();
        String m3u8Url = getM3U8ChunkUrl(url);
        String baseUrl = m3u8Url.substring(0, m3u8Url.lastIndexOf("/") + 1);
        String m3u8 = m3u8Url + "?wmsAuthSign=" + jwiol;
        String playlist = getM3U8Playlist(m3u8);
        String playlistUrl = baseUrl + playlist;
        List<String> tsUrls = getTSUrls(playlistUrl);
        List<String> tsFullUrls = new ArrayList<>();

        for (String tsUrl : tsUrls) {
            String tsFullUrl = baseUrl + tsUrl;
            System.err.println("tsUrl = " + tsUrl);
            System.err.println("tsUrl = " + tsFullUrl);
            tsFullUrls.add(tsFullUrl);
        }

        System.err.println("jwiol = " + jwiol);
        System.err.println("m3u8Url = " + m3u8Url);
        System.err.println("baseUrl = " + baseUrl);
        System.err.println("m3u8 = " + m3u8);
        System.err.println("playlist = " + playlist);
        System.err.println("playlistUrl = " + playlistUrl);

        doDownload(tsFullUrls);

    }
}

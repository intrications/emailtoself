package uk.co.ashtonbrsc.emailtoself;

import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;

import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

public class WebPageTitleRequest implements Runnable {

        public static final int HANDLE_TITLE = (int) 0xdc9260f7f85ea7b3L;

        private static final String LOG_TAG = "WebPageTitleRequest";

        private final String mUrl;
        private final Handler mHandler;
        private String mTitle;

        public WebPageTitleRequest(String url, Handler handler) {
                super();
                mUrl = url;
                mHandler = handler;
        }

        public void run() {
                try {
                        URL url = new URL(mUrl);
                        URLConnection connection = url.openConnection();
                        InputStream input = connection.getInputStream();
                        try {
                                String encoding = connection.getContentEncoding();
                                if (encoding == null) {
                                        encoding = HTTP.DEFAULT_CONTENT_CHARSET;
                                }
                                Reader reader = new InputStreamReader(input, encoding);
                                mTitle = parseTitle(reader);
                        } finally {
                                input.close();
                        }
                } catch (IOException e) {
                        Log.e(LOG_TAG, "i/o error", e);
                } catch (RuntimeException e) {
                        Log.e(LOG_TAG, "runtime error", e);
                } catch (Error e) {
                        Log.e(LOG_TAG, "severe error", e);
                } finally {
                	if (!Thread.interrupted()) {
//                		Log.d(getClass().getName(), "Thread not interrupted");
                        Message msg = mHandler.obtainMessage(HANDLE_TITLE, mTitle);
                        msg.sendToTarget();
//                	} else {
//                		Log.d(getClass().getName(), "Thread interrupted");
                	}
                }
        }

        static String parseTitle(Reader src) throws IOException {
                BufferedReader reader = new BufferedReader(src);
                StringBuilder builder = null;
                while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                                break;
                        }
                        String lowerCaseLine = line.toLowerCase();
                        if (builder == null) {
                                int start = lowerCaseLine.indexOf("<title>");
                                if (start != -1) {
                                        start += "<title>".length();
                                        int end = lowerCaseLine.indexOf("<", start);
                                        if (end != -1) {
                                                CharSequence title = line.subSequence(start, end);
                                                builder = new StringBuilder(title);
                                                break;
                                        } else {
                                            end = line.length();
                                                CharSequence firstPart = line.subSequence(start, end);
                                                builder = new StringBuilder(firstPart);
                                        }
                                }
                        } else {
                                int end = lowerCaseLine.indexOf("<");
                                if (end != -1) {
                                        builder.append(line, 0, end);
                                        break;
                                } else {
                                        builder.append(line);
                                }
                        }
                        if (lowerCaseLine.indexOf("</head>") != -1
                                        || lowerCaseLine.indexOf("<body") != -1) {
                                // Title did not exist or was malformed. Abandon any
                                // partial data.
                                builder = null;
                                break;
                        }

                }
                if (builder != null) {
                        // Remove escaped characters
                        String source = builder.toString();
                        source = Html.fromHtml(source).toString();
                        source = source.trim();
                        return source;
                } else {
                        return null;
                }
        }
}
package com.rwcode.editor;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView wv;
    private ValueCallback<Uri[]> fileCb;
    private static final int REQ_FILE = 7;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#2B2E32"));
        getWindow().setNavigationBarColor(Color.parseColor("#25282C"));
        wv = new WebView(this);
        wv.setBackgroundColor(Color.parseColor("#1B1D1F"));
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        wv.addJavascriptInterface(new Bridge(), "Android");
        wv.setWebViewClient(new WebViewClient());
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });
        wv.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        } else super.onActivityResult(req, res, data);
    }

    @Override
    public void onBackPressed() {
        // Закрыть панель/поиск внутри страницы, иначе выйти
        wv.evaluateJavascript("(function(){var p=document.querySelector('.panel.on');if(p){p.classList.remove('on');return 1}return 0})()",
                v -> { if ("0".equals(v)) finish(); });
    }

    class Bridge {
        /** Загрузка текста по https с docs.google.com (обход CORS). Ответ — в window[cb](ok, text). */
        @JavascriptInterface
        public void fetchUrl(final String url, final String cb) {
            if (url == null || !url.startsWith("https://docs.google.com/") || cb == null || !cb.matches("\\w+")) return;
            new Thread(() -> {
                boolean ok = true;
                String res;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(25000);
                    c.setInstanceFollowRedirects(true);
                    if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                    res = o.toString("UTF-8");
                } catch (Exception e) {
                    ok = false;
                    res = String.valueOf(e);
                }
                final String js = "window." + cb + "(" + ok + "," + JSONObject.quote(res) + ")";
                runOnUiThread(() -> wv.evaluateJavascript(js, null));
            }).start();
        }

        /** Сохраняет файл в Загрузки/RWCode. Возвращает путь или "ERR:...". */
        @JavascriptInterface
        public String saveFile(String name, String text) {
            try {
                name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                byte[] data = text.getBytes("UTF-8");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RWCode");
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(data); }
                    return "Download/RWCode/" + name;
                }
                File f = new File(getExternalFilesDir(null), name);
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
                return f.getPath();
            } catch (Exception e) {
                return "ERR:" + e;
            }
        }
    }
}

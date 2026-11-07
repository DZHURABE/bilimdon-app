package tj.bilimdon.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/* Иловани GitHub'дан ўзи янгилайди:
   ҳар очилганда (ва ҳар 30 дақиқада) update_url'дан янги index.html'ни текширади,
   янги бўлса сақлайди ва дарҳол (ёки кейинги очилишда) ишлатади.
   Интернет бўлмаса — охирги сақланган нусха ишлайди. */
public class MainActivity extends Activity {
    private static final String BASE = "file:///android_asset/index.html";
    private static final int PICK = 77;
    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private String server, updateUrl;
    private File cache;
    private SharedPreferences pref;
    private long lastCheck = 0;
    private volatile boolean checking = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0F1320"));
        getWindow().setNavigationBarColor(Color.parseColor("#0F1320"));
        server = getString(R.string.server_url).trim().replaceAll("/+$", "");
        updateUrl = getString(R.string.update_url).trim();
        cache = new File(getFilesDir(), "app.html");
        pref = getSharedPreferences("bd", MODE_PRIVATE);
        /* янги APK ўрнатилса — эски кешни ташлаймиз */
        int vc = 0;
        try { vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; } catch (Exception e) { }
        if (pref.getInt("vc", -1) != vc) { cache.delete(); pref.edit().putInt("vc", vc).apply(); }

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0F1320"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return route(r.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) { return route(url); }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame() && !server.isEmpty()) v.loadUrl("file:///android_asset/offline.html");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "Расм / Image"), PICK);
                return true;
            }
        });
        setContentView(web);
        home();
        checkUpdate();
    }

    private void home() {
        if (!server.isEmpty()) { web.loadUrl(server + "/"); return; }
        String html = cache.exists() ? read(cache) : null;
        if (html != null && html.length() > 50000) web.loadDataWithBaseURL(BASE, html, "text/html", "utf-8", null);
        else web.loadUrl(BASE);
    }

    private void checkUpdate() {
        if (!server.isEmpty() || updateUrl.isEmpty() || checking) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck < 30 * 60 * 1000L && lastCheck != 0) return;
        lastCheck = now;
        checking = true;
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(updateUrl + "?t=" + (now / 60000)).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(20000);
                c.setUseCaches(false);
                if (c.getResponseCode() == 200) {
                    String html = new String(readAll(c.getInputStream()), StandardCharsets.UTF_8);
                    String cur = cache.exists() ? read(cache) : readAsset();
                    if (html.length() > 50000 && html.contains("</html>") && !html.equals(cur)) {
                        File tmp = new File(getFilesDir(), "app.tmp");
                        try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(html.getBytes(StandardCharsets.UTF_8)); }
                        tmp.renameTo(cache);
                        runOnUiThread(() -> web.evaluateJavascript(
                            "(function(){try{return (typeof view==='undefined'||view==='home'||view==='admin'||!(me()||isAdmin()))&&!document.querySelector('#sheet.on')?1:0}catch(e){return 1}})()",
                            v -> { if ("1".equals(v)) home(); }));
                    }
                }
                c.disconnect();
            } catch (Exception e) { /* интернет йўқ — эски нусха ишлайверади */ }
            checking = false;
        }).start();
    }

    @Override protected void onResume() { super.onResume(); if (web != null) checkUpdate(); }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[16384]; int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        in.close();
        return o.toByteArray();
    }
    private String read(File f) { try { return new String(readAll(new FileInputStream(f)), StandardCharsets.UTF_8); } catch (Exception e) { return null; } }
    private String readAsset() { try { return new String(readAll(getAssets().open("index.html")), StandardCharsets.UTF_8); } catch (Exception e) { return ""; } }

    /* тўлов (pay.dc.tj), WhatsApp, қўнғироқ — ташқи иловада очилади */
    private boolean route(String url) {
        if (url.startsWith("bd://retry")) { home(); return true; }
        if (url.startsWith("file://") || url.startsWith("about:") || url.startsWith("data:")) return false;
        if (!server.isEmpty() && url.startsWith(server)) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { }
        return true;
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && fileCb != null) {
            Uri[] r = (res == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
            fileCb.onReceiveValue(r);
            fileCb = null;
        }
        super.onActivityResult(req, res, data);
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){try{var s=document.getElementById('sheet');if(s&&s.classList.contains('on')){closeSheet();return 1}if(typeof view!=='undefined'&&view!=='home'&&view!=='admin'){go(isAdmin()?'admin':'home');return 1}}catch(e){}return 0})()",
            v -> { if (!"1".equals(v)) MainActivity.super.onBackPressed(); });
    }
}

package tj.bilimdon.app;

import android.app.Activity;
import android.content.Intent;
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

/* Илова иловани серверингиздан очади: серверга янгилик қўйсангиз,
   ҳамма телефонда кейинги очилишда ўзи янгиланади (APK'ни қайта ўрнатиш шарт эмас). */
public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private static final int PICK = 77;
    private String server;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0F1320"));
        getWindow().setNavigationBarColor(Color.parseColor("#0F1320"));
        server = getString(R.string.server_url).trim().replaceAll("/+$", "");
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0F1320"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return route(r.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) { return route(url); }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) v.loadUrl("file:///android_asset/offline.html");
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
        if (b != null) web.restoreState(b); else home();
    }

    private void home() {
        if (server.isEmpty()) web.loadUrl("file:///android_asset/index.html");
        else web.loadUrl(server + "/");
    }

    /* ўз сервери ичида қолади; тўлов (pay.dc.tj), WhatsApp, қўнғироқ — ташқи иловада очилади */
    private boolean route(String url) {
        if (url.startsWith("bd://retry")) { home(); return true; }
        if (url.startsWith("file://")) return false;
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

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){try{var s=document.getElementById('sheet');if(s&&s.classList.contains('on')){closeSheet();return 1}if(typeof view!=='undefined'&&view!=='home'&&view!=='admin'){go(isAdmin()?'admin':'home');return 1}}catch(e){}return 0})()",
            v -> { if (!"1".equals(v)) MainActivity.super.onBackPressed(); });
    }
}

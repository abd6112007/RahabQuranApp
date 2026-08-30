package com.rahab.quranapp;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

/**
 * غلاف (Wrapper) بسيط يعرض ملف الويب المحلي (index.html) بملء الشاشة
 * دون أي تعديل على محتوى الملف نفسه.
 *
 * يستخدم WebViewAssetLoader بدلاً من file:// مباشرة، لأن:
 *  - يمنح الصفحة أصل (origin) آمن https://appassets.androidplatform.net
 *  - localStorage / IndexedDB / Cache API تعمل بشكل طبيعي وصحيح (بعكس file://)
 *  - يسمح بتسجيل Service Worker إن وجد ملفه لاحقاً
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String ASSET_DOMAIN = "appassets.androidplatform.net";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إبقاء الشاشة مضاءة أثناء القراءة (اختياري، شائع في تطبيقات القرآن)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_DOMAIN)
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);              // localStorage / sessionStorage
        settings.setDatabaseEnabled(true);                // WebSQL/قواعد بيانات قديمة إن استُخدمت
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);  // استخدام الكاش تلقائياً عند عدم الاتصال
        settings.setAllowFileAccess(false);                // غير مطلوب مع AssetLoader
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // تفعيل تصحيح الأخطاء عبر chrome://inspect أثناء التطوير فقط (يمكن حذفه في الإصدار النهائي)
        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // لا نتجاوز أخطاء SSL افتراضياً؛ يمكن تخصيص هذا إذا احتجت شهادات خاصة
                super.onReceivedSslError(view, handler, error);
            }
        });

        // نقطة البداية: ملف index.html الموجود داخل مجلد assets كما هو تماماً
        webView.loadUrl("https://" + ASSET_DOMAIN + "/assets/index.html");
    }

    // دعم زر الرجوع للتنقل داخل تاريخ الصفحة بدل إغلاق التطبيق مباشرة
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

package cz.martykan.webtube;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import com.getbase.floatingactionbutton.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    View appWindow;
    Window window;
    ProgressBar progress;
    View mCustomView;
    FrameLayout customViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        appWindow = findViewById(R.id.appWindow);
        window = this.getWindow();
        progress = (ProgressBar) findViewById(R.id.progress);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        // To save login info
        CookieManager.getInstance().setAcceptCookie(true);
        if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view,CustomViewCallback callback) {
                // if a view already exists then immediately terminate the new one
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                webView.setVisibility(View.GONE);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewContainer.addView(view);

                View decorView = getWindow().getDecorView();
                // Hide the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);

            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                if (mCustomView == null)
                    return;

                webView.setVisibility(View.VISIBLE);
                customViewContainer.setVisibility(View.GONE);

                mCustomView.setVisibility(View.GONE);
                customViewContainer.removeView(mCustomView);
                mCustomView = null;

                View decorView = getWindow().getDecorView();
                // Show the status bar.
                int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                decorView.setSystemUiVisibility(uiOptions);
            }


            public void onProgressChanged(WebView view, int percentage) {
                progress.setVisibility(View.VISIBLE);
                progress.setProgress(percentage);

                // For more advnaced loading
                if(Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {
                    if (percentage == 100) {
                        progress.setIndeterminate(true);
                    } else {
                        progress.setIndeterminate(false);
                    }
                    webView.evaluateJavascript("(function() { return document.getElementsByClassName('_mks')[0] != null; })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("LOADING", value.toString());
                                    if (value.equals("false")) {
                                        progress.setVisibility(View.INVISIBLE);
                                    } else {
                                        onProgressChanged(webView, 100);
                                    }
                                }
                            });
                }
            }


        });

        webView.setWebViewClient(new WebViewClient() {
            // Open links in a browser window (except for sign-in dialogs and YouTube URLs)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("http") && !url.contains("accounts.google.") && !url.contains("youtube.")) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }

            public void onLoadResource(WebView view, String url) {
                // Gets rid of orange outlines (causes bug on jellybean)
                if (Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {
                    String css = "*, *:focus { /*overflow-x: hidden !important;*/ " +
                            "/*transform: translate3d(0,0,0) !important; -webkit-transform: translate3d(0,0,0) !important;*/ outline: none !important; -webkit-tap-highlight-color: rgba(255,255,255,0) !important; -webkit-tap-highlight-color: transparent !important; }";
                    webView.loadUrl("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var style = document.createElement('style');" +
                            "style.type = 'text/css';" +
                            "style.innerHTML = '" + css + "';" +
                            "parent.appendChild(style)" +
                            "})()");
                }

                // To change the statusbar color
                if (Integer.valueOf(Build.VERSION.SDK_INT) >= 21) {
                    webView.evaluateJavascript("(function() { if(document.getElementById('player').style.visibility == 'hidden' || document.getElementById('player').innerHTML == '') { return 'not_video'; } else { return 'video'; } })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                    if (!value.toString().contains("not_video")) {
                                        window.setStatusBarColor(getApplication().getResources().getColor(R.color.colorWatchDark));
                                    } else {
                                        window.setStatusBarColor(getApplication().getResources().getColor(R.color.colorPrimaryDark));
                                    }
                                }
                            });
                }
            }

            // Deal with error messages
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (description.toString().contains("NETWORK_CHANGED")) {
                    webView.loadUrl("https://m.youtube.com/");
                } else if (description.toString().contains("NAME_NOT_RESOLVED")) {
                    Snackbar.make(appWindow, "Oh no! You are not connected to the internet.", Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                } else {
                    Snackbar.make(appWindow, "Oh no! " + description, Snackbar.LENGTH_INDEFINITE).setAction("Reload", clickListener).show();
                }
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowFileAccess(false);

        webSettings.setDatabaseEnabled(true);

        String cachePath = this.getApplicationContext()
                .getDir("cache", Context.MODE_PRIVATE).getPath();
        webSettings.setAppCachePath(cachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setHorizontalScrollBarEnabled(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setBackgroundColor(Color.WHITE);
        webView.setScrollbarFadingEnabled(true);
        webView.setNetworkAvailable(true);

        webView.loadUrl("https://m.youtube.com/");

        // Floating action buttons
        FloatingActionButton fabBrowser = (FloatingActionButton) findViewById(R.id.fab_browser);
        fabBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl())));
            }
        });

        FloatingActionButton fabRefresh = (FloatingActionButton) findViewById(R.id.fab_refresh);
        fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        FloatingActionButton fabHome = (FloatingActionButton) findViewById(R.id.fab_home);
        fabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("https://m.youtube.com/");
            }
        });
    }

    // For the snackbar with error message
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            webView.loadUrl("https://m.youtube.com/");
        }
    };

    // For easier navigation
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }

    }
}

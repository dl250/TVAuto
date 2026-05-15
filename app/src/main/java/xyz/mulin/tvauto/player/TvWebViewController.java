package xyz.mulin.tvauto.player;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import xyz.mulin.tvauto.model.UserScript;

public final class TvWebViewController {
    public interface CurrentUrlProvider {
        String getCurrentUrl();
    }

    public interface UserScriptProvider {
        UserScript findMatchingScript(String url);
    }

    private final WebView webView;
    private final CurrentUrlProvider currentUrlProvider;
    private final UserScriptProvider userScriptProvider;

    public TvWebViewController(
            WebView webView,
            CurrentUrlProvider currentUrlProvider,
            UserScriptProvider userScriptProvider
    ) {
        this.webView = webView;
        this.currentUrlProvider = currentUrlProvider;
        this.userScriptProvider = userScriptProvider;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setup() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");

        webView.setOnTouchListener((v, event) -> true);
        webView.setOnKeyListener((v, keyCode, event) -> true);
        webView.setOnGenericMotionListener((v, event) -> true);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                if (url.startsWith("https://test.ustc.edu.cn/")) {
                    String js =
                            "(function(){" +
                                    "var style=document.createElement('style');" +
                                    "style.innerHTML=`" +
                                    "body * { visibility:hidden !important; }" +
                                    "#test, #test * { visibility:visible !important; }" +
                                    "#test { position:absolute !important; top:0 !important;left: 0% !important;  }" +
                                    "html, body { overflow:hidden !important; }" +
                                    "`;" +
                                    "document.head.appendChild(style);" +
                                    "})();";

                    view.evaluateJavascript(js, null);
                }
            }



            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectPreferredScript(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(
                        "Boolean(window.__TVAUTO_USER_SCRIPT_INJECTED__ || window.__VIDEO_RESIZE_INJECTED__)",
                        value -> {
                    if ("true".equals(value)) {
                        Log.d("TJS", "onPageStarted 阶段注入成功");
                    } else {
                        Log.d("TJS", "onPageStarted 阶段注入失败，onPageFinished 二次注入");
                        injectPreferredScript(view, url);
                    }
                });
            }
        });
    }

    private void injectPreferredScript(WebView view, String pageUrl) {
        UserScript userScript = userScriptProvider.findMatchingScript(pageUrl);
        if (userScript != null) {
            injectUserScript(view, userScript);
            return;
        }
        injectVideoResizeJs(view);
    }

    private void injectUserScript(WebView view, UserScript userScript) {
        String wrappedScript =
                "(function(){" +
                        "try{" +
                        userScript.getJavascript() +
                        "\n;window.__TVAUTO_USER_SCRIPT_INJECTED__=true;" +
                        "}catch(e){console.error('TVAuto user script error',e);}" +
                        "})();";
        view.evaluateJavascript(wrappedScript, null);
    }
    // 注入 JavaScript  (全屏)
    private void injectVideoResizeJs(WebView view) {
        String c = currentUrlProvider.getCurrentUrl();
        if (!c.startsWith("file:///") && !c.startsWith("https://test.ustc.edu.cn/")) {
            String encodedJs ="KGZ1bmN0aW9uKCkgewogICAgdmFyIGxheWVySWQgPSAndHZhdXRvX2xvYWRpbmdfbGF5ZXInOwogICAgaWYgKCFkb2N1bWVudC5nZXRFbGVtZW50QnlJZChsYXllcklkKSkgewogICAgICAgIHZhciBjc3MgPSBgCiAgICAgICAgICAgIEBrZXlmcmFtZXMgcHVsc2VKdW1wIHsKICAgICAgICAgICAgICAgIDAlLCAxMDAlIHsgdHJhbnNmb3JtOiB0cmFuc2xhdGVZKDApIHNjYWxlKDEpOyB9CiAgICAgICAgICAgICAgICAzMCUgeyB0cmFuc2Zvcm06IHRyYW5zbGF0ZVkoLTAuMTJlbSkgc2NhbGUoMS4wMyk7IH0KICAgICAgICAgICAgICAgIDYwJSB7IHRyYW5zZm9ybTogdHJhbnNsYXRlWSgwLjAyZW0pIHNjYWxlKDAuOTgpOyB9CiAgICAgICAgICAgIH0KICAgICAgICAgICAgIyR7bGF5ZXJJZH0gewogICAgICAgICAgICAgICAgcG9zaXRpb246IGZpeGVkOyB0b3A6IDA7IGxlZnQ6IDA7IHJpZ2h0OiAwOyBib3R0b206IDA7CiAgICAgICAgICAgICAgICB3aWR0aDogMTAwdnc7IGhlaWdodDogMTAwdmg7CiAgICAgICAgICAgICAgICBiYWNrZ3JvdW5kOiAjMDAwMDAwOyAKICAgICAgICAgICAgICAgIHotaW5kZXg6IDIxNDc0ODM2NDc7IAogICAgICAgICAgICAgICAgZGlzcGxheTogZmxleDsganVzdGlmeS1jb250ZW50OiBjZW50ZXI7IGFsaWduLWl0ZW1zOiBjZW50ZXI7CiAgICAgICAgICAgICAgICBwb2ludGVyLWV2ZW50czogbm9uZTsKICAgICAgICAgICAgICAgIHRyYW5zaXRpb246IG9wYWNpdHkgMC4zczsKICAgICAgICAgICAgICAgIHRyYW5zZm9ybTogbm9uZSAhaW1wb3J0YW50OwogICAgICAgICAgICAgICAgbWFyZ2luOiAwICFpbXBvcnRhbnQ7CiAgICAgICAgICAgICAgICBwYWRkaW5nOiAwICFpbXBvcnRhbnQ7CiAgICAgICAgICAgICAgICBib3gtc2l6aW5nOiBib3JkZXItYm94OwogICAgICAgICAgICB9CiAgICAgICAgICAgIC50di10ZXh0LWNvbnRhaW5lciB7CiAgICAgICAgICAgICAgICBmb250LWZhbWlseTogc2Fucy1zZXJpZjsgZm9udC13ZWlnaHQ6IDkwMDsgZm9udC1zaXplOiA1dnc7CiAgICAgICAgICAgICAgICBkaXNwbGF5OiBmbGV4OyB3aGl0ZS1zcGFjZTogbm93cmFwOyBsZXR0ZXItc3BhY2luZzogMC4wNWVtOwogICAgICAgICAgICAgICAgdHJhbnNmb3JtOiBub25lOwogICAgICAgICAgICB9CiAgICAgICAgICAgIC50di1jaGFyIHsKICAgICAgICAgICAgICAgIGRpc3BsYXk6IGlubGluZS1ibG9jazsKICAgICAgICAgICAgICAgIGFuaW1hdGlvbjogcHVsc2VKdW1wIDAuOHMgaW5maW5pdGUgZWFzZS1vdXQ7CiAgICAgICAgICAgIH0KICAgICAgICBgOwogICAgICAgIHZhciBzdHlsZSA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQoJ3N0eWxlJyk7CiAgICAgICAgc3R5bGUuYXBwZW5kQ2hpbGQoZG9jdW1lbnQuY3JlYXRlVGV4dE5vZGUoY3NzKSk7CiAgICAgICAgZG9jdW1lbnQuaGVhZC5hcHBlbmRDaGlsZChzdHlsZSk7CgogICAgICAgIHZhciBsYXllciA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQoJ2RpdicpOwogICAgICAgIGxheWVyLmlkID0gbGF5ZXJJZDsKICAgICAgICBsYXllci5pbm5lckhUTUwgPSBgCiAgICAgICAgICAgIDxkaXYgY2xhc3M9InR2LXRleHQtY29udGFpbmVyIj4KICAgICAgICAgICAgICAgIDxzcGFuIGNsYXNzPSJ0di1jaGFyIiBzdHlsZT0iY29sb3I6IzMzMzgzQzsgYW5pbWF0aW9uLWRlbGF5OjBzIj5UPC9zcGFuPgogICAgICAgICAgICAgICAgPHNwYW4gY2xhc3M9InR2LWNoYXIiIHN0eWxlPSJjb2xvcjojMzMzODNDOyBhbmltYXRpb24tZGVsYXk6MC4wNHMiPlY8L3NwYW4+CiAgICAgICAgICAgICAgICA8c3BhbiBjbGFzcz0idHYtY2hhciIgc3R5bGU9ImNvbG9yOiMwMDc5RkI7IGFuaW1hdGlvbi1kZWxheTowLjA4cyI+QTwvc3Bhbj4KICAgICAgICAgICAgICAgIDxzcGFuIGNsYXNzPSJ0di1jaGFyIiBzdHlsZT0iY29sb3I6IzAwNzlGQjsgYW5pbWF0aW9uLWRlbGF5OjAuMTJzIj51PC9zcGFuPgogICAgICAgICAgICAgICAgPHNwYW4gY2xhc3M9InR2LWNoYXIiIHN0eWxlPSJjb2xvcjojMDA3OUZCOyBhbmltYXRpb24tZGVsYXk6MC4xNnMiPnQ8L3NwYW4+CiAgICAgICAgICAgICAgICA8c3BhbiBjbGFzcz0idHYtY2hhciIgc3R5bGU9ImNvbG9yOiMwMDc5RkI7IGFuaW1hdGlvbi1kZWxheTowLjIwcyI+bzwvc3Bhbj4KICAgICAgICAgICAgPC9kaXY+YDsKICAgICAgICBkb2N1bWVudC5kb2N1bWVudEVsZW1lbnQuYXBwZW5kQ2hpbGQobGF5ZXIpOwogICAgfQoKICAgIGZ1bmN0aW9uIHNob3dMb2FkaW5nKCkgewogICAgICAgIHZhciBlbCA9IGRvY3VtZW50LmdldEVsZW1lbnRCeUlkKGxheWVySWQpOwogICAgICAgIGlmIChlbCkgeyBlbC5zdHlsZS5vcGFjaXR5ID0gJzEnOyBlbC5zdHlsZS5kaXNwbGF5ID0gJ2ZsZXgnOyB9CiAgICB9CiAgICBmdW5jdGlvbiBoaWRlTG9hZGluZygpIHsKICAgICAgICB2YXIgZWwgPSBkb2N1bWVudC5nZXRFbGVtZW50QnlJZChsYXllcklkKTsKICAgICAgICBpZiAoZWwpIHsgCiAgICAgICAgICAgIGVsLnN0eWxlLm9wYWNpdHkgPSAnMCc7IAogICAgICAgICAgICBzZXRUaW1lb3V0KCgpID0+IHsgaWYoZWwuc3R5bGUub3BhY2l0eSA9PT0gJzAnKSBlbC5zdHlsZS5kaXNwbGF5ID0gJ25vbmUnOyB9LCAzMDApOwogICAgICAgIH0KICAgIH0KCiAgICBzaG93TG9hZGluZygpOwoKICAgIHdpbmRvdy5fX1ZJREVPX1JFU0laRV9JTkpFQ1RFRF9fID0gdHJ1ZTsKICAgIHZhciB1cmwgPSB3aW5kb3cubG9jYXRpb24uaHJlZi50b0xvd2VyQ2FzZSgpOwogICAgdmFyIGNvbnRhaW5zRG91eXUgPSB1cmwuaW5jbHVkZXMoJ2RvdXl1Jyk7CiAgICB2YXIgY29udGFpbnNNM3U4ID0gdXJsLmluY2x1ZGVzKCcubTN1OCcpOwogICAgdmFyIGNvbnRhaW5zTTN1ID0gdXJsLmluY2x1ZGVzKCcubTN1Jyk7CiAgICB2YXIgY29udGFpbnNIdXlhID0gdXJsLmluY2x1ZGVzKCdodXlhJyk7CiAgICB2YXIgbmVlZFNjYWxlSGFsZiA9ICEoY29udGFpbnNEb3V5dSB8fCBjb250YWluc00zdTggfHwgY29udGFpbnNNM3UgfHwgY29udGFpbnNIdXlhKTsKICAgIGxldCBjb3VudCA9IDA7CgogICAgdmFyIGludGVydmFsID0gc2V0SW50ZXJ2YWwoZnVuY3Rpb24oKSB7CiAgICAgICAgY29uc29sZS5sb2coIm9uUGFnZVN0YXJ0ZWQtPiBnZXRfdmlkZW8iKTsKICAgICAgICB2YXIgdmlkZW8gPSBkb2N1bWVudC5xdWVyeVNlbGVjdG9yKCd2aWRlbycpOwogICAgICAgIAogICAgICAgIGlmICh2aWRlbykgewogICAgICAgICAgICBpZiAoIXZpZGVvLmdldEF0dHJpYnV0ZSgnZGF0YS10dmF1dG8tYm91bmQnKSkgewogICAgICAgICAgICAgICAgdmlkZW8uYWRkRXZlbnRMaXN0ZW5lcignd2FpdGluZycsIHNob3dMb2FkaW5nKTsKICAgICAgICAgICAgICAgIHZpZGVvLmFkZEV2ZW50TGlzdGVuZXIoJ2xvYWRzdGFydCcsIHNob3dMb2FkaW5nKTsKICAgICAgICAgICAgICAgIHZpZGVvLmFkZEV2ZW50TGlzdGVuZXIoJ3NlZWtpbmcnLCBzaG93TG9hZGluZyk7CiAgICAgICAgICAgICAgICB2aWRlby5hZGRFdmVudExpc3RlbmVyKCdwbGF5aW5nJywgaGlkZUxvYWRpbmcpOwogICAgICAgICAgICAgICAgdmlkZW8uYWRkRXZlbnRMaXN0ZW5lcignY2FucGxheScsIGhpZGVMb2FkaW5nKTsKICAgICAgICAgICAgICAgIHZpZGVvLmFkZEV2ZW50TGlzdGVuZXIoJ3NlZWtlZCcsIGhpZGVMb2FkaW5nKTsKICAgICAgICAgICAgICAgIHZpZGVvLnNldEF0dHJpYnV0ZSgnZGF0YS10dmF1dG8tYm91bmQnLCAndHJ1ZScpOwogICAgICAgICAgICB9CiAgICAgICAgICAgIGRvY3VtZW50LmJvZHkuc3R5bGUudHJhbnNmb3JtT3JpZ2luID0gJ3RvcCBsZWZ0JzsKCiAgICAgICAgICAgIGlmIChuZWVkU2NhbGVIYWxmKSB7CiAgICAgICAgICAgICAgICBkb2N1bWVudC5ib2R5LnN0eWxlLnRyYW5zZm9ybSA9ICdzY2FsZSgwLjUpJzsKICAgICAgICAgICAgICAgIHZpZGVvLnN0eWxlLndpZHRoID0gJ2NhbGMoMjAwdmggKiAxNiAvIDkpJzsKICAgICAgICAgICAgICAgIHZpZGVvLnN0eWxlLmhlaWdodCA9ICcyMDB2aCc7CiAgICAgICAgICAgIH0gZWxzZSB7CiAgICAgICAgICAgICAgICBkb2N1bWVudC5ib2R5LnN0eWxlLnRyYW5zZm9ybSA9ICcnOwogICAgICAgICAgICAgICAgdmlkZW8uc3R5bGUud2lkdGggPSAnY2FsYygxMDB2aCAqIDE2IC8gOSknOwogICAgICAgICAgICAgICAgdmlkZW8uc3R5bGUuaGVpZ2h0ID0gJzEwMHZoJzsKICAgICAgICAgICAgfQoKICAgICAgICAgICAgdmlkZW8uc3R5bGUucG9zaXRpb24gPSAnZml4ZWQnOwogICAgICAgICAgICB2aWRlby5zdHlsZS50b3AgPSAnMCc7CiAgICAgICAgICAgIHZpZGVvLnN0eWxlLmxlZnQgPSAnMCc7CiAgICAgICAgICAgIHZpZGVvLnN0eWxlLm9iamVjdEZpdCA9ICdjb3Zlcic7CiAgICAgICAgICAgIHZpZGVvLnN0eWxlLnpJbmRleCA9ICc5OTk5JzsKICAgICAgICAgICAgdmlkZW8uc3R5bGUuYmFja2dyb3VuZENvbG9yID0gJ2JsYWNrJzsKICAgICAgICAgICAgdmlkZW8ubXV0ZWQgPSBmYWxzZTsKICAgICAgICAgICAgdmlkZW8udm9sdW1lID0gMS4wOwogICAgICAgICAgICB2aWRlby5wbGF5KCk7CiAgICAgICAgICAgIAogICAgICAgICAgICBsZXQgZWwgPSB2aWRlbzsKICAgICAgICAgICAgd2hpbGUgKGVsKSB7CiAgICAgICAgICAgICAgICBlbC5zdHlsZS5vdmVyZmxvdyA9ICd2aXNpYmxlJzsKICAgICAgICAgICAgICAgIGlmIChlbC5zdHlsZSAmJiBlbCAhPT0gZG9jdW1lbnQuYm9keSkgZWwuc3R5bGUuekluZGV4ID0gJzk5OTknOwogICAgICAgICAgICAgICAgZWwgPSBlbC5wYXJlbnRFbGVtZW50OwogICAgICAgICAgICB9CiAgICAgICAgICAgIGlmICghdmlkZW8ucGF1c2VkICYmIHZpZGVvLnJlYWR5U3RhdGUgPj0gMyAmJiB2aWRlby5tb3pIYXNBdWRpbyAhPT0gZmFsc2UpIHsKICAgICAgICAgICAgICAgIGNvbnNvbGUubG9nKCJvblBhZ2VTdGFydGVkLT4g5aSE55CG5a6M5oiQIik7CiAgICAgICAgICAgICAgICBoaWRlTG9hZGluZygpOwogICAgICAgICAgICAgICAgY2xlYXJJbnRlcnZhbChpbnRlcnZhbCk7CiAgICAgICAgICAgIH0KICAgICAgICAgICAgY291bnQrKzsKICAgICAgICAgICAgaWYoY291bnQgPiAxMCl7CiAgICAgICAgICAgICAgICBjb25zb2xlLmxvZygib25QYWdlU3RhcnRlZC0+IOi2heaXtuiHquWKqOWFs+mXreWumuaXtuWZqCIpOwogICAgICAgICAgICAgICAgY2xlYXJJbnRlcnZhbChpbnRlcnZhbCk7CiAgICAgICAgICAgIH0KICAgICAgICB9IGVsc2UgewogICAgICAgICAgICBzaG93TG9hZGluZygpOwogICAgICAgIH0KICAgIH0sIDUwMCk7Cn0pKCk7";
            try {
                byte[] decodedBytes = Base64.decode(encodedJs, Base64.DEFAULT);
                String jsCode = new String(decodedBytes);
                view.evaluateJavascript(jsCode, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

package xyz.mulin.tvauto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.content.pm.ActivityInfo;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private WebView webView;
    private SharedPreferences configPrefs;
    private SharedPreferences programPrefs;
    private GestureDetector gestureDetector;

    private final LinkedHashMap<String, String> channelsMap  = new LinkedHashMap<>() {
    };
    private String[] channels;
    private int currentChannelIndex = 0;
    private int lastIndex = -1;
    private float ratio;
    private int ratio_flag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height =  dm.heightPixels;
        ratio = (float) dm.widthPixels / dm.heightPixels;

        if(isTV(this)){
            setContentView(R.layout.activity_main_n);
            ratio_flag = 0;
        }
        else if (ratio > 16f/9f) { // 16:9比例判断
            setContentView(R.layout.activity_main);
            ratio_flag = 1;
        } else if(ratio < 16f/9f){
            setContentView(R.layout.activity_main_v);
            ratio_flag = 2;
        }else{
            setContentView(R.layout.activity_main_n);
            ratio_flag = 0;
        }
        showToast((isTV(this)?"TV ":"")+width+"x"+height+" @"+width/gcd(width,height)+":"+height/gcd(width,height));

        programPrefs = getSharedPreferences("TVAuto_Program", Context.MODE_PRIVATE);
        loadUserChannels();
        configPrefs = getSharedPreferences("TVAuto_Config", MODE_PRIVATE);
        currentChannelIndex = configPrefs.getInt("lastChannel", 0);

        if(channelsMap.isEmpty()){
            channelsMap.put("file:///android_asset/add_channel_help.html", "频道添加指南");
        }
        else if(Objects.equals(channelsMap.get("file:///android_asset/add_channel_help.html"), "频道添加指南")){
            channelsMap.remove("file:///android_asset/add_channel_help.html");
        }

        channels = channelsMap.keySet().toArray(new String[0]);

        webView = findViewById(R.id.webView);
        setupWebView();
        setupWebViewLayoutParams();
        loadChannel(currentChannelIndex);
        if(ratio_flag != 0) {
            setupChannelBar();
        }
        setupGestureDetector();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    private void setupWebViewLayoutParams() {
        webView.post(() -> {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;

            LinearLayout.LayoutParams params;
            if (ratio_flag == 1) {
                int width = Math.round(screenHeight * 16f / 9f);
                params = new LinearLayout.LayoutParams(width, screenHeight);
            } else if (ratio_flag == 2) {
                int height = Math.round(screenWidth * 9f / 16f);
                params = new LinearLayout.LayoutParams(screenWidth, height);
            } else {
                params = new LinearLayout.LayoutParams(screenWidth, screenHeight);
            }
            webView.setLayoutParams(params);
        });
        webView.setFocusable(false);
    }


    private void setupChannelBar() {
        LinearLayout channelBar = findViewById(R.id.channelBar);

        for (int i = 0; i < channels.length; i++) {
            final int index = i;
            MaterialButton button = new MaterialButton(this);
            button.setText(channelsMap.get(channels[i]));
            button.setCornerRadius(24);
            button.setStrokeWidth(2);
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#333333")));
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#546E7A")));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(18, -8, 18, -8);
            button.setLayoutParams(params);

            button.setTextSize(10+10*Math.abs(ratio - 16/9.f));
            button.setPadding(10, 10, 10, 10);
            button.setOnClickListener(v -> {
                currentChannelIndex = index;
                loadChannel(currentChannelIndex);
                saveChannelIndex();
            });

            channelBar.addView(button);
        }
        if (ratio_flag == 1) manageMobileChannels(channelBar);
    }
    private void manageMobileChannels(LinearLayout channelBar) {
        LinearLayout addChannelLayout = new LinearLayout(this);
        addChannelLayout.setOrientation(LinearLayout.VERTICAL);
        addChannelLayout.setPadding(5, 10, 5, 10);

        EditText nameInput = new EditText(this);
        nameInput.setHint("频道名称");
        nameInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addChannelLayout.addView(nameInput);

        EditText urlInput = new EditText(this);
        urlInput.setHint("直播 URL");
        urlInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addChannelLayout.addView(urlInput);

        MaterialButton btnAdd = new MaterialButton(this);
        btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#54CC7A")));
        btnAdd.setText("添加频道");
        btnAdd.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnAdd.setTextSize(10+10*Math.abs(ratio - 16/9.f));
        addChannelLayout.addView(btnAdd);

        EditText inputAll = new EditText(this);
        inputAll.setHint("导入脚本");
        inputAll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addChannelLayout.addView(inputAll);

        MaterialButton btnImport = new MaterialButton(this);
        btnImport.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6600")));
        btnImport.setText("一键导入");
        btnImport.setTextSize(10+10*Math.abs(ratio - 16/9.f));
        btnImport.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addChannelLayout.addView(btnImport);

        MaterialButton btnDel = new MaterialButton(this);
        btnDel.setText("删除当前频道");
        btnDel.setTextSize(10+10*Math.abs(ratio - 16/9.f));
        btnDel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
        addChannelLayout.addView(btnDel);

        btnDel.setOnClickListener(v -> deleteCurrentChannel());


        btnAdd.setOnClickListener(v -> addOneChannel(nameInput,urlInput));
        btnImport.setOnClickListener(v -> addAllChannels(inputAll,0));
        channelBar.addView(addChannelLayout);
    }
    private void manageTvChannels() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("频道管理");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        scrollView.addView(layout);

        EditText nameInput = new EditText(this);
        nameInput.setHint("频道名称");
        layout.addView(nameInput);

        EditText urlInput = new EditText(this);
        urlInput.setHint("直播 URL");
        layout.addView(urlInput);

        MaterialButton btnAdd = new MaterialButton(this);
        btnAdd.setText("添加频道");
        btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#54CC7A")));
        layout.addView(btnAdd);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2);
        dividerParams.setMargins(0, 30, 0, 30);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(Color.LTGRAY);
        layout.addView(divider);

        EditText inputAll = new EditText(this);
        inputAll.setHint("批量导入格式：\"名称\",\"URL\"; ...\n输入为空导入默认频道");
        inputAll.setMinLines(3);
        inputAll.setGravity(Gravity.TOP);
        layout.addView(inputAll);

        MaterialButton btnImport = new MaterialButton(this);
        btnImport.setText("一键导入");
        btnImport.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6600")));
        layout.addView(btnImport);

        MaterialButton btnDel = new MaterialButton(this);
        btnDel.setText("删除当前频道");
        btnDel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
        layout.addView(btnDel);

        TextView texInfo = new TextView(this);
        texInfo.setText(R.string.tvauto_v);
        layout.addView(texInfo);

        builder.setView(scrollView);
        AlertDialog dialog = builder.create();
        dialog.show();


        btnAdd.setOnClickListener(v -> addOneChannel(nameInput,urlInput));
        btnImport.setOnClickListener(v -> addAllChannels(inputAll,1));
        btnDel.setOnClickListener(v -> deleteCurrentChannel());
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 300;
            private static final int SWIPE_VELOCITY_THRESHOLD = 200;

            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) switchToPrevChannel();
                        else switchToNextChannel();
                        return true;
                    }
                }
                return false;
            }
        });
    }
    // 定义变量
    private boolean volumeUpPressed = false;
    private boolean volumeDownPressed = false;
    private long volumeUpTime = 0;
    private long volumeDownTime = 0;
    private static final long COMBO_PRESS_THRESHOLD = 500; // 500ms内认为是同时按下

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long now = System.currentTimeMillis();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = true;
            volumeUpTime = now;
            if (volumeDownPressed && Math.abs(volumeUpTime - volumeDownTime) <= COMBO_PRESS_THRESHOLD) {
                manageTvChannels();
                resetVolumeKeys();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownPressed = true;
            volumeDownTime = now;
            if (volumeUpPressed && Math.abs(volumeUpTime - volumeDownTime) <= COMBO_PRESS_THRESHOLD) {
                manageTvChannels();
                resetVolumeKeys();
                return true;
            }
        }

        switch (keyCode) {

            case KeyEvent.KEYCODE_DPAD_CENTER:
                loadChannel(currentChannelIndex); return true;
            case KeyEvent.KEYCODE_MENU:
                manageTvChannels(); return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                switchToPrevChannel(); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                switchToNextChannel(); return true;
            case KeyEvent.KEYCODE_0: case KeyEvent.KEYCODE_1: case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3: case KeyEvent.KEYCODE_4: case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6: case KeyEvent.KEYCODE_7: case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                handleDigitInput(keyCode - KeyEvent.KEYCODE_0); return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // 重置按键状态
    private void resetVolumeKeys() {
        volumeUpPressed = false;
        volumeDownPressed = false;
        volumeUpTime = 0;
        volumeDownTime = 0;
    }


    private int digitBuffer = -1;
    private long lastDigitTime = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable digitConfirmRunnable = this::confirmDigitInput;

    private void handleDigitInput(int digit) {
        long now = System.currentTimeMillis();

        // 判断是否超时
        if (now - lastDigitTime > 2000) {
            digitBuffer = digit;
        } else {
            digitBuffer = digitBuffer * 10 + digit;
        }

        lastDigitTime = now;
        if(digitBuffer>0){
            showToast(String.valueOf(digitBuffer));
        }
        // 移除旧的延迟任务，并重新延迟执行确认输入
        handler.removeCallbacks(digitConfirmRunnable);
        handler.postDelayed(digitConfirmRunnable, 2000);
    }

    private void confirmDigitInput() {
        if (digitBuffer >= 0 && digitBuffer < channels.length) {
            currentChannelIndex = digitBuffer;
            loadChannel(currentChannelIndex);
            saveChannelIndex();
        }
        else{
            showToast("无此频道！");
        }
        digitBuffer = -1; // 重置缓冲
    }


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDisplayZoomControls(false);
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                injectJsFromAssets(view, "video_resize.js");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void injectJsFromAssets(WebView view, String assetFileName) {
        try {
            InputStream input = getAssets().open(assetFileName);
            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            String js = new String(buffer, "UTF-8");
            view.evaluateJavascript("javascript:" + js, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void loadChannel(int index) {
        String channel = channels[index];
        if (index != lastIndex)
            showToast(index+"  "+channelsMap.get(channel));
        lastIndex = index;
        webView.loadUrl(channel);
    }

    private void saveChannelIndex() {
        configPrefs.edit().putInt("lastChannel", currentChannelIndex).apply();
    }

    private void switchToNextChannel() {
        currentChannelIndex = (currentChannelIndex + 1) % channels.length;
        loadChannel(currentChannelIndex);
        saveChannelIndex();
    }

    private void switchToPrevChannel() {
        currentChannelIndex = (currentChannelIndex - 1 + channels.length) % channels.length;
        loadChannel(currentChannelIndex);
        saveChannelIndex();
    }
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void saveUserChannel(String url, String name) {
        String json = programPrefs.getString("user_channels", "[]");
        try {
            JSONArray array = new JSONArray(json);
            // 检查是否已存在相同 url，避免重复
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("url").equals(url)) {
                    return; // 已存在，不添加
                }
            }

            JSONObject newChannel = new JSONObject();
            newChannel.put("name", name);
            newChannel.put("url", url);
            array.put(newChannel);

            programPrefs.edit().putString("user_channels", array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadUserChannels() {
        channelsMap.clear();
        String json = programPrefs.getString("user_channels", "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                String url = obj.getString("url");
                channelsMap.put(url, name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private  void addOneChannel(EditText inputName,EditText inputUrl){
        String name = inputName.getText().toString().trim();
        String url = inputUrl.getText().toString().trim();

        if (!name.isEmpty() && !url.isEmpty()) {
            if (channelsMap.containsKey(url)) {
                showToast("该频道已存在！");
            } else {
                saveUserChannel(url, name);
                channelsMap.put(url, name);
                channels = channelsMap.keySet().toArray(new String[0]);
                showToast("频道已添加！");
                restartApp();
            }
        } else {
            showToast("请输入频道名称和 URL");
        }
    }

    private void addAllChannels(EditText inputAll,int mode){
        String input = inputAll.getText().toString().trim();
        if (input.isEmpty()) {
            if(mode == 1){
                showToast("导入默认频道脚本");
                inputAll.setText(R.string.DefaultChannel);
            }else{
                showToast("请输入导入脚本");
            }
            return;
        }

        String[] entries = input.split(";");
        Pattern pattern = Pattern.compile("^\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*$");
        int count = 0, errorCount = 0;

        for (String entry : entries) {
            Matcher matcher = pattern.matcher(entry);
            if (matcher.matches()) {
                String name = matcher.group(1).trim();
                String url = matcher.group(2).trim();
                if (!channelsMap.containsKey(url)) {
                    saveUserChannel(url, name);
                    channelsMap.put(url, name);
                    count++;
                }
            } else {
                errorCount++;
            }
        }

        if (count > 0) {
            channels = channelsMap.keySet().toArray(new String[0]);
            showToast("成功导入 " + count + " 个频道" + (errorCount > 0 ? "，跳过 " + errorCount + " 条格式错误" : ""));
            restartApp();
        } else {
            showToast("无有效频道或频道已存在");
        }
    }

    private void deleteCurrentChannel() {
        String currentUrl = channels[currentChannelIndex];

        // 保护内置频道不被删除
        if (currentUrl.startsWith("file:///android_asset")) {
            showToast("内置频道不可删除");
            return;
        }

        String json = programPrefs.getString("user_channels", "[]");
        try {
            JSONArray array = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String url = obj.getString("url");
                // 不等于当前频道 url 的都加入新数组
                if (!url.equals(currentUrl)) {
                    newArray.put(obj);
                }
            }

            // 保存更新后的数组
            programPrefs.edit().putString("user_channels", newArray.toString()).apply();

            // 重新加载到内存
            loadUserChannels();
            channels = channelsMap.keySet().toArray(new String[0]);

            // 调整 currentChannelIndex，避免越界
            if (currentChannelIndex >= channels.length) {
                currentChannelIndex = Math.max(0, channels.length - 1);
            }
            saveChannelIndex();
            showToast("频道已删除");
            restartApp();

        } catch (JSONException e) {
            e.printStackTrace();
            showToast("删除失败");
        }
    }

    public void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
            startActivity(intent);
            finish();
        }
    public static int gcd(int a, int b) {
        if(b == 0) return a;
        else return gcd(b, a % b);
    }
    private boolean isTV(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

}

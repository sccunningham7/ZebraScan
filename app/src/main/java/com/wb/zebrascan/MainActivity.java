package com.wb.zebrascan;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.Set;

import static android.provider.ContactsContract.Intents.Insert.ACTION;

public class MainActivity extends AppCompatActivity {

    // DataWedge strings
    private static final String EXTRA_PROFILENAME = "ZebraScan";

    // DataWedge Extras
    private static final String EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
    private static final String EXTRA_KEY_APPLICATION_NAME = "com.symbol.datawedge.api.APPLICATION_NAME";
    private static final String EXTRA_KEY_NOTIFICATION_TYPE = "com.symbol.datawedge.api.NOTIFICATION_TYPE";
    private static final String EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
    private static final String EXTRA_RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION";
    private static final String EXTRA_REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
    private static final String EXTRA_UNREGISTER_NOTIFICATION = "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION";
    private static final String EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";

    private static final String EXTRA_RESULT_NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
    private static final String EXTRA_KEY_VALUE_SCANNER_STATUS = "SCANNER_STATUS";
    private static final String EXTRA_KEY_VALUE_PROFILE_SWITCH = "PROFILE_SWITCH";
    private static final String EXTRA_KEY_VALUE_CONFIGURATION_UPDATE = "CONFIGURATION_UPDATE";
    private static final String EXTRA_KEY_VALUE_NOTIFICATION_STATUS = "STATUS";
    private static final String EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME = "PROFILE_NAME";
    private static final String EXTRA_SEND_RESULT = "SEND_RESULT";

    // Scanner control
    private static final String EXTRA_SWITCH_SCANNER_EX = "com.symbol.datawedge.api.SWITCH_SCANNER_EX";

    private static final String EXTRA_RESULT_GET_VERSION_INFO = "com.symbol.datawedge.api.RESULT_GET_VERSION_INFO";
    private static final String EXTRA_RESULT = "RESULT";
    private static final String EXTRA_RESULT_INFO = "RESULT_INFO";
    private static final String EXTRA_COMMAND = "COMMAND";

    // DataWedge Actions
    private static final String ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION";
    private static final String ACTION_RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    private static final String ACTION_RESULT = "com.symbol.datawedge.api.RESULT_ACTION";

    // private variables
    private Boolean bRequestSendResult = false;
    final String LOG_TAG = "ZebraScan";
    private static Boolean CameraSelectedScanner = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Init settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // If cookies set to wipe, wipe them here
        if (sharedPreferences.getBoolean("cookies_exit", true)) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        }

        // Set toolbar up
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final WebView mainWebView = findViewById(R.id.webView);
        final TextView errorView = findViewById(R.id.errorView);

        if (!isDataWedgeInstalled(getPackageManager())) {
            mainWebView.setVisibility(View.INVISIBLE);
            errorView.setVisibility(View.VISIBLE);
        } else {
            // Set webview up
            final ProgressBar progressBar = findViewById(R.id.progressBar);
            mainWebView.setWebViewClient(new WebViewClient());
            mainWebView.setWebChromeClient(new WebChromeClient() {
                // Progress bar mapping
                public void onProgressChanged(WebView view, int progress) {
                    if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                    progressBar.setProgress(progress);
                    if (progress == 100) {
                        progressBar.setVisibility(ProgressBar.GONE);
                    }
                }
            });

            // Load RocketRez and enable JS
            mainWebView.loadUrl("https://secure.rocket-rez.com/rocketscan/");
            WebSettings webSettings = mainWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
        }

        // Create DataWedge profile
        CreateProfile();

        // Register for status change notification
        // Use REGISTER_FOR_NOTIFICATION: http://techdocs.zebra.com/datawedge/latest/guide/api/registerfornotification/
        Bundle b = new Bundle();
        b.putString(EXTRA_KEY_APPLICATION_NAME, getPackageName());
        b.putString(EXTRA_KEY_NOTIFICATION_TYPE, "SCANNER_STATUS");     // register for changes in scanner status
        sendDataWedgeIntentWithExtra(EXTRA_REGISTER_NOTIFICATION, b);

        registerReceivers();
    }

    /**
     * Checks if datawedge is installed using package name
     * @param packageManager The packagemanger for the calling context.
     * @return true if installed, false otherwise
     */
    private boolean isDataWedgeInstalled(PackageManager packageManager) {
        try {
            PackageInfo v = packageManager.getPackageInfo("com.symbol.datawedge", 0);
            Log.i(LOG_TAG, "DataWedge is installed. Version found: " + v.versionName );
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.i(LOG_TAG, "DataWedge is not installed!");
            return false;
        }
    }

    /**
     * Inflates options menu to toolbar
     * @author Chandler Cunningham
     * @param menu The menu to add.
     * @return True if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * Handle toolbar click events
     * @author Chandler Cunningham
     * @param item The selected toolbar button
     * @return true if successful, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final WebView mainWebView = findViewById(R.id.webView);
        switch (item.getItemId()) {
            case R.id.action_back: {
                if (mainWebView.canGoBack()) {
                    mainWebView.goBack();
                }
                return true;
            }
            case R.id.action_forward : {
                if (mainWebView.canGoForward()) {
                    mainWebView.goForward();
                }
                return true;
            }
            case R.id.action_refresh : {
                mainWebView.reload();
                return true;
            }
            case R.id.action_settings : {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
            default : {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Creates or updates a profile in DataWedge. Intent broadcast.
     * From Zebra's DataCapture1 demo application
     * @author Zebra
     */
    public void CreateProfile () {

        // Send DataWedge intent with extra to create profile
        // Use CREATE_PROFILE: http://techdocs.zebra.com/datawedge/latest/guide/api/createprofile/
        sendDataWedgeIntentWithExtra(EXTRA_CREATE_PROFILE, EXTRA_PROFILENAME);

        // Configure created profile to apply to this app
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", EXTRA_PROFILENAME);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");  // Create profile if it does not exist

        // Configure barcode input plugin
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true"); //  This is the default
        Bundle autoBarcodeProps = new Bundle();
        autoBarcodeProps.putString("configure_all_scanners", "true");
        autoBarcodeProps.putString("decoding_led_feedback", "true");
        autoBarcodeProps.putString("decode_haptic_feedback", "true");
        barcodeConfig.putBundle("PARAM_LIST", autoBarcodeProps);
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig);

        // Associate profile with this app
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", getPackageName());
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});
        profileConfig.remove("PLUGIN_CONFIG");

        // Apply configs
        // Use SET_CONFIG: http://techdocs.zebra.com/datawedge/latest/guide/api/setconfig/
        sendDataWedgeIntentWithExtra(EXTRA_SET_CONFIG, profileConfig);

        // Configure intent output for captured data to be sent to this app
        Bundle intentConfig = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT");
        intentConfig.putString("RESET_CONFIG", "true");
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", "com.wb.zebrascan.ACTION");
        intentProps.putString("intent_delivery", "2");
        intentConfig.putBundle("PARAM_LIST", intentProps);
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig);
        sendDataWedgeIntentWithExtra(EXTRA_SET_CONFIG, profileConfig);

        Log.i(LOG_TAG, "Created profile"); }

    /**
     * Toggle soft scan trigger from UI onClick() event
     *  Use SOFT_SCAN_TRIGGER: http://techdocs.zebra.com/datawedge/latest/guide/api/softscantrigger/
     *  From Zebra's DataCapture1 demo application
     * @author Zebra
     */
    public void ToggleSoftScanTrigger (View view){
        sendDataWedgeIntentWithExtra(EXTRA_SOFT_SCAN_TRIGGER, "TOGGLE_SCANNING");
    }

    /**
     * Toggles the selected scanner between the built-in and camera
     * @author Chandler Cunningham
     */
    public void ToggleSelectedScanner(View view) {
        ImageButton imageButton = findViewById(R.id.toggleButton);
        if (CameraSelectedScanner) {
            imageButton.setImageResource(R.drawable.ic_barcode);
            sendDataWedgeIntentWithExtra(EXTRA_SWITCH_SCANNER_EX, "AUTO");
            CameraSelectedScanner = false;
            Toast toast = Toast.makeText(getApplicationContext(), "Switched to Scanner", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0 ,50);
            toast.show();
        } else {
            imageButton.setImageResource(R.drawable.ic_camera_alt_black_24dp);
            sendDataWedgeIntentWithExtra(EXTRA_SWITCH_SCANNER_EX, "INTERNAL_CAMERA");
            CameraSelectedScanner = true;
            Toast toast = Toast.makeText(getApplicationContext(), "Switched to Camera", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0 ,50);
            toast.show();
        }
    }

    /**
     *  Create filter for the broadcast intent
     *  From Zebra's DataCapture1 demo application
     *  @author Zebra
     */
    private void registerReceivers() {

        Log.d(LOG_TAG, "registerReceivers()");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESULT_NOTIFICATION);   // for notification result
        filter.addAction(ACTION_RESULT);                // for error code result
        filter.addCategory(Intent.CATEGORY_DEFAULT);    // needed to get version info

        // register to received broadcasts via DataWedge scanning
        filter.addAction(getResources().getString(R.string.activity_intent_filter_action));
        filter.addAction(getResources().getString(R.string.activity_action_from_service));
        registerReceiver(myBroadcastReceiver, filter);
    }

    /**
     * Unregister scanner status notification
     * From Zebra's DataCapture1 demo application
     * @author Zebra
     */
    public void unRegisterScannerStatus() {
        Log.d(LOG_TAG, "unRegisterScannerStatus()");
        Bundle b = new Bundle();
        b.putString(EXTRA_KEY_APPLICATION_NAME, getPackageName());
        b.putString(EXTRA_KEY_NOTIFICATION_TYPE, EXTRA_KEY_VALUE_SCANNER_STATUS);
        Intent i = new Intent();
        i.setAction(ACTION);
        i.putExtra(EXTRA_UNREGISTER_NOTIFICATION, b);
        this.sendBroadcast(i);
    }

    /**
     * Handles received intents based on their content. Uses listeners registered in earlier
     * methods
     * From Zebra's DataCapture1 demo application
     * @author Zebra
     */
    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "DataWedge Action:" + action);

            // Get DataWedge version info
            if (intent.hasExtra(EXTRA_RESULT_GET_VERSION_INFO))
            {
                Bundle versionInfo = intent.getBundleExtra(EXTRA_RESULT_GET_VERSION_INFO);
                assert versionInfo != null;
                String DWVersion = versionInfo.getString("DATAWEDGE");
                Log.i(LOG_TAG, "DataWedge Version: " + DWVersion);
            }

            assert action != null;
            if (action.equals(getResources().getString(R.string.activity_intent_filter_action)))
            {
                //  Received a barcode scan
                try
                {
                    sendScanResult(intent);
                }
                catch (Exception e)
                {
                    //  Catch error if the UI does not exist when we receive the broadcast...
                }
            }

            else if (action.equals(ACTION_RESULT))
            {
                // Register to receive the result code
                if ((intent.hasExtra(EXTRA_RESULT)) && (intent.hasExtra(EXTRA_COMMAND)))
                {
                    String command = intent.getStringExtra(EXTRA_COMMAND);
                    String result = intent.getStringExtra(EXTRA_RESULT);
                    StringBuilder info = new StringBuilder();

                    if (intent.hasExtra(EXTRA_RESULT_INFO))
                    {
                        Bundle result_info = intent.getBundleExtra(EXTRA_RESULT_INFO);
                        assert result_info != null;
                        Set<String> keys = result_info.keySet();
                        for (String key : keys) {
                            Object object = result_info.get(key);
                            if (object instanceof String) {
                                info.append(key).append(": ").append(object).append("\n");
                            } else if (object instanceof String[]) {
                                String[] codes = (String[]) object;
                                for (String code : codes) {
                                    info.append(key).append(": ").append(code).append("\n");
                                }
                            }
                        }
                        Log.d(LOG_TAG, "Command: "+command+"\n" +
                                "Result: " +result+"\n" +
                                "Result Info: " + info + "\n");
                        Toast.makeText(getApplicationContext(), "Error Resulted. Command:" + command + "\nResult: " + result + "\nResult Info: " +info, Toast.LENGTH_LONG).show();
                    }
                }

            }

            // Register for scanner change notification
            else if (action.equals(ACTION_RESULT_NOTIFICATION))
            {
                if (intent.hasExtra(EXTRA_RESULT_NOTIFICATION))
                {
                    Bundle extras = intent.getBundleExtra(EXTRA_RESULT_NOTIFICATION);
                    assert extras != null;
                    String notificationType = extras.getString(EXTRA_RESULT_NOTIFICATION_TYPE);
                    if (notificationType != null)
                    {
                        switch (notificationType) {
                            case EXTRA_KEY_VALUE_SCANNER_STATUS:
                                // Change in scanner status occurred
                                String displayScannerStatusText = extras.getString(EXTRA_KEY_VALUE_NOTIFICATION_STATUS) +
                                        ", profile: " + extras.getString(EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME);
                                //Toast.makeText(getApplicationContext(), displayScannerStatusText, Toast.LENGTH_SHORT).show();
                                Log.i(LOG_TAG, "Scanner status: " + displayScannerStatusText);
                                break;

                            case EXTRA_KEY_VALUE_PROFILE_SWITCH:
                                // Received change in profile
                                // For future enhancement
                                break;

                            case  EXTRA_KEY_VALUE_CONFIGURATION_UPDATE:
                                // Configuration change occurred
                                // For future enhancement
                                break;
                        }
                    }
                }
            }
        }
    };

    /**
     * Sets JavaScript event for RocketRez to catch
     * Modified from Zebra's DataCapture1 demo application
     * @author Chandler Cunningham, Zebra
     * @param initiatingIntent intent that fired the method
     */
    private void sendScanResult(Intent initiatingIntent)
    {
        // store decoded data
        String decodedData = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));
        final WebView mainWebView = findViewById(R.id.webView);
        // Set barcode date to window event
        //TODO: Set this event trigger to one specific to Android in RocketRez
        mainWebView.loadUrl("javascript:window.onScanAppBarCodeData(\"" + decodedData + "\",\"\",\"\")");
    }

    /**
     * Sends intent to DataWedge with extra bundle
     * From Zebra's DataCapture1 demo application
     * @author Zebra
     * @param extraKey extra key
     * @param extras extras as bundle
     */
    private void sendDataWedgeIntentWithExtra(String extraKey, Bundle extras)
    {
        Intent dwIntent = new Intent();
        dwIntent.setAction(MainActivity.ACTION_DATAWEDGE);
        dwIntent.putExtra(extraKey, extras);
        if (bRequestSendResult)
            dwIntent.putExtra(EXTRA_SEND_RESULT, "true");
        this.sendBroadcast(dwIntent);
    }

    /**
     * Sends intent to DataWedge with extra string
     * From Zebra's DataCapture1 demo application
     * @author Zebra
     * @param extraKey extra key
     * @param extraValue extra as string
     */
    private void sendDataWedgeIntentWithExtra(String extraKey, String extraValue)
    {
        Intent dwIntent = new Intent();
        dwIntent.setAction(MainActivity.ACTION_DATAWEDGE);
        dwIntent.putExtra(extraKey, extraValue);
        if (bRequestSendResult)
            dwIntent.putExtra(EXTRA_SEND_RESULT, "true");
        this.sendBroadcast(dwIntent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceivers();
        // Set toggle button appropriately.
        ImageButton imageButton = findViewById(R.id.toggleButton);
        if (CameraSelectedScanner) {
            imageButton.setImageResource(R.drawable.ic_camera_alt_black_24dp);
            sendDataWedgeIntentWithExtra(EXTRA_SWITCH_SCANNER_EX, "INTERNAL_CAMERA");
        } else {
            imageButton.setImageResource(R.drawable.ic_barcode);
            sendDataWedgeIntentWithExtra(EXTRA_SWITCH_SCANNER_EX, "AUTO");
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(myBroadcastReceiver);
        unRegisterScannerStatus();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

}

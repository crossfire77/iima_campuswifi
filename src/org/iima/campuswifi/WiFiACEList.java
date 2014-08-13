/* Copyright 2010 OddRain
 * Copyright 2010 marcus905 <marcus90@gmail.com>
 * Copyright 2012 Rahul Yashwant Doiphode <a12rahuld@iimahd.ernet.in>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/* Changelog:
 * ----------
 * 
 * v1.0 - 20121127 - initial release
 * v1.1 - 20121226 - updated checking for username/password fields, incorporated checking android OS version, fixed crash on exit on JellyBean devices
 * v1.2 - 20130413 - market release
 * v1.3 - 20130701 - setting up new wifimobile access point
 * v1.4 - 20130917 - setting up new wifistudent access point
 * 
 */

package org.iima.campuswifi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.net.ProxyProperties;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WiFiACEList extends Activity implements
		OnSharedPreferenceChangeListener {

	private static final String INT_PRIVATE_KEY = "private_key";
	private static final String INT_PHASE2 = "phase2";
	private static final String INT_PASSWORD = "password";
	private static final String INT_IDENTITY = "identity";
	private static final String INT_EAP = "eap";
	private static final String INT_CLIENT_CERT = "client_cert";
	private static final String INT_CA_CERT = "ca_cert";
	private static final String INT_ANONYMOUS_IDENTITY = "anonymous_identity";
	private static final String INT_ENTERPRISEFIELD_NAME = "android.net.wifi.WifiConfiguration$EnterpriseField";
	protected static final int SHOW_PREFERENCES = 0;
	private WifiManager wifiManager;
	private ListView aceListView;
	private List<WifiConfiguration> aceList;
	private WifiConfiguration selectedConfig;
	private WiFiACEConfigAdapter aceAdapter;
	private boolean editingPrefs = false;

	
    public void createShowExtWarnDialog(){
    	Log.i(getPackageName(), "EXT Warning Dialog");
    	
    	String ExtWarnErr = "";
		
    	this.ExtWarningDialog("WARNING!", "\nOpen WiFi settings and remove any saved access points like: "
    			+ "wifimobile, wifistudent, wifistaff, wififaculty, wifiguest."
    			+ "To remove these access points: LONG press on the name of the saved access point and click 'Forget Network'"
				+ "\n\n" + "Click OK to continue"
				+ "\n" + "Click CANCEL to close application");
    	
    	
    	Log.e(getPackageName(), ExtWarnErr);

    }
    
	private boolean ExtWarningDialog(final String title, final String message) {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(title)
				.setCancelable(false)
				.setMessage(message)
				.setNegativeButton(android.R.string.no, 
						new DialogInterface.OnClickListener() {
                    		public void onClick(DialogInterface dialog, int which) {
                    		
                    			WiFiACEList.this.finish();
                    		
                    		}
                		})
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
								final int which) {
									try {
										
										// do nothing (will close dialog)
										
									} catch (Exception e) {
										e.printStackTrace();
										Log.e(getPackageName(), e.getStackTrace().toString());
									}
							}
						}).show();
		return true;
	}
	
	
	private void editConfig(WifiConfiguration selectedConfig){


		// Populate Preferences
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		prefs
				.registerOnSharedPreferenceChangeListener(WiFiACEList.this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.clear();

		if (selectedConfig.SSID != null) {
			editor.putString(WiFiACESettings.PREF_SSID,
					selectedConfig.SSID.replaceAll("\"", ""));
		}

		if (selectedConfig.BSSID != null) {
			editor.putString(WiFiACESettings.PREF_BSSID,
					selectedConfig.BSSID);
		}

		editor.putBoolean(WiFiACESettings.PREF_HIDDEN_SSID,
				selectedConfig.hiddenSSID);

		editor.putBoolean(WiFiACESettings.PREF_KEY_NONE,
				selectedConfig.allowedKeyManagement
						.get(WifiConfiguration.KeyMgmt.NONE));
		editor.putBoolean(WiFiACESettings.PREF_KEY_PSK,
				selectedConfig.allowedKeyManagement
						.get(WifiConfiguration.KeyMgmt.WPA_PSK));
		editor.putBoolean(WiFiACESettings.PREF_KEY_EAP,
				selectedConfig.allowedKeyManagement
						.get(WifiConfiguration.KeyMgmt.WPA_EAP));
		editor.putBoolean(WiFiACESettings.PREF_KEY_IEEE,
				selectedConfig.allowedKeyManagement
						.get(WifiConfiguration.KeyMgmt.IEEE8021X));

		editor.putBoolean(WiFiACESettings.PREF_AUTH_OPEN,
				selectedConfig.allowedAuthAlgorithms
						.get(WifiConfiguration.AuthAlgorithm.OPEN));
		editor.putBoolean(WiFiACESettings.PREF_AUTH_LEAP,
				selectedConfig.allowedAuthAlgorithms
						.get(WifiConfiguration.AuthAlgorithm.LEAP));
		editor.putBoolean(WiFiACESettings.PREF_AUTH_SHARED,
				selectedConfig.allowedAuthAlgorithms
						.get(WifiConfiguration.AuthAlgorithm.SHARED));

		editor.putBoolean(WiFiACESettings.PREF_SEC_WPA,
				selectedConfig.allowedProtocols
						.get(WifiConfiguration.Protocol.WPA));
		editor.putBoolean(WiFiACESettings.PREF_SEC_RSN,
				selectedConfig.allowedProtocols
						.get(WifiConfiguration.Protocol.RSN));

		editor.putBoolean(WiFiACESettings.PREF_PAIR_NONE,
				selectedConfig.allowedPairwiseCiphers
						.get(WifiConfiguration.PairwiseCipher.NONE));
		editor.putBoolean(WiFiACESettings.PREF_PAIR_CCMP,
				selectedConfig.allowedPairwiseCiphers
						.get(WifiConfiguration.PairwiseCipher.CCMP));
		editor.putBoolean(WiFiACESettings.PREF_PAIR_TKIP,
				selectedConfig.allowedPairwiseCiphers
						.get(WifiConfiguration.PairwiseCipher.TKIP));

		editor.putBoolean(WiFiACESettings.PREF_GRP_WEP40,
				selectedConfig.allowedGroupCiphers
						.get(WifiConfiguration.GroupCipher.WEP40));
		editor.putBoolean(WiFiACESettings.PREF_GRP_WEP104,
				selectedConfig.allowedGroupCiphers
						.get(WifiConfiguration.GroupCipher.WEP104));
		editor.putBoolean(WiFiACESettings.PREF_GRP_TKIP,
				selectedConfig.allowedGroupCiphers
						.get(WifiConfiguration.GroupCipher.TKIP));
		editor.putBoolean(WiFiACESettings.PREF_GRP_CCMP,
				selectedConfig.allowedGroupCiphers
						.get(WifiConfiguration.GroupCipher.CCMP));

		if (selectedConfig.wepTxKeyIndex > 3
				|| selectedConfig.wepTxKeyIndex < 0) {
			editor.putInt(WiFiACESettings.PREF_WEPKEY_IDX, 
					selectedConfig.wepTxKeyIndex);
		}

		if (selectedConfig.wepKeys[0] != null
				&& selectedConfig.wepKeys[0].length() >= 2) {
			editor.putString(WiFiACESettings.PREF_WEPKEY_KEY0,
					removeQuotes(selectedConfig.wepKeys[0]));
		}

		if (selectedConfig.wepKeys[1] != null
				&& selectedConfig.wepKeys[1].length() >= 2) {
		editor.putString(WiFiACESettings.PREF_WEPKEY_KEY1,
					removeQuotes(selectedConfig.wepKeys[1]));
		}

		if (selectedConfig.wepKeys[2] != null
				&& selectedConfig.wepKeys[2].length() >= 2) {
			editor.putString(WiFiACESettings.PREF_WEPKEY_KEY2,
					removeQuotes(selectedConfig.wepKeys[2]));
		}

		if (selectedConfig.wepKeys[3] != null
				&& selectedConfig.wepKeys[3].length() >= 2) {
			editor.putString(WiFiACESettings.PREF_WEPKEY_KEY3,
					removeQuotes(selectedConfig.wepKeys[3]));
		}

		if (selectedConfig.preSharedKey != null
				&& selectedConfig.preSharedKey.length() >= 2) {
			editor.putString(WiFiACESettings.PREF_WPA_KEY,
					removeQuotes(selectedConfig.preSharedKey));
		}

		// Reflection magic needs to be done here to access non-public
		// APIs
		// Also here new ad-hoc switch for CM6 users
		
		try {
			// Let the magic start
			Class[] wcClasses = WifiConfiguration.class.getClasses();
			// null for overzealous java compiler
			Class wcEnterpriseField = null;

			for (Class wcClass : wcClasses)
				if (wcClass
						.getName()
						.equals(
								INT_ENTERPRISEFIELD_NAME)) {
					wcEnterpriseField = wcClass;
					break;
				}
			boolean noEnterpriseFieldType = false; 
			if(wcEnterpriseField == null)
				noEnterpriseFieldType = true; // Cupcake/Donut access enterprise settings directly

			// I know there is enterpriseFields but I haven't
			// gotten around it yet
			// nulls here to workaround the overzealous java compiler
			Field wcefAnonymousId = null, wcefCaCert = null, wcefClientCert = null, wcefEap = null, wcefIdentity = null, wcefPassword = null, wcefPhase2 = null, wcefPrivateKey = null;
			Field[] wcefFields = WifiConfiguration.class.getFields();
			// Dispatching Field vars
			for (Field wcefField : wcefFields) {
				if (wcefField
						.getName().trim()
						.equals(
								INT_ANONYMOUS_IDENTITY))
					wcefAnonymousId = wcefField;
				else if (wcefField.getName().trim().equals(
						INT_CA_CERT))
					wcefCaCert = wcefField;
				else if (wcefField
						.getName().trim()
						.equals(
								INT_CLIENT_CERT))
					wcefClientCert = wcefField;
				else if (wcefField.getName().trim().equals(
						INT_EAP))
					wcefEap = wcefField;
				else if (wcefField.getName().trim().equals(
						INT_IDENTITY))
					wcefIdentity = wcefField;
				else if (wcefField.getName().trim().equals(
						INT_PASSWORD))
					wcefPassword = wcefField;
				else if (wcefField.getName().trim().equals(
						INT_PHASE2))
					wcefPhase2 = wcefField;
				else if (wcefField
						.getName().trim()
						.equals(
								INT_PRIVATE_KEY))
					wcefPrivateKey = wcefField;
			}
			
			Method wcefValue = null;
			if(!noEnterpriseFieldType){
			for(Method m: wcEnterpriseField.getMethods())
				//System.out.println(m.getName());
				if(m.getName().trim().equals("value")){
					wcefValue = m;
				break;
			}
			}
			// if (selectedConfig.eap.value() != null) {
			
			String tVal = null;
			if(noEnterpriseFieldType)
				tVal = (String) wcefEap.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefEap.get(selectedConfig), null);
			
			if (tVal != null) {
				editor.putString(WiFiACESettings.PREF_ENTERPRISE_EAP,
				/* selectedConfig.eap.value() */
				tVal);
			} 

			// if (selectedConfig.phase2.value() != null) {
			
			if(noEnterpriseFieldType)
				tVal = (String) wcefPhase2.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefPhase2.get(selectedConfig), null);

			if (tVal != null) {
				editor.putString(
						WiFiACESettings.PREF_ENTERPRISE_PHASE2,
						removeQuotes(tVal));
			}

			// if (selectedConfig.identity.value() != null) {
			if(noEnterpriseFieldType)
				tVal = (String) wcefIdentity.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefIdentity.get(selectedConfig), null);

			if (tVal != null) {
				editor.putString(WiFiACESettings.PREF_ENTERPRISE_IDENT,
						removeQuotes(tVal));
			}

			// if (selectedConfig.anonymous_identity.value() != null) {
			if(noEnterpriseFieldType)
				tVal = (String) wcefAnonymousId.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefAnonymousId.get(selectedConfig), null);

			if (tVal != null) {
				editor.putString(
						WiFiACESettings.PREF_ENTERPRISE_ANON_IDENT,
						removeQuotes(tVal));
			}

			// if (selectedConfig.password.value() != null) {
			if(noEnterpriseFieldType)
				tVal = (String) wcefPassword.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefPassword.get(selectedConfig), null);

			if (tVal != null) {
				editor.putString(WiFiACESettings.PREF_ENTERPRISE_PASS,
						removeQuotes(tVal));
			}

			// if (selectedConfig.client_cert.value() != null &&
			// selectedConfig.client_cert.value().length() >= 2
			if(noEnterpriseFieldType)
				tVal = (String) wcefClientCert.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefClientCert.get(selectedConfig), null);

			if (tVal != null && tVal.length() >= 2) {
				editor.putString(
						WiFiACESettings.PREF_ENTERPRISE_CLIENT_CERT,
						removeQuotes(tVal));
			}

			// if (selectedConfig.ca_cert.value() != null &&
			// selectedConfig.ca_cert.value().length() >= 2) {
			if(noEnterpriseFieldType)
				tVal = (String) wcefCaCert.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefCaCert.get(selectedConfig), null);

			if (tVal != null && tVal.length() >= 2) {
				editor.putString(
						WiFiACESettings.PREF_ENTERPRISE_CA_CERT,
						removeQuotes(tVal));
			}

			// if (selectedConfig.private_key.value() != null &&
			// selectedConfig.private_key.value().length() >= 2) {
			if(noEnterpriseFieldType)
				tVal = (String) wcefPrivateKey.get(selectedConfig);
			else
				tVal = (String) wcefValue.invoke(wcefPrivateKey.get(selectedConfig), null);

			if (tVal != null && tVal.length() >= 2) {
				editor.putString(
						WiFiACESettings.PREF_ENTERPRISE_PRIV_KEY,
						removeQuotes(tVal));
			}
			
			// Adhoc for CM6
			// nested try-catch for graceful fail.
			try{
			Field wcAdhoc = WifiConfiguration.class.getField("adhocSSID");
			Field wcAdhocFreq = WifiConfiguration.class.getField("frequency");
			editor.putBoolean(WiFiACESettings.PREF_ADHOC,
					wcAdhoc.getBoolean(selectedConfig));
			editor.putString(WiFiACESettings.PREF_ADHOC_FREQUENCY,
					Integer.toString(wcAdhocFreq.getInt(selectedConfig)));
			}catch(Exception e){
				e.printStackTrace();
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// FIXME Not used to Android, what should I do here?
			e.printStackTrace();
		}

	
		// FIXME Up to here converted 8 errors in 19 warnings.

		editor.commit();
		editingPrefs = true;
					
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		checkSDKVersion();
		
		setContentView(R.layout.main);
		
		// Setup WiFi
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		aceListView = (ListView) findViewById(R.id.aceList);

		aceListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> _av, View _v, int _index,
					long arg3) {
				selectedConfig = aceList.get(_index);

				Context context = getApplicationContext();
					
				editConfig(selectedConfig);
				
				// Display Preferences
				Intent i = new Intent(context, WiFiACESettings.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivityForResult(i, SHOW_PREFERENCES);
			}
		});

		aceList = new ArrayList<WifiConfiguration>();
		int resID = R.layout.wifi_ace_config_item;
		aceAdapter = new WiFiACEConfigAdapter(this, resID, aceList);
		aceListView.setAdapter(aceAdapter);

		Context context = getApplicationContext();
		context.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				//loadWifiConfigs();
				System.out.println("Recieved wifi state changed action:"
						+ intent);
			}
		}, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

		checkWifiState();
		
		createShowExtWarnDialog();
		
	}
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		
		switch (item.getItemId()) {
	    case R.id.about:
	    	Builder builder = new AlertDialog.Builder(this);
			
	    	PackageInfo pi = null;
	    	try{
	    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    		}
	    	builder.setTitle(getString(R.string.ABOUT_TITLE));
	    	builder.setMessage(getString(R.string.app_name)+
	    			"\n\nV"+pi.versionName+"C"+pi.versionCode+
	    			"\n\n"+getString(R.string.ABOUT_CONTENT));
			builder.setPositiveButton(getString(android.R.string.ok), null);
			builder.show();
	        return true;
	    }
	    return false;
	}
*/
	private void loadWifiConfigs() {
		Log.i(getPackageName(),"loading WifiConfigs");
		aceList.clear();
		List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
		for (WifiConfiguration config : configs) {
			aceList.add(config);
		}
		aceAdapter.notifyDataSetChanged();
	}
	
	private void clearWifiConfigs() {
		Log.i(getPackageName(),"clearing WifiConfigs");

		List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
		int cnt=0;
		for (WifiConfiguration config : configs) {
			cnt=cnt+1;
			if (config.SSID == "wifimobile") {
				Toast.makeText(getBaseContext(), "removing wifimobile", Toast.LENGTH_LONG).show();
				wifiManager.removeNetwork(cnt);
			}
		}

	}

	private void checkWifiState() {
		boolean enabled = wifiManager.isWifiEnabled();
		if (!enabled) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.app_name));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(getString(R.string.WIFI_ENABLE_MSG));
			builder.setPositiveButton(getString(R.string.YES),
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							wifiManager.setWifiEnabled(true);
						}
					});
			builder.setNegativeButton(getString(R.string.NO), null);
			builder.show();
		} else {
			//loadWifiConfigs();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		checkWifiState();
		editingPrefs = false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (editingPrefs) {
			// Save
			saveWiFiConfig();
		}
	}

	@SuppressLint("NewApi")
	public void saveTestWiFiConfig(View view) throws Exception  {
		// Save TEST Wifi Config from Preferences
		
		// >>> CORE CODE IS HERE, now just design a UI around it! <<<
		

		Log.i(getPackageName(),"Core: started");
				
		EditText et_username = (EditText)findViewById(R.id.username);
		EditText et_password = (EditText)findViewById(R.id.password);
		Spinner sp_wifiap = (Spinner)findViewById(R.id.wifiap);
		
		String username = et_username.getText().toString();
		String password = et_password.getText().toString();
		String wifiap = sp_wifiap.getSelectedItem().toString();

		if(username.isEmpty() || password.isEmpty() ) {

			Builder builder = new AlertDialog.Builder(this);

			Log.i(getPackageName(),"EmptyBox: displayed");
			
	    	PackageInfo pi = null;
	    	try{
	    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    		}
	    	builder.setTitle("Warning!");
	    	builder.setMessage("Please enter the username and password.");
			builder.setPositiveButton(getString(android.R.string.ok), null);
			builder.show();

		} else {
				
		//selectedConfig = aceList.get(0);
		
	    WifiConfiguration newConfig = new WifiConfiguration(); 
		
		newConfig.SSID = surroundWithQuotes("wifistudent");
	    // for AP selection
		// newConfig.SSID = surroundWithQuotes(wifiap);
		newConfig.hiddenSSID = true;
			
		// key management
		newConfig.allowedKeyManagement.clear();
		newConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.IEEE8021X);
		newConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_EAP);
				
		// Authentication Algorithms
		newConfig.allowedAuthAlgorithms.clear();
		
		// GroupCiphers
		newConfig.allowedGroupCiphers.clear();
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP40);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP104);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
		
		// PairwiseCiphers
		newConfig.allowedPairwiseCiphers.clear();
		newConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
		newConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
		
		// Protocols
		newConfig.allowedProtocols.clear();
		newConfig.allowedProtocols
					.set(WifiConfiguration.Protocol.RSN);
		newConfig.allowedProtocols
					.set(WifiConfiguration.Protocol.WPA);

		/*
		// for AP selection
		newConfig.SSID = surroundWithQuotes(wifiap);
		newConfig.hiddenSSID = true;
			
		// key management
		newConfig.allowedKeyManagement.clear();
		newConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.IEEE8021X);
		

		// GroupCiphers
		newConfig.allowedGroupCiphers.clear();
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP40);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP104);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
		newConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
		
		// PairwiseCiphers
		newConfig.allowedPairwiseCiphers.clear();
		newConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
		newConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
		newConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.NONE);
		
		// Authentication Algorithms
		newConfig.allowedAuthAlgorithms.clear();
		newConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);
		newConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.SHARED);
		newConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.LEAP);
		
		// Protocols
		newConfig.allowedProtocols.clear();
		newConfig.allowedProtocols
					.set(WifiConfiguration.Protocol.RSN);
		newConfig.allowedProtocols
					.set(WifiConfiguration.Protocol.WPA);
		 */
		
		// Enterprise configuration
		// Reflection magic here too, need access to non-public APIs
		// Used also to access CM6 adhoc support

		try {
			// Let the magic start
			Class[] wcClasses = WifiConfiguration.class.getClasses();
			// null for overzealous java compiler
			Class wcEnterpriseField = null;

			for (Class wcClass : wcClasses)
				if (wcClass.getName().equals(
						INT_ENTERPRISEFIELD_NAME)) {
					wcEnterpriseField = wcClass;
					break;
				}
			boolean noEnterpriseFieldType = false; 
			if(wcEnterpriseField == null)
				noEnterpriseFieldType = true; // Cupcake/Donut access enterprise settings directly
			// I know there is enterpriseFields but I haven't
			// gotten around it yet
			// nulls here to workaround the overzealous java compiler
			Field wcefAnonymousId = null, wcefCaCert = null, wcefClientCert = null, wcefEap = null, wcefIdentity = null, wcefPassword = null, wcefPhase2 = null, wcefPrivateKey = null;
			Field[] wcefFields = WifiConfiguration.class.getFields();
			// Dispatching Field vars
			for (Field wcefField : wcefFields) {
				if (wcefField
						.getName()
						.equals(
								INT_ANONYMOUS_IDENTITY))
					wcefAnonymousId = wcefField;
				else if (wcefField.getName().equals(
						INT_CA_CERT))
					wcefCaCert = wcefField;
				else if (wcefField.getName().equals(
						INT_CLIENT_CERT))
					wcefClientCert = wcefField;
				else if (wcefField.getName().equals(
						INT_EAP))
					wcefEap = wcefField;
				else if (wcefField.getName().equals(
						INT_IDENTITY))
					wcefIdentity = wcefField;
				else if (wcefField.getName().equals(
						INT_PASSWORD))
					wcefPassword = wcefField;
				else if (wcefField.getName().equals(
						INT_PHASE2))
					wcefPhase2 = wcefField;
				else if (wcefField.getName().equals(
						INT_PRIVATE_KEY))
					wcefPrivateKey = wcefField;
			}
			
			
			Method wcefSetValue = null;
			if(!noEnterpriseFieldType){
			for(Method m: wcEnterpriseField.getMethods())
				//System.out.println(m.getName());
				if(m.getName().trim().equals("setValue"))
					wcefSetValue = m;
			}
			
						
			
			wcefSetValue.invoke(wcefEap.get(newConfig), "PEAP");
			wcefSetValue.invoke(wcefPhase2.get(newConfig), "auth=MSCHAPV2");
			
			wcefSetValue.invoke(wcefIdentity.get(newConfig), username);
			wcefSetValue.invoke(wcefPassword.get(newConfig), password);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// FIXME As above, what should I do here?
			e.printStackTrace();
		}


		//wifiManager.updateNetwork(newConfig);
		
		// add proxy info

		Log.i(getPackageName(),"Core: adding proxy information");
		newConfig.ipAssignment = WifiConfiguration.IpAssignment.UNASSIGNED;
		newConfig.proxySettings = WifiConfiguration.ProxySettings.STATIC;
		newConfig.linkProperties.clear();
		newConfig.linkProperties.setHttpProxy(new ProxyProperties("192.168.32.4", 8080, ""));
		

		// Save Wifi Config from Preferences
		Context context = getApplicationContext();
		Toast.makeText(context, "Saving Settings", Toast.LENGTH_LONG).show();
		
		// save network
		Log.i(getPackageName(),"Core: saving");
		wifiManager.addNetwork(newConfig);
		wifiManager.enableNetwork(newConfig.networkId, false);
		wifiManager.saveConfiguration();
		
		Log.i(getPackageName(),"Core: saved");
		
		Log.i(getPackageName(),"Core: disabling WiFi");
		wifiManager.setWifiEnabled(false);
		Log.i(getPackageName(),"Core: enabling WiFi");
		wifiManager.setWifiEnabled(true);
		
		
		try {
			showExitBox();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i(getPackageName(),"Core: DONE! quitting application");

		//WiFiACEList.this.finish();
		
		}
		
	}
	
	
	private void saveWiFiConfig() {
		// Save Wifi Config from Preferences
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		String slidingTemp = prefs.getString(WiFiACESettings.PREF_SSID, null);
		if (slidingTemp != null) {
			selectedConfig.SSID = surroundWithQuotes(slidingTemp);
		}
		slidingTemp = prefs.getString(WiFiACESettings.PREF_BSSID, null);
		if (slidingTemp != null && 
				slidingTemp.length() == 17 && // avoid regex matching if can't be a macaddr
				slidingTemp.matches(
						"[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}")
						) {
			selectedConfig.BSSID = slidingTemp;
		}

		if (!prefs.getBoolean(WiFiACESettings.PREF_HIDDEN_SSID, false)) {
			selectedConfig.hiddenSSID = false;
		} else {
			selectedConfig.hiddenSSID = true;
		}

		selectedConfig.allowedKeyManagement.clear();
		if (prefs.getBoolean(WiFiACESettings.PREF_KEY_IEEE, false)) {
			selectedConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.IEEE8021X);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_KEY_PSK, false)) {
			selectedConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_KEY_EAP, false)) {
			selectedConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_EAP);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_KEY_NONE, false)) {
			selectedConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.NONE);
		}

		// GroupCiphers
		selectedConfig.allowedGroupCiphers.clear();
		if (prefs.getBoolean(WiFiACESettings.PREF_GRP_WEP40, false)) {
			selectedConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP40);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_GRP_WEP104, false)) {
			selectedConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP104);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_GRP_CCMP, false)) {
			selectedConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_GRP_TKIP, false)) {
			selectedConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
		}

		// PairwiseCiphers
		selectedConfig.allowedPairwiseCiphers.clear();
		if (prefs.getBoolean(WiFiACESettings.PREF_PAIR_TKIP, false)) {
			selectedConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_PAIR_CCMP, false)) {
			selectedConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_PAIR_NONE, false)) {
			selectedConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.NONE);
		}

		// Authentication Algorithms
		selectedConfig.allowedAuthAlgorithms.clear();
		if (prefs.getBoolean(WiFiACESettings.PREF_AUTH_OPEN, false)) {
			selectedConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_AUTH_SHARED, false)) {
			selectedConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.SHARED);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_AUTH_LEAP, false)) {
			selectedConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.LEAP);
		}

		// Protocols
		selectedConfig.allowedProtocols.clear();
		if (prefs.getBoolean(WiFiACESettings.PREF_SEC_RSN, false)) {
			selectedConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		}
		if (prefs.getBoolean(WiFiACESettings.PREF_SEC_WPA, false)) {
			selectedConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		}

		// WEP Keys
		String pIdx = prefs.getString(WiFiACESettings.PREF_WEPKEY_IDX, "-1");
		//System.err.println(pIdx);
		int idx = Integer.parseInt(pIdx);
		if (!(idx < 0 || idx > 3))
			selectedConfig.wepTxKeyIndex = idx;


		slidingTemp = prefs.getString(WiFiACESettings.PREF_WEPKEY_KEY0, null);
		if (slidingTemp != null) {
			switch(slidingTemp.length()){
			case 10:
			case 26:
			case 58:
				if(slidingTemp.matches("[0-9A-Fa-f]*"))
					selectedConfig.wepKeys[0] = slidingTemp;
				break;
			default:
			selectedConfig.wepKeys[0] = surroundWithQuotes(prefs.getString(
					WiFiACESettings.PREF_WEPKEY_KEY0, ""));
			}
		}
		slidingTemp = prefs.getString(WiFiACESettings.PREF_WEPKEY_KEY1, null);
		if (slidingTemp != null) {
			switch(slidingTemp.length()){
			case 10:
			case 26:
			case 58:
				if(slidingTemp.matches("[0-9A-Fa-f]*"))
					selectedConfig.wepKeys[1] = slidingTemp;
				break;
			default:
			selectedConfig.wepKeys[1] = surroundWithQuotes(prefs.getString(
					WiFiACESettings.PREF_WEPKEY_KEY1, ""));
			}
		}
		slidingTemp = prefs.getString(WiFiACESettings.PREF_WEPKEY_KEY2, null);
		if (slidingTemp != null) {
			switch(slidingTemp.length()){
			case 10:
			case 26:
			case 58:
				if(slidingTemp.matches("[0-9A-Fa-f]*"))
					selectedConfig.wepKeys[2] = slidingTemp;
				break;
			default:
			selectedConfig.wepKeys[2] = surroundWithQuotes(prefs.getString(
					WiFiACESettings.PREF_WEPKEY_KEY2, ""));
			}
		}
		slidingTemp = prefs.getString(WiFiACESettings.PREF_WEPKEY_KEY3, null);
		if (slidingTemp != null) {
			switch(slidingTemp.length()){
			case 10:
			case 26:
			case 58:
				if(slidingTemp.matches("[0-9A-Fa-f]*"))
					selectedConfig.wepKeys[3] = slidingTemp;
				break;
			default:
			selectedConfig.wepKeys[3] = surroundWithQuotes(prefs.getString(
					WiFiACESettings.PREF_WEPKEY_KEY3, ""));
			}
		}
		
		slidingTemp = prefs.getString(WiFiACESettings.PREF_WPA_KEY, null);
		if (slidingTemp != null) {
			if(slidingTemp.matches("[0-9A-Fa-f]{64}"))
				selectedConfig.preSharedKey = slidingTemp;
			else
				selectedConfig.preSharedKey = surroundWithQuotes(slidingTemp);
		}

		// Enterprise Settings
		// Reflection magic here too, need access to non-public APIs
		// Used also to access CM6 adhoc support
		// FIXME Make me pretty, as I'm uglier than ever before.

		try {
			// Let the magic start
			Class[] wcClasses = WifiConfiguration.class.getClasses();
			// null for overzealous java compiler
			Class wcEnterpriseField = null;

			for (Class wcClass : wcClasses)
				if (wcClass.getName().equals(
						INT_ENTERPRISEFIELD_NAME)) {
					wcEnterpriseField = wcClass;
					break;
				}
			boolean noEnterpriseFieldType = false; 
			if(wcEnterpriseField == null)
				noEnterpriseFieldType = true; // Cupcake/Donut access enterprise settings directly
			// I know there is enterpriseFields but I haven't
			// gotten around it yet
			// nulls here to workaround the overzealous java compiler
			Field wcefAnonymousId = null, wcefCaCert = null, wcefClientCert = null, wcefEap = null, wcefIdentity = null, wcefPassword = null, wcefPhase2 = null, wcefPrivateKey = null;
			Field[] wcefFields = WifiConfiguration.class.getFields();
			// Dispatching Field vars
			for (Field wcefField : wcefFields) {
				if (wcefField
						.getName()
						.equals(
								INT_ANONYMOUS_IDENTITY))
					wcefAnonymousId = wcefField;
				else if (wcefField.getName().equals(
						INT_CA_CERT))
					wcefCaCert = wcefField;
				else if (wcefField.getName().equals(
						INT_CLIENT_CERT))
					wcefClientCert = wcefField;
				else if (wcefField.getName().equals(
						INT_EAP))
					wcefEap = wcefField;
				else if (wcefField.getName().equals(
						INT_IDENTITY))
					wcefIdentity = wcefField;
				else if (wcefField.getName().equals(
						INT_PASSWORD))
					wcefPassword = wcefField;
				else if (wcefField.getName().equals(
						INT_PHASE2))
					wcefPhase2 = wcefField;
				else if (wcefField.getName().equals(
						INT_PRIVATE_KEY))
					wcefPrivateKey = wcefField;
			}
			
			
			Method wcefSetValue = null;
			if(!noEnterpriseFieldType){
			for(Method m: wcEnterpriseField.getMethods())
				//System.out.println(m.getName());
				if(m.getName().trim().equals("setValue"))
					wcefSetValue = m;
			}
			
			String tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_EAP,
					null);
			if (tVal != null) {
				// selectedConfig.eap.setValue(tVal, ""));
				if(!noEnterpriseFieldType)
				wcefSetValue.invoke(
						wcefEap.get(selectedConfig), tVal);
				else
					wcefEap.set(selectedConfig, tVal);

			}
			tVal = prefs
					.getString(WiFiACESettings.PREF_ENTERPRISE_PHASE2, null);
			if (tVal != null) {
				// selectedConfig.phase2.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_PHASE2, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefPhase2.get(selectedConfig), tVal);
					else
						wcefPhase2.set(selectedConfig, surroundWithQuotes(tVal));
			}

			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_IDENT, null);
			if (tVal != null) {
				// selectedConfig.identity.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_IDENT, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefIdentity.get(selectedConfig), tVal);
					else
						wcefIdentity.set(selectedConfig, surroundWithQuotes(tVal));

			}

			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_ANON_IDENT,
					null);
			if (tVal != null) {
				// selectedConfig.anonymous_identity
				// .setValue(convertToQuotedString(prefs.getString(
				// WiFiACESettings.PREF_ENTERPRISE_ANON_IDENT, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefAnonymousId.get(selectedConfig), tVal);
					else
						wcefAnonymousId.set(selectedConfig, surroundWithQuotes(tVal));

			}
			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_PASS, null);
			if (tVal != null) {
				// selectedConfig.password.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_PASS, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefPassword.get(selectedConfig), tVal);
					else
						wcefPassword.set(selectedConfig, surroundWithQuotes(tVal));

			}

			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_CLIENT_CERT,
					null);
			if (tVal != null) {
				// selectedConfig.client_cert.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_CLIENT_CERT,
				// "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefClientCert.get(selectedConfig), tVal);
					else
						wcefClientCert.set(selectedConfig, surroundWithQuotes(tVal));

			}

			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_CA_CERT,
					null);
			if (tVal != null) {
				// selectedConfig.ca_cert.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_CA_CERT, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefCaCert.get(selectedConfig), tVal);
					else
						wcefCaCert.set(selectedConfig, surroundWithQuotes(tVal));

			}

			tVal = prefs.getString(WiFiACESettings.PREF_ENTERPRISE_PRIV_KEY,
					null);
			if (tVal != null) {
				// selectedConfig.private_key.setValue(convertToQuotedString(prefs
				// .getString(WiFiACESettings.PREF_ENTERPRISE_PRIV_KEY, "")));
				if(!noEnterpriseFieldType)
					wcefSetValue.invoke(
							wcefPrivateKey.get(selectedConfig), tVal);
					else
						wcefPrivateKey.set(selectedConfig, surroundWithQuotes(tVal));

			}

			// Adhoc for CM6
			// if non-CM6 fails gracefully thanks to nested try-catch
			
			try{
			Field wcAdhoc = WifiConfiguration.class.getField("adhocSSID");
			Field wcAdhocFreq = WifiConfiguration.class.getField("frequency");
			wcAdhoc.setBoolean(selectedConfig, prefs.getBoolean(WiFiACESettings.PREF_ADHOC,
					false));
			int freq = Integer.parseInt(prefs.getString(WiFiACESettings.PREF_ADHOC_FREQUENCY,
					"2462")); 	// default to channel 11
			//System.err.println(freq);
			wcAdhocFreq.setInt(selectedConfig, freq); 
			} catch (Exception e){
				e.printStackTrace();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// FIXME As above, what should I do here?
			e.printStackTrace();
		}

		wifiManager.updateNetwork(selectedConfig);
		wifiManager.enableNetwork(selectedConfig.networkId, false);
		wifiManager.saveConfiguration();
		
	}

	static String removeQuotes(String str) {
		int len = str.length();
		if ((len > 1) && (str.charAt(0) == '"') && (str.charAt(len - 1) == '"')) {
			return str.substring(1, len - 1);
		}
		return str;
	}

	static String surroundWithQuotes(String string) {
		return "\"" + string + "\"";
	}
	
	public void toggleAdvConfig(View view) throws Exception  {

		ListView advConfigView = (ListView)findViewById(R.id.aceList);
		CheckBox advConfigToggle = (CheckBox)findViewById(R.id.advConfigToggle);
			
		//Context context = getApplicationContext();
	
		if (advConfigToggle.isChecked()==true) {
			//Toast.makeText(context, "IF", Toast.LENGTH_SHORT).show();
			advConfigView.setVisibility(0);
			Log.i(getPackageName(),"AdvConfig: Enabled");
			loadWifiConfigs();
		} else {
			//Toast.makeText(context, "ELSE", Toast.LENGTH_SHORT).show();
			advConfigView.setVisibility(8);
			Log.i(getPackageName(),"AdvConfig: Disabled");
		}
		
	}

	public void showHelpBox(View view) throws Exception  {
		
		Builder builder = new AlertDialog.Builder(this);
		
		Log.i(getPackageName(),"HelpBox: displayed");
		
    	PackageInfo pi = null;
    	try{
    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
    	}catch(Exception e){
    		e.printStackTrace();
    		}
    	builder.setTitle("Help");
    	builder.setMessage("This app is designed for Android OS version 4.0.3 (Ice Cream Sandwich) and above." +
    			"\n\nBefore you begin, remove any saved access points." +
    			"\n\nSteps to Setup: \n1) Select the access point to setup \n2) Enter the IIM-A username (without the @iimahd.ernet.in part) \n3) Enter your WiFi password \n4) Click on SAVE \n" +
    			"\n\n\nTroubleshooting: \n1) If you do not remember your password, please contact CCC. \n2) If you are getting \"Disconnected\" prompt try disabling and then re-enabling WiFi. \n3) If the device is still not able to connect, recheck the password, delete the access point (\"Forget Network\") from the Wifi Setting menu, restart app and do the setup again.");
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.show();
	}
	
	public void showAboutBox(View view) throws Exception  {
		
		Builder builder = new AlertDialog.Builder(this);

		Log.i(getPackageName(),"AboutBox: displayed");
		
    	PackageInfo pi = null;
    	try{
    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
    	}catch(Exception e){
    		e.printStackTrace();
    		}
    	builder.setTitle(getString(R.string.ABOUT_TITLE));
    	builder.setMessage(getString(R.string.app_name)+
    			"\n\nv"+pi.versionName+"-C"+pi.versionCode+
    			"\n\n"+getString(R.string.ABOUT_CONTENT));
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.show();
	}
	
	public void showExitBox() throws Exception  {
		
		Builder builder = new AlertDialog.Builder(this);
		
		Log.i(getPackageName(),"Splash: displayed");
		
    	PackageInfo pi = null;
    	try{
    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
    	}catch(Exception e){
    		e.printStackTrace();
    		}
    	builder.setTitle("Done!");
    	builder.setMessage("These settings are saved! \nDevice should now connect to the new network.\nThis app will now close. ");
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                //Activity transfer to wifi settings
	               Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
	               startActivity(intent);
	        	   WiFiACEList.this.finish();
	           }
	       });
		builder.show();
	}
	
	public void togglePwd(View view) throws Exception  {

		TextView passwordView = (TextView)findViewById(R.id.password);
		CheckBox passwordToggle = (CheckBox)findViewById(R.id.togglepwdchkbox);
			
		//Context context = getApplicationContext();
	
		if (passwordToggle.isChecked()==true) {
			//Toast.makeText(context, "IF", Toast.LENGTH_SHORT).show();
			passwordView.setTransformationMethod(null);
			Log.i(getPackageName(),"ShowPwd: Enabled");
		} else {
			//Toast.makeText(context, "ELSE", Toast.LENGTH_SHORT).show();
			passwordView.setTransformationMethod(new PasswordTransformationMethod());
			Log.i(getPackageName(),"ShowPwd: Disabled");
		}

	}
	
	public void checkSDKVersion() {
		
		String sdk_var = android.os.Build.VERSION.SDK;
		String androidOS = android.os.Build.VERSION.RELEASE;

		Log.i(getPackageName(),"SDK Detected: " + sdk_var);
		
		// build using SDK version 15 (4.0.3) so check if the device has a lower sdk version
		if(Integer.valueOf(sdk_var)<15) {

        	Log.i(getPackageName(),"Incompatible Android Version ("+ androidOS + "), SDK version ("+ sdk_var + ") detected quitting application");
        	
			Builder builder = new AlertDialog.Builder(this);

	    	PackageInfo pi = null;
	    	try{
	    	pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    		}
	    	builder.setTitle("Incompatible Android Version");
	    	builder.setMessage("This app is not compatiable with the Android version ("+ 
	    				androidOS + ") detected on this device.\n\n" +
	    				"The app will function only on devices with Android version 4.0.3 (ICS) and above! \n\n" +
	    				"For older android version visit: http://stdwww.iimahd.ernet.in/ccc/devicesetting.php ");
			builder.setPositiveButton(getString(android.R.string.ok),  new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {    
	            	WiFiACEList.this.finish();
	            } 
	          });
			builder.show();
					
		}
		
	}
}
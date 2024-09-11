package com.moutamid.givegetvalue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fxn.stash.Stash;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;
import com.moutamid.givegetvalue.bluetooth.BluetoothConnectionService;
import com.moutamid.givegetvalue.bluetooth.DeviceListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;
    BluetoothConnectionService mBluetoothConnection;
    String extractedDevice = "test";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    BluetoothDevice mBTDevice;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;
    public static String extractedValue = "";
    public static String extractedType = "";
    ProgressDialog mProgressDialog;
    private boolean isReceiver1Registered = false;
    private boolean isReceiver2Registered = false;
    private boolean isReceiver3Registered = false;
    private boolean isReceiver4Registered = false;

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };


    @SuppressLint("MissingPermission")
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().equals(extractedDevice)) {
                    lvNewDevices.setVisibility(View.GONE);
                    mBTDevices.add(device);
                    Log.d(TAG, "onItemClick: You Clicked on a device.");
                    String deviceName = mBTDevices.get(0).getName();
                    String deviceAddress = mBTDevices.get(0).getAddress();
                    Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                    Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        Log.d(TAG, "Trying to pair with " + deviceName);
                        mBTDevices.get(0).createBond();
                        mBTDevice = mBTDevices.get(0);
                        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this, MainActivity.this, confirmation_lyt);
                        mProgressDialog.dismiss();
                        startConnection();
                    }
                } else {
                    mBTDevices.add(device);
                }
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);

                mBluetoothAdapter.cancelDiscovery();


            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    @SuppressLint("MissingPermission")
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    private static final String PREFS_NAME = "AppPrefs";
    private static final String USER_ID_KEY = "UserID";
    public static EditText valueEditText, passwordEditText;
    public static Spinner typeSpinner;
    public static TextView balanceTextView;
    private Button readButton, requestButton, giveButton, addButton;
    private ImageView qrCodeImageView;
    public static int currentBalance = 0;
    private boolean isMasterUser = false;
    Button enterButton, userButton, masterButton, quitButton;
    String valueType;
    private String masterKeyAlias;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    TextView incomingTextView;
    RelativeLayout confirmation_lyt;
    LinearLayout available_Devices;
    Button NoButton, yesButton, connectButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String userId = getUserId(this);
        Log.d("UserID", userId + "  ID");
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        available_Devices = findViewById(R.id.available_Devices);
        yesButton = findViewById(R.id.yesButton);
        NoButton = findViewById(R.id.NoButton);
        connectButton = findViewById(R.id.connectButton);
        incomingTextView = findViewById(R.id.incomingText);
        confirmation_lyt = findViewById(R.id.confirmation_lyt);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                available_Devices.setVisibility(View.INVISIBLE);
                startConnection();
            }
        });
        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmation_lyt.setVisibility(View.GONE);
            }
        });
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmation_lyt.setVisibility(View.GONE);
                byte[] bytes = "yes".getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
                Stash.put(valueType + "_balance", currentBalance);
                balanceTextView.setText("Balance for " + valueType + ": " + currentBalance);
                valueEditText.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.GONE);
                enterButton.setVisibility(View.GONE);
                userButton.setEnabled(false);
                masterButton.setEnabled(true);
                isMasterUser = false;
                readButton.setVisibility(View.VISIBLE);
                requestButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                typeSpinner.setVisibility(View.VISIBLE);
                balanceTextView.setVisibility(View.VISIBLE);
                quitButton.setVisibility(View.GONE);
                qrCodeImageView.setVisibility(View.GONE);
                valueEditText.setText("");
//                if (Stash.getInt(valueType + "_balance", 0) == 0) {
//                    giveButton.setVisibility(View.GONE);
//                } else {
                giveButton.setVisibility(View.VISIBLE);
//                }
            }
        });
       checkBluetoothPermissions();
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create("master_passwords_prefs", masterKeyAlias, this, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("password1", "abc");
            editor.putString("password2", "DqPn9tkdbPWN");
            editor.putString("password3", "auAVMN5Qf6PH");
            editor.apply();
        } catch (Exception e) {
            Log.d("Exception", e.getMessage() + " msg");
        }

        quitButton = findViewById(R.id.quitButton);
        requestButton = findViewById(R.id.requestButton);
         userButton = findViewById(R.id.userButton);
        enterButton = findViewById(R.id.enterButton);
        masterButton = findViewById(R.id.masterButton);
        passwordEditText = findViewById(R.id.passwordEditText);
        addButton = findViewById(R.id.addButton);
        valueEditText = findViewById(R.id.valueEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        balanceTextView = findViewById(R.id.balanceTextView);
        readButton = findViewById(R.id.readButton);
        giveButton = findViewById(R.id.giveButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        Log.d("valueeee", currentBalance + "   " + Stash.getInt("balance", 0));
        if (Stash.getInt(valueType + "_balance", 0) == 0) {
            giveButton.setVisibility(View.GONE);
        } else {
            giveButton.setVisibility(View.VISIBLE);
        }
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnEnableDisable_Discoverable();
                scanQRCode();
            }
        });
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userButton.setBackgroundResource(R.drawable.btn_bg);  // Set to active
                masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Set to inactive
                valueEditText.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.GONE);
                enterButton.setVisibility(View.GONE);
                userButton.setEnabled(false);
                masterButton.setEnabled(true);
                isMasterUser = false;
                readButton.setVisibility(View.VISIBLE);
                requestButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                typeSpinner.setVisibility(View.VISIBLE);
                balanceTextView.setVisibility(View.VISIBLE);
                giveButton.setVisibility(View.VISIBLE);
                quitButton.setVisibility(View.GONE);
                qrCodeImageView.setVisibility(View.GONE);
                valueEditText.setText("");
            }
        });
        userButton.setBackgroundResource(R.drawable.btn_bg);  // Active state background
        masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Inactive state background
        readButton.setVisibility(View.VISIBLE);
        requestButton.setVisibility(View.VISIBLE);
checkApp(MainActivity.this);
        userButton.setOnClickListener(v -> {
            userButton.setBackgroundResource(R.drawable.btn_bg);  // Set to active
            masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Set to inactive
            valueEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.GONE);
            enterButton.setVisibility(View.GONE);
            userButton.setEnabled(false);
            masterButton.setEnabled(true);
            isMasterUser = false;
            readButton.setVisibility(View.VISIBLE);
            requestButton.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.GONE);
            typeSpinner.setVisibility(View.VISIBLE);
            balanceTextView.setVisibility(View.VISIBLE);
            quitButton.setVisibility(View.GONE);
            qrCodeImageView.setVisibility(View.GONE);
            valueEditText.setText("");
//            typeSpinner.setEnabled(false);
        });

        masterButton.setOnClickListener(v -> {
            masterButton.setBackgroundResource(R.drawable.btn_bg);
            userButton.setBackgroundResource(R.drawable.btn_bg_lght);
            passwordEditText.setVisibility(View.VISIBLE);
            valueEditText.setVisibility(View.GONE);
            enterButton.setVisibility(View.VISIBLE);
            masterButton.setEnabled(false);
            userButton.setEnabled(true);
            quitButton.setVisibility(View.GONE);
            qrCodeImageView.setVisibility(View.GONE);
            valueEditText.setText("");
            isMasterUser = true;
            readButton.setVisibility(View.GONE);
            typeSpinner.setEnabled(true);
            addButton.setVisibility(View.GONE);
            giveButton.setVisibility(View.GONE);
            requestButton.setVisibility(View.GONE);
            typeSpinner.setVisibility(View.GONE);
            balanceTextView.setVisibility(View.GONE);
            typeSpinner.setSelection(0);
            balanceTextView.setText("Balance: 0");

        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                Stash.put("position", position);
                if (position != 0) {
                    if (isMasterUser && position > 0) { // Ensure a valid type is selected
                        valueType = selectedItem;
                        int categoryBalance = Stash.getInt(valueType + "_balance", 0);
                        balanceTextView.setText("Balance for " + valueType + ": " + categoryBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);
                        if (Stash.getInt(valueType + "_balance", 0) == 0) {
                            giveButton.setVisibility(View.GONE);
                            addButton.setVisibility(View.VISIBLE);
                        } else {
                            giveButton.setVisibility(View.VISIBLE);
                            addButton.setVisibility(View.VISIBLE);

                        }
                    } else {
                        String valueType = selectedItem;
                        int categoryBalance = Stash.getInt(valueType + "_balance", 0);
                        balanceTextView.setText("Balance for " + valueType + ": " + categoryBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);

                        if (Stash.getInt(valueType + "_balance", 0) == 0) {
                            giveButton.setVisibility(View.GONE);
                        } else {
                            giveButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where no item is selected
            }
        });
        if (isMasterUser) {
            typeSpinner.setSelection(0);
//            balanceTextView.setText("Balance: 0");

        } else {
            typeSpinner.setSelection(Stash.getInt("position"));
        }
//        typeSpinner.setId(position);
        giveButton.setOnClickListener(v -> checkBluetoothPermissions_());
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (valueType != null && !valueType.isEmpty()) {
                    String valueToAddStr = valueEditText.getText().toString();
                    if (!valueToAddStr.isEmpty()) {
                        int valueToAdd = Integer.parseInt(valueToAddStr);
                        addValueToBalance(valueType, valueToAdd);
                    } else {
                        Toast.makeText(MainActivity.this, "Please enter a value to add", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a type", Toast.LENGTH_SHORT).show();
                }
            }
        });
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RequestActivity.class));
            }
        });
//        addButton.setOnClickListener(v -> addValueToBalance(valueType, valueToAdd));
        enterButton.setOnClickListener(view -> addPasswordAsMaster());
    }

    private void startGiveProcess() {
        valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString();
        if (valueToGive.isEmpty()) {
            Toast.makeText(this, "Please enter some value to give", Toast.LENGTH_SHORT).show();
            return;
        }
        if (valueType.equals("Select Type")) {
            Toast.makeText(this, "Please select any type", Toast.LENGTH_SHORT).show();
            return;
        }
        giveButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        readButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.GONE);
        quitButton.setVisibility(View.VISIBLE);
        quitButton.setEnabled(false);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Bluetooth operations...
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter1 = bluetoothManager.getAdapter();
        String deviceName = bluetoothAdapter1 != null ? bluetoothAdapter1.getName() : "Unknown Device";

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        String qrData = "Type: " + valueType + ", Value: " + valueToGive + ", Device: " + deviceName + ", Timestamp: " + formattedDate;
        generateQRCode(qrData);

//        quitButton.setEnabled(true);

        // TODO after confirmation
        currentBalance = Stash.getInt(valueType + "_balance");
        currentBalance -= Integer.parseInt(valueToGive);
        quitButton.setEnabled(true);
    }


    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(writer.encode(data, BarcodeFormat.QR_CODE, 512, 512));
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
            quitButton.setVisibility(View.VISIBLE);
            available_Devices.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bmp;
    }

    private boolean isMasterPassword(String inputPassword) {
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    "master_passwords_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sharedPreferences.getAll().containsValue(inputPassword);
    }

    private void addPasswordAsMaster() {
        String enteredPassword = passwordEditText.getText().toString();
        if (!isMasterPassword(enteredPassword)) {
            Toast.makeText(this, "Login failed, Try Again", Toast.LENGTH_SHORT).show();
            passwordEditText.setText("");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show();
        passwordEditText.setText("");
        valueEditText.setVisibility(View.VISIBLE);
        passwordEditText.setVisibility(View.GONE);
        enterButton.setVisibility(View.GONE);
        readButton.setVisibility(View.VISIBLE);
        typeSpinner.setEnabled(true);
        addButton.setVisibility(View.VISIBLE);
        giveButton.setVisibility(View.GONE);
        requestButton.setVisibility(View.VISIBLE);
        typeSpinner.setVisibility(View.VISIBLE);
        balanceTextView.setVisibility(View.VISIBLE);

        valueEditText.setEnabled(true);
    }



    @Override
    protected void onResume() {
        super.onResume();

        // Register BroadcastReceiver 1
        if (!isReceiver1Registered) {
            IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, filter1);
            isReceiver1Registered = true;
        }

        // Register BroadcastReceiver 2
        if (!isReceiver2Registered) {
            IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mBroadcastReceiver2, filter2);
            isReceiver2Registered = true;
        }

        // Register BroadcastReceiver 3
        if (!isReceiver3Registered) {
            IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, filter3);
            isReceiver3Registered = true;
        }

        // Register BroadcastReceiver 4
        if (!isReceiver4Registered) {
            IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver4, filter4);
            isReceiver4Registered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Optionally, unregister receivers here if needed, but be careful to check the flags
        // For example:
        if (isReceiver1Registered) {
            unregisterReceiver(mBroadcastReceiver1);
            isReceiver1Registered = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister BroadcastReceiver 1
        if (isReceiver1Registered) {
            unregisterReceiver(mBroadcastReceiver1);
            isReceiver1Registered = false;
        }

        // Unregister BroadcastReceiver 2
        if (isReceiver2Registered) {
            unregisterReceiver(mBroadcastReceiver2);
            isReceiver2Registered = false;
        }

        // Unregister BroadcastReceiver 3
        if (isReceiver3Registered) {
            unregisterReceiver(mBroadcastReceiver3);
            isReceiver3Registered = false;
        }

        // Unregister BroadcastReceiver 4
        if (isReceiver4Registered) {
            unregisterReceiver(mBroadcastReceiver4);
            isReceiver4Registered = false;
        }
    }

    private void addValueToBalance(String valueType, int valueToAdd) {
        int categoryBalance = Stash.getInt(valueType + "_balance", 0);
        Log.d("BalanceUpdate", "1   " + categoryBalance);
        categoryBalance += valueToAdd;
        Log.d("BalanceUpdate", "2   " + categoryBalance);
        Stash.put(valueType + "_balance", categoryBalance);
        Log.d("BalanceUpdate", "3   " + Stash.getInt(valueType + "_balance", 0));
        balanceTextView.setText("Balance for " + valueType + ": " + categoryBalance);
        Log.d("BalanceUpdate", "Updated Balance for " + valueType + ": " + categoryBalance);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void scanQRCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code");
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedText = result.getContents();
                readButton.setVisibility(View.VISIBLE);
                String[] parts = scannedText.split(", ");
                extractedType = "";
                String extractedTimestamp = "";
                for (String part : parts) {
                    if (part.startsWith("Type: ")) {
                        extractedType = part.substring("Type: ".length());
                    } else if (part.startsWith("Value: ")) {
                        extractedValue = part.substring("Value: ".length());
                    } else if (part.startsWith("Device: ")) {
                        extractedDevice = part.substring("Device: ".length());
                    } else if (part.startsWith("Timestamp: ")) {
                        extractedTimestamp = part.substring("Timestamp: ".length());
                    }
                }

                currentBalance = Stash.getInt(extractedType + "_balance");
                currentBalance += Integer.parseInt(extractedValue);
                valueEditText.setText(extractedValue);
//                    typeSpinner.setSelection(Integer.parseInt(extractedType));
//                balanceTextView.setText("Balance: " + extractedValue);
                System.out.println("Device: " + extractedDevice);
//                giveButton.setVisibility(View.GONE);
                System.out.println("Timestamp: " + extractedTimestamp);
                Stash.put("type", "reader");
                btnDiscover();
                mProgressDialog = ProgressDialog.show(MainActivity.this, "Connecting Bluetooth"
                        , "Please Wait...", true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGiveProcess();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to scan and connect to nearby devices.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable Bluetooth
                enableBluetooth();
            } else {
                // Permission denied
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(USER_ID_KEY, null);

        if (userId == null) {
            userId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(USER_ID_KEY, userId);
            editor.apply();
        }

        return userId;
    }

    private void checkBluetoothPermissions_() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                startGiveProcess();
            }
        } else {
            startGiveProcess();
        }
    }

    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection." + device + "       " + uuid);
        mBluetoothConnection.startClient(device, uuid);
    }

    @SuppressLint("MissingPermission")
    public void btnEnableDisable_Discoverable() {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);
    }

    @SuppressLint("MissingPermission")
    public void btnDiscover() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }


    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();
        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Log.d(TAG, "Trying to pair with " + deviceName);
        mBTDevices.get(i).createBond();
        mBTDevice = mBTDevices.get(i);
        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this, MainActivity.this, confirmation_lyt);
//        }
    }
    public static void checkApp(Activity activity) {
        String appName = "GiverApp";

        new Thread(() -> {
            URL google = null;
            try {
                google = new URL("https://raw.githubusercontent.com/Moutamid/Moutamid/main/apps.txt");
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String input = null;
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if ((input = in != null ? in.readLine() : null) == null) break;
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                stringBuffer.append(input);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String htmlData = stringBuffer.toString();

            try {
                JSONObject myAppObject = new JSONObject(htmlData).getJSONObject(appName);

                boolean value = myAppObject.getBoolean("value");
                String msg = myAppObject.getString("msg");

                if (value) {
                    activity.runOnUiThread(() -> {
                        new AlertDialog.Builder(activity)
                                .setMessage(msg)
                                .setCancelable(false)
                                .show();
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            List<String> permissions = new ArrayList<>();

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                // Permissions already granted
                enableBluetooth(); // Call your function that handles Bluetooth
            }
        } else {
            // For older versions, no need for runtime permissions
            enableBluetooth();
        }
    }


    private void enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
        // Register Bluetooth state change receivers after permissions are granted
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);  // Make sure this receiver is properly defined
        lvNewDevices.setOnItemClickListener(MainActivity.this);
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);  //
        btnEnableDisable_Discoverable();
        btnDiscover();

    }

}

package com.moutamid.givegetvalue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fxn.stash.Stash;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RequestActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String USER_ID_KEY = "UserID";
    private BluetoothPairingReceiver pairingReceiver = new BluetoothPairingReceiver();
    private EditText valueEditText, passwordEditText;
    private Spinner typeSpinner;
    private TextView balanceTextView;
    private Button readButton, requestButton, giveButton, addButton;
    private ImageView qrCodeImageView;
    private int currentBalance = 0;
    private int position_ = 0;
    private boolean isMasterUser = false;
    Button enterButton, userButton, masterButton, quitButton;
    String valueType;
    int valueToAdd;
    ImageView receiver;
    private String masterKeyAlias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        String userId = getUserId(this);
        Log.d("UserID", userId + "  ID");
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
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(pairingReceiver, filter);
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Discover services after successful connection
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Handle disconnection
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Get the discovered services
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        UUID serviceUUID = service.getUuid();
                        // Get characteristics of the service
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            UUID characteristicUUID = characteristic.getUuid();
                            // Read characteristic if necessary
                            gatt.readCharacteristic(characteristic);
                        }
                    }
                } else {
                    // Handle error
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Handle the received message
                    String receivedMessage = new String(characteristic.getValue());
                    Toast.makeText(RequestActivity.this, "Received Message: " + receivedMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                // Handle write status if necessary
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                // Handle notifications if enabled
            }
        };

        quitButton = findViewById(R.id.quitButton);
        requestButton = findViewById(R.id.requestButton);
        receiver = findViewById(R.id.receiver);
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
        if (Stash.getInt("balance", 0) == 0) {
            readButton.setVisibility(View.VISIBLE);
            giveButton.setVisibility(View.GONE);
        } else {
            readButton.setVisibility(View.GONE);
            giveButton.setVisibility(View.VISIBLE);
        }
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanQRCode();

            }
        });
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    finish();
            }
        });
        userButton.setBackgroundResource(R.drawable.btn_bg);  // Active state background
        masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Inactive state background


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
                position_ = position;
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
        giveButton.setOnClickListener(v -> startGiveProcess());
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (valueType != null && !valueType.isEmpty()) {
                    String valueToAddStr = valueEditText.getText().toString();
                    if (!valueToAddStr.isEmpty()) {
                        int valueToAdd = Integer.parseInt(valueToAddStr);
                        addValueToBalance(valueType, valueToAdd);
                    } else {
                        Toast.makeText(RequestActivity.this, "Please enter a value to add", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RequestActivity.this, "Please select a type", Toast.LENGTH_SHORT).show();
                }
            }
        });
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String valueToGive = valueEditText.getText().toString();
                if (valueToGive.isEmpty()) {
                    Toast.makeText(RequestActivity.this, "Please enter some value to request", Toast.LENGTH_SHORT).show();
                    return;
                }     BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }
                BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter bluetoothAdapter1 = bluetoothManager.getAdapter();
                String macAddress = "abc";
                if (bluetoothAdapter1 != null) {
                    macAddress = bluetoothAdapter1.getName(); // Likely to be null on newer Android versions
                    Log.d("Bluetooth MAC", "Bluetooth MAC Address: " + macAddress);

                }
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(date);
                String qrData = "Type: " + position_ + ", Value: " + valueEditText.getText().toString() + ", Device: " + macAddress + ", Timestamp: " + formattedDate;
                generateQRCode(qrData);
            }
        });

//        addButton.setOnClickListener(v -> addValueToBalance(valueType, valueToAdd));
        enterButton.setOnClickListener(view -> addPasswordAsMaster());
    }

    private void startGiveProcess() {
        String valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString();
        if (valueToGive.isEmpty()) {
            Toast.makeText(this, "Please enter some value to give", Toast.LENGTH_SHORT).show();
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
            bluetoothAdapter.enable();
        }
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter1 = bluetoothManager.getAdapter();
        String macAddress = "abc";
        if (bluetoothAdapter1 != null) {
            macAddress = bluetoothAdapter1.getName(); // Likely to be null on newer Android versions
            Log.d("Bluetooth MAC", "Bluetooth MAC Address: " + macAddress);

        }
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        String qrData = "Type: " + position_ + ", Value: " + valueToGive + ", Device: " + macAddress + ", Timestamp: " + formattedDate;
        generateQRCode(qrData);

//        TODO after confirmation
//        currentBalance = Stash.getInt(valueType + "_balance");
//        currentBalance -= Integer.parseInt(valueToGive);
//        Stash.put(valueType + "_balance", currentBalance);
//        balanceTextView.setText("Balance for " + valueType + ": " + currentBalance);
    }


    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(writer.encode(data, BarcodeFormat.QR_CODE, 512, 512));
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
            quitButton.setVisibility(View.VISIBLE);
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
            startActivity(new Intent(this, RequestActivity.class));
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(pairingReceiver);
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
        startActivity(new Intent(this, RequestActivity.class));
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
                Toast.makeText(this, "Scanned: " + scannedText, Toast.LENGTH_LONG).show();
                readButton.setVisibility(View.VISIBLE);
                String[] parts = scannedText.split(", ");
                String extractedType = "";
                String extractedValue = "";
                String extractedDevice = "";
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
                valueEditText.setText(extractedValue);
                typeSpinner.setSelection(Integer.parseInt(extractedType));
                balanceTextView.setText("Balance: " + extractedValue);
                System.out.println("Device: " + extractedDevice);
                giveButton.setVisibility(View.GONE);
                System.out.println("Timestamp: " + extractedTimestamp);
//                readButton.setText("Get");
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice targetDevice = null;
                BluetoothGatt gatt;

// Ensure Bluetooth is enabled
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                    Toast.makeText(this, "Enabling Bluetooth", Toast.LENGTH_SHORT).show();
                }

// Find the target Bluetooth device
                String targetDeviceName = "V2102";  // Change this to your actual device name
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    Log.d("PairedDevice", "Device Name: " + device.getName());
                    if (device.getName().equals(targetDeviceName)) {
                        targetDevice = device;
                        break;
                    }
                }

                if (targetDevice != null) {
                    // Connect to the GATT server
                    BluetoothDevice finalTargetDevice = targetDevice;
                    gatt = targetDevice.connectGatt(RequestActivity.this, false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            super.onConnectionStateChange(gatt, status, newState);

                            // Run on the UI thread to avoid Looper exceptions
                            runOnUiThread(() -> {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    Log.d("Bluetooth", "Connected to GATT server.");
                                    Toast.makeText(RequestActivity.this, "Connected to GATT server.", Toast.LENGTH_SHORT).show();

                                    // Delay service discovery for 1 second
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        boolean result = gatt.discoverServices();
                                        Log.d("Bluetooth", "Service discovery started: " + result);
                                    }, 1000);

                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    Log.d("Bluetooth", "Disconnected from GATT server.");
                                    Toast.makeText(RequestActivity.this, "Disconnected from GATT server.", Toast.LENGTH_SHORT).show();
                                }

                                if (status == BluetoothGatt.GATT_FAILURE) {
                                    Log.e("Bluetooth", "GATT Failure, Status: " + status);
                                    Toast.makeText(RequestActivity.this, "GATT Failure, Status: " + status, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

//                        @Override
//                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                            super.onServicesDiscovered(gatt, status);
//
//                            if (status == BluetoothGatt.GATT_SUCCESS) {
//                                Log.d("Bluetooth", "Services discovered.");
//                                Toast.makeText(MainActivity.this, "Services discovered!", Toast.LENGTH_SHORT).show();
//
//                                List<BluetoothGattService> services = gatt.getServices();
//                                for (BluetoothGattService service : services) {
//                                    UUID serviceUUID = service.getUuid();
//                                    Log.d("Bluetooth", "Service UUID: " + serviceUUID);
//
//                                    try {
//                                        // Create Bluetooth Socket and send message
//                                        BluetoothSocket bluetoothSocket = finalTargetDevice.createRfcommSocketToServiceRecord(serviceUUID);
//                                        bluetoothSocket.connect();
//                                        OutputStream outputStream = bluetoothSocket.getOutputStream();
//                                        String message = "Hello from Client!";
//                                        outputStream.write(message.getBytes());
//                                        Log.d("Bluetooth", "Message sent: " + message);
//
//                                    } catch (IOException e) {
//                                        Log.e("Bluetooth", "Error: " + e.getMessage());
//                                        e.printStackTrace();
//                                    }
//                                }
//                            } else {
//                                Log.e("Bluetooth", "Failed to discover services. Status: " + status);
//                            }
//                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            super.onServicesDiscovered(gatt, status);

                            runOnUiThread(() -> {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    Log.d("Bluetooth", "Services discovered successfully.");

                                    UUID serviceUUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
                                    UUID characteristicUUID = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");

                                    BluetoothGattService gattService = gatt.getService(serviceUUID);
                                    if (gattService != null) {
                                        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristicUUID);

                                        if (gattCharacteristic != null) {
                                            int properties = gattCharacteristic.getProperties();
                                            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
//                                                    (properties & BluetoothGattCharacteristic.) > 0) {
                                                // Set the message you want to send
                                                String message = "Hello from Client!";
                                                gattCharacteristic.setValue(message.getBytes());

                                                boolean writeSuccess = gatt.writeCharacteristic(gattCharacteristic);

                                                if (writeSuccess) {
                                                    Log.d("Bluetooth", "Message written to characteristic.");
                                                } else {
                                                    Log.e("Bluetooth", "Failed to write message to characteristic.");
                                                }
                                            }
//                                            else {
//                                                Log.e("Bluetooth", "Characteristic does not support writing. Properties: " + properties);
//                                            }
                                        } else {
                                            Log.e("Bluetooth", "Characteristic not found.");
                                        }
                                    } else {
                                        Log.e("Bluetooth", "Service not found.");
                                    }
                                } else {
                                    Log.e("Bluetooth", "Failed to discover services. Status: " + status);
                                }
                            });
                        }


                        @Override
                        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("Bluetooth", "Characteristic read: " + characteristic.getValue());
                            }
                        }

                    });

                    if (gatt == null) {
                        Log.e("Bluetooth", "BluetoothGatt is null.");
                    }

                } else {
                    Log.d("Bluetooth", "Device not found!");
                    Toast.makeText(this, "Device not found!", Toast.LENGTH_SHORT).show();
                }

            } else {
                    System.out.println("Device not found!");
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
}

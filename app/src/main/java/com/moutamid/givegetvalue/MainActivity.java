package com.moutamid.givegetvalue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.fxn.stash.Stash;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothPairingReceiver pairingReceiver = new BluetoothPairingReceiver();
    private static final String TARGET_BLUETOOTH_MAC_ADDRESS = "D0:9C:AE:62:92:91";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID
    private EditText valueEditText, passwordEditText;
    private Spinner typeSpinner;
    private TextView balanceTextView;
    private Button modeButton, giveButton, addButton;
    private ImageView qrCodeImageView;
    private int currentBalance = 0;
    private int position_ = 0;
    private boolean isMasterUser = false;
    Button enterButton, userButton, masterButton, quitButton;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    String valueType;
    int valueToAdd;
    ImageView receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(pairingReceiver, filter);
        quitButton = findViewById(R.id.quitButton);
        receiver = findViewById(R.id.receiver);
        userButton = findViewById(R.id.userButton);
        enterButton = findViewById(R.id.enterButton);
        masterButton = findViewById(R.id.masterButton);
        passwordEditText = findViewById(R.id.passwordEditText);
        addButton = findViewById(R.id.addButton);
        valueEditText = findViewById(R.id.valueEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        balanceTextView = findViewById(R.id.balanceTextView);
        modeButton = findViewById(R.id.modeButton);
        giveButton = findViewById(R.id.giveButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        Log.d("valueeee", currentBalance + "   " + Stash.getInt("balance", 0));
        if (Stash.getInt("balance", 0) == 0) {
            modeButton.setVisibility(View.VISIBLE);
            giveButton.setVisibility(View.GONE);
        } else {
            modeButton.setVisibility(View.GONE);
            giveButton.setVisibility(View.VISIBLE);
        }
        receiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanQRCode();

            }
        });
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            }
        });
//        balanceTextView.setText("Balance: " + Stash.getInt("balance", 0));
//        typeSpinner.setEnabled(false);
        userButton.setBackgroundResource(R.drawable.btn_bg);  // Active state background
        masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Inactive state background
//        valueEditText.setEnabled(Stash.getInt("balance", 0) != 0);
        userButton.setOnClickListener(v -> {
            userButton.setBackgroundResource(R.drawable.btn_bg);  // Set to active
            masterButton.setBackgroundResource(R.drawable.btn_bg_lght);  // Set to inactive
            valueEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.GONE);
            enterButton.setVisibility(View.GONE);
            userButton.setEnabled(false);
            masterButton.setEnabled(true);
            isMasterUser = false;
            modeButton.setVisibility(View.VISIBLE);

            addButton.setVisibility(View.GONE);
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
            valueEditText.setText("");
            isMasterUser = true;
            modeButton.setVisibility(View.GONE);
            typeSpinner.setEnabled(true);
            addButton.setVisibility(View.GONE);
            giveButton.setVisibility(View.GONE);
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
//                                String valueType = parent.getItemAtPosition(position).toString();
                        valueType = selectedItem;
                        int categoryBalance = Stash.getInt(valueType + "_balance", 0);
                        balanceTextView.setText("Balance for " + valueType + ": " + categoryBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);

                        String valueToAddStr = valueEditText.getText().toString();
                        if (!valueToAddStr.isEmpty()) {
                            valueToAdd = Integer.parseInt(valueToAddStr);
                            addValueToBalance(valueType, valueToAdd);
                        }
                    } else {
                        String valueType = selectedItem;
                        int categoryBalance = Stash.getInt(valueType + "_balance", 0);
                        balanceTextView.setText("Balance for " + valueType + ": " + categoryBalance);
                        Log.d("BalanceDisplay", "Balance for " + valueType + ": " + categoryBalance);
                        if (Stash.getInt(valueType + "_balance", 0) == 0) {
                            modeButton.setVisibility(View.VISIBLE);
                            giveButton.setVisibility(View.GONE);
                        } else {
                            modeButton.setVisibility(View.GONE);
                            giveButton.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
//                    Toast.makeText(MainActivity.this, "Please select any type", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where no item is selected
            }
        });
        if (isMasterUser) {
            typeSpinner.setSelection(0);
            balanceTextView.setText("Balance: 0");

        } else {
            typeSpinner.setSelection(Stash.getInt("position"));
        }
//        typeSpinner.setId(position);
        giveButton.setOnClickListener(v -> startGiveProcess());
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            }
        });
//        addButton.setOnClickListener(v -> addValueToBalance(valueType, valueToAdd));
        enterButton.setOnClickListener(view -> addPasswordAsMaster());
    }

    private void startGiveProcess() {
        String valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString();
        giveButton.setVisibility(View.GONE);
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

//        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(TARGET_BLUETOOTH_MAC_ADDRESS);
//        new ConnectBluetoothTask(device).execute();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        String qrData = "Type: " + position_ + ", Value: " + valueToGive + ", Device: " + macAddress+", Timestamp: " + formattedDate;
        generateQRCode(qrData);
        Stash.getInt(valueType + "_balance");

        currentBalance = Stash.getInt(valueType + "_balance");
        ;
        currentBalance -= Integer.parseInt(valueToGive);

        Stash.put(valueType + "_balance", currentBalance);
        balanceTextView.setText("Balance for " + valueType + ": " + currentBalance);
    }

    private class ConnectBluetoothTask extends AsyncTask<Void, Void, Boolean> {
        private final BluetoothDevice device;
        private BluetoothSocket bluetoothSocket;

        ConnectBluetoothTask(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                return true;
            } catch (IOException e) {
                Log.e("BluetoothConnection", "Connection failed", e);
                try {
                    bluetoothSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", int.class).invoke(device, 1);
                    bluetoothSocket.connect();
                    return true;
                } catch (Exception e2) {
                    Log.e("BluetoothConnection", "Could not connect using reflection", e2);
                    try {
                        bluetoothSocket.close();
                    } catch (IOException closeException) {
                        Log.e("BluetoothConnection", "Could not close the client socket", closeException);
                    }
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d("BluetoothConnection", "Connected successfully");
            } else {
                Log.d("BluetoothConnection", "Failed to connect");
            }
        }
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

    private void addValueAsMaster() {
        String valueType = typeSpinner.getSelectedItem().toString();
        String valueToAdd = valueEditText.getText().toString();
        if (!isMasterUser || valueToAdd.isEmpty()) {
            return;
        }
        if (position_ == 0) {
//            Toast.makeText(this, "Please select any type", Toast.LENGTH_SHORT).show();
            return;
        }
        currentBalance += Integer.parseInt(valueToAdd);
        balanceTextView.setText("Balance: " + currentBalance);
        Stash.put("balance", currentBalance);
        Log.d("valueeee", currentBalance + "   " + Stash.getInt("balance", 0));
    }

    private void addPasswordAsMaster() {
        String masterPassword = "1234";
        String enteredPassword = passwordEditText.getText().toString();
        if (!enteredPassword.equals(masterPassword)) {
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
        addButton.setVisibility(View.VISIBLE);
        modeButton.setVisibility(View.GONE);
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
modeButton.setVisibility(View.VISIBLE);
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
                balanceTextView.setText("Balance: "+ extractedValue);
                System.out.println("Device: " + extractedDevice);
         giveButton.setVisibility(View.GONE);       System.out.println("Timestamp: " + extractedTimestamp);
                modeButton.setText("Get");
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }

// Your target device name
                String targetDeviceName = extractedDevice;

// Get the list of paired devices
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                BluetoothDevice targetDevice = null;

                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(targetDeviceName)) {
                        targetDevice = device;
                        break;
                    }
                }

                if (targetDevice != null) {
                    try {
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                        BluetoothSocket bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Device not found!");
                }

            }
        }
    }
}

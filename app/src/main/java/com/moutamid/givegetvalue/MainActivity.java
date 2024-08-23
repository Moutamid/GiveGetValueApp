package com.moutamid.givegetvalue;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.moutamid.givegetvalue.R;

public class MainActivity extends AppCompatActivity {

    private EditText valueEditText;
    private Spinner typeSpinner;
    private TextView balanceTextView;
    private Button modeButton;
    private Button giveButton;
    private ImageView qrCodeImageView;

    private int currentBalance = 90; // Example starting balance
    private boolean isReadMode = true;
    private boolean isGiving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        valueEditText = findViewById(R.id.valueEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        balanceTextView = findViewById(R.id.balanceTextView);
        modeButton = findViewById(R.id.modeButton);
        giveButton = findViewById(R.id.giveButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);

        // Set up spinner options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Update the UI based on the initial balance and mode
        updateUI();

        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });

        giveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isGiving) {
                    startGiveProcess();
                } else {
                    quitGiveProcess();
                }
            }
        });
    }

    private void updateUI() {
        balanceTextView.setText("Balance: " + currentBalance);

        if (currentBalance == 0) {
            valueEditText.setEnabled(false);
            giveButton.setEnabled(false);
        } else {
            valueEditText.setEnabled(true);
            giveButton.setEnabled(true);
        }

        if (isReadMode) {
            modeButton.setText("Read");
        } else {
            modeButton.setText("Write");
        }

        if (isGiving) {
            giveButton.setText("Quit");
        } else {
            giveButton.setText("Give");
        }
    }

    private void toggleMode() {
        isReadMode = !isReadMode;
        updateUI();
    }

    private void startGiveProcess() {
        // Validate input and start the "Give" process
        String valueType = typeSpinner.getSelectedItem().toString();
        String valueToGive = valueEditText.getText().toString();

        if (valueType.isEmpty() || valueToGive.isEmpty() || Integer.parseInt(valueToGive) > currentBalance) {
            // Handle validation error (e.g., show a toast message)
            return;
        }

        isGiving = true;
        updateUI();

        // Turn on Bluetooth if it's off
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        // Generate QR code
        String qrData = "Type: " + valueType + ", Value: " + valueToGive;
        generateQRCode(qrData);

        // Adjust the current balance (for demonstration purposes)
        currentBalance -= Integer.parseInt(valueToGive);
        balanceTextView.setText("Balance: " + currentBalance);
    }

    private void quitGiveProcess() {
        isGiving = false;
        qrCodeImageView.setVisibility(View.GONE);
        updateUI();
    }

    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(writer.encode(data, BarcodeFormat.QR_CODE, 512, 512));
            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private Bitmap toBitmap(com.google.zxing.common.BitMatrix matrix) {
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
}

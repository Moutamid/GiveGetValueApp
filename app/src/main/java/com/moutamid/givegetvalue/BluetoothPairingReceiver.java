package com.moutamid.givegetvalue;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothPairingReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothPairingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

            Log.d(TAG, "Pairing request received for " + device.getAddress());
            if (pairingVariant == BluetoothDevice.PAIRING_VARIANT_PIN) {
                String pin = "1234"; // Replace with your desired PIN or handle it dynamically
                Log.d(TAG, "PIN: " + pin);
                device.setPin(pin.getBytes());

                // Optionally, confirm the pairing
                device.setPairingConfirmation(true);
            }
        }
    }
}

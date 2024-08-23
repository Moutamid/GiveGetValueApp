package com.moutamid.givegetvalue;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MasterActivity extends AppCompatActivity {

    private EditText valueEditText;
    private Spinner typeSpinner;
    private TextView balanceTextView;
    private Button modeButton;

    private int currentBalance = 90;
    private boolean isReadMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        valueEditText = findViewById(R.id.valueEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        balanceTextView = findViewById(R.id.balanceTextView);
        modeButton = findViewById(R.id.modeButton);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        updateUI();

        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });
    }

    private void updateUI() {
        balanceTextView.setText("Balance: " + currentBalance);

        if (currentBalance == 0) {
            valueEditText.setEnabled(false);
        } else {
            valueEditText.setEnabled(true);
        }

        if (isReadMode) {
            modeButton.setText("Read");
        } else {
            modeButton.setText("Write");
        }
    }

    private void toggleMode() {
        isReadMode = !isReadMode;
        updateUI();
    }
}

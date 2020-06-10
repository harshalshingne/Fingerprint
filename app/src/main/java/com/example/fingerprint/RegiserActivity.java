package com.example.fingerprint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

public class RegiserActivity extends AppCompatActivity {
    private static final String TAG = RegiserActivity.class.getSimpleName();


    private RadioGroup radioGroup;
    private boolean loginOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regiser);

        radioGroup = (RadioGroup)findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                if(id == R.id.with_fingerprint){
                    loginOption = false;
                }

            }
        });
        Button signUpButton = (Button) findViewById(R.id.sign_up_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int selectedButtonId = radioGroup.getCheckedRadioButtonId();
                 if(selectedButtonId == -1){
                    Toast.makeText(RegiserActivity.this, "Login option must be selected", Toast.LENGTH_LONG).show();
                }else{
//                    Gson gson = ((CustomApplication)getApplication()).getGsonObject();
                    UserObject userData = new UserObject(loginOption);

                    Intent loginIntent = new Intent(RegiserActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                }
            }
        });
    }
}
package com.example.fingerprint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintHandler fingerprintHandler;
    private static final String FINGERPRINT_KEY = "key_name";
    private static final int REQUEST_USE_FINGERPRINT = 300;
    protected static Gson mGson;
    protected static CustomSharedPreference mPref;
    private static UserObject mUser;
    private static String userString;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fingerprintHandler = new FingerprintHandler(this);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        // check support for android fingerprint on device
        checkDeviceFingerprintSupport();
        //generate fingerprint keystore
        generateFingerprintKeyStore();
        //instantiate Cipher class
        Cipher mCipher = instantiateCipher();
        if(mCipher != null){
            cryptoObject = new FingerprintManager.CryptoObject(mCipher);
        }
        ImageView fingerprintImage = (ImageView)findViewById(R.id.fingerprint_image);
        fingerprintImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkDeviceFingerprintSupport() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_USE_FINGERPRINT);
        } else {
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(LoginActivity.this, "Fingerprint is not supported in this device", Toast.LENGTH_LONG).show();
            }
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(LoginActivity.this, "Fingerprint not yet configured", Toast.LENGTH_LONG).show();
            }
            if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(LoginActivity.this, "Screen lock is not secure and enable", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateFingerprintKeyStore(){
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        try {

            keyGenerator.init(new KeyGenParameterSpec.Builder(FINGERPRINT_KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        keyGenerator.generateKey();
    }
    private Cipher instantiateCipher(){
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)keyStore.getKey(FINGERPRINT_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | UnrecoverableKeyException |
                CertificateException | IOException | KeyStoreException | InvalidKeyException e) {
            throw new RuntimeException("Failed to instantiate Cipher class");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_USE_FINGERPRINT){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // check support for android fingerprint on device
                checkDeviceFingerprintSupport();
                //generate fingerprint keystore
                generateFingerprintKeyStore();
                //instantiate Cipher class
                Cipher mCipher = instantiateCipher();
                if(mCipher != null){
                    cryptoObject = new FingerprintManager.CryptoObject(mCipher);
                }
            }
            else{
                Toast.makeText(this, "permission refused", Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(this, "unknown", Toast.LENGTH_LONG).show();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static class FingerprintHandler extends FingerprintManager.AuthenticationCallback{
        private static final String TAG = FingerprintHandler.class.getSimpleName();
        private Context context;
        public FingerprintHandler(Context context){
            this.context = context;
        }
        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            Log.d(TAG, "Error message " + errorCode + ": " + errString);
            Toast.makeText(context, "Error authenticating fingerprint", Toast.LENGTH_LONG).show();
        }
        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
            Toast.makeText(context, "Fingerprint authentication successful", Toast.LENGTH_LONG).show();
        }
        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            userString = mPref.getUserData();
            mUser = mGson.fromJson(userString, UserObject.class);
            if(mUser != null){
                Toast.makeText(context, "Fingerprint authentication successful", Toast.LENGTH_LONG).show();
//
            }else{
                Toast.makeText(context, "You must register before login with fingerprint", Toast.LENGTH_LONG).show();
            }
        }
        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }
        public void completeFingerAuthentication(FingerprintManager fingerprintManager, FingerprintManager.CryptoObject cryptoObject){
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try{
                fingerprintManager.authenticate(cryptoObject, new CancellationSignal(), 0, this, null);
            }catch (SecurityException ex) {
                Log.d(TAG, "An error occurred:\n" + ex.getMessage());
            } catch (Exception ex) {
                Log.d(TAG, "An error occurred\n" + ex.getMessage());
            }
        }
    }
}
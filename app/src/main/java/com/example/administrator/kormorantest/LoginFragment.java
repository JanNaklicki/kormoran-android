package com.example.administrator.kormorantest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;

import javax.net.ssl.HttpsURLConnection;
import android.os.NetworkOnMainThreadException;



public class LoginFragment extends android.app.Fragment {

//    android.widget.TextView loginMessage;
//    android.widget.EditText loginField;
//    android.widget.EditText passwordField;
//    android.widget.Button loginButton;
    TextView loginMessage;
    EditText loginField;
    EditText passwordField;
    Button loginButton;
    Button logOutButton;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container,false);

        loginMessage = rootView.findViewById(R.id.login_Message);
        loginField = rootView.findViewById(R.id.login_Text);
        passwordField = rootView.findViewById(R.id.pass_Text);
        loginButton = rootView.findViewById(R.id.submit_Button);
        logOutButton = rootView.findViewById(R.id.logout_Button);

        buttonOperations();

        loginField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
               buttonOperations();
            }

        });
        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                buttonOperations();
            }
        });
        
        
        loginButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                new Api().execute(loginField.getText().toString(), passwordField.getText().toString());

            }
        });
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanLoginData();
                buttonOperations();

            }
        });


        return rootView;
    }

    private class Api extends AsyncTask<String, Integer, Boolean> {
        private String hash(String text){
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] messageDigest = md.digest(text.getBytes());
                BigInteger no = new BigInteger(1, messageDigest);
                String hashtext = no.toString(16);
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }

                return hashtext.toUpperCase();
            }  catch(NoSuchAlgorithmException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected Boolean doInBackground(String... pass) {
            String username = pass[0];
            String password = pass[1];
            try {
                String Request = "http://kormoran.educationhost.cloud/api/administrate.php";
                URL url = new URL(Request);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json;");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                String auth = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = auth.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int status = con.getResponseCode();
                String statusResp = String.valueOf(status);

                Log.e("Status response",statusResp);
                if (status == 200) {
                    return true;
                }
                if(status == 400){
                    //Bad Request
                    //Probably problem with parameters
                    return false;
                }
                if(status == 403){
                    //Forbidden
                    //Wrong login/password
                    return false;
                }
                if(status == 501){
                    //Not Implemented
                    //Not POST/GET method
                    return false;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch(NetworkOnMainThreadException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            loginToast(result);
            if (result){
                saveLoginData();
                buttonOperations();
            }
        }
    }

    private void buttonOperations() {
        if(isLoginDataStored())
        {
            changeButton(true);
        }
        else
        {
            changeButton(false);
            if(isLoginDataCorrect())
            {
                changeButtonColour(true);
            }
            else
            {
                changeButtonColour(false);
            }
        }
    }

    private void changeButtonColour(boolean state){
        loginButton.setClickable(state);
        if (state){
            loginButton.setAlpha(1.0f);
        }else {
            loginButton.setAlpha(0.4f);
        }
    }

    private void changeButton(boolean state){
        loginButton.setClickable(state);
        if (state){
            loginButton.setVisibility(View.GONE);
            logOutButton.setVisibility(View.VISIBLE);
        }else {
            loginButton.setVisibility(View.VISIBLE);
            logOutButton.setVisibility(View.GONE);
        }
    }

    public boolean isLoginDataStored(){

        SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        String loginPref = sharedPref.getString("login", "");
        String passwordPref = sharedPref.getString("password", "");
        return (!loginPref.equals("") && !passwordPref.equals(""));
    }

    private boolean isLoginDataCorrect(){
        return (!loginField.getText().toString().isEmpty() && !passwordField.getText().toString().isEmpty());
    }

    private void loginToast(boolean successful){
        Context context = getActivity().getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        CharSequence text;
        if (successful){
            text = "Zalogowano";
        }else{
            text = "Nie udało się zalogować :(";
        }
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void saveLoginData(){
        SharedPreferences sp = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("login", loginField.getText().toString());
        editor.putString("password", passwordField.getText().toString());
        editor.apply();

    }

    private void cleanLoginData() {
        SharedPreferences sp = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("login", "");
        editor.putString("password", "");
        editor.apply();
        buttonOperations();
    }
}

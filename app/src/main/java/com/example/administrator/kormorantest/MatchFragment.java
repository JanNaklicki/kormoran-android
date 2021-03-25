package com.example.administrator.kormorantest;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;


public class MatchFragment extends android.app.Fragment {
    public String matchId;
    public String tournamentId;
    public String tournamentRepName;
    public String team1;
    public String team2;


    public MatchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_match, container,false);
        TextView team1_name = (TextView)rootView.findViewById(R.id.team1_name);
        TextView team2_name = (TextView)rootView.findViewById(R.id.team2_name);
        team1_name.setText(team1);
        team2_name.setText(team2);
        Button loginButton = rootView.findViewById(R.id.button_save_scores);
        loginButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                EditText team1_score_edit = (EditText)rootView.findViewById(R.id.team1_score);
                EditText team2_score_edit = (EditText)rootView.findViewById(R.id.team2_score);
                final String team1_score = team1_score_edit.getText().toString();
                final String team2_score = team2_score_edit.getText().toString();
                System.out.println(team1_score);
                System.out.println(team2_score);
                new ApiUpdateScore().execute(tournamentId, matchId, team1_score, team2_score);
            }
        });
        return rootView;
    }

    private class ApiUpdateScore extends AsyncTask<String, Integer, Integer> {

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

        private String getCredentials(String type){
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            if(type.equals("login")) return sharedPref.getString("login", "");
            else if(type.equals("password")) return sharedPref.getString("password", "");
            else return "Error occurred";
        }

        @Override
        protected Integer doInBackground(String... parameters) {
            String username = getCredentials("login");
            String password = getCredentials("password");
            String name = parameters[0];
            String id = parameters[1];
            String state = "finished";
            Integer pointsTeam1 = Integer.parseInt(parameters[2]);
            Integer pointsTeam2 =Integer.parseInt( parameters[3]);
            String winner = "";
            if(pointsTeam1 > pointsTeam2) winner = team1;
            if(pointsTeam2 > pointsTeam1) winner = team2;
            if(pointsTeam1.equals(pointsTeam2)) winner = "draft?";

            try {
                String Request = "http://kormoran.educationhost.cloud/api/matches.php";
                URL url = new URL(Request);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                String auth =
                        "{" +
                                "\"username\":\"" + username + "\"," +
                                "\"password\":\"" + password + "\"," +
                                "\"tournament\":\"" + name + "\"," +
                                "\"id\":\"" + id + "\"," +
                                "\"state\":\"" + state + "\"," +
                                "\"points_team_1\":\"" + pointsTeam1 + "\"," +
                                "\"points_team_2\":\"" + pointsTeam2 + "\"," +
                                "\"winner\":\"" + winner + "\"" +
                                "}";
                System.out.println(auth);
                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = auth.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int status = con.getResponseCode();
                if (status == 200) {
                    return 200;
                }
                if(status == 400){
                    //Bad Request
                    //Probably problem with parameters
                    return 400;
                }
                if(status == 403){
                    //Forbidden
                    //Wrong login/password
                    return 403;
                }
                if(status == 404){
                    //Not Found
                    //Missing id and/or tournament parameter
                    return 404;
                }
                if(status == 501){
                    //Not Implemented
                    //Not POST/GET method
                    return 501;
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
            return 400;
        }

        @Override
        protected void onPostExecute(Integer result) {
            showDialog(result);
        }

        private void showDialog(Integer result){
            Context context = getActivity().getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            CharSequence text;
            if(result == 200){
                text = "Wynik meczu zaktualizowany pomyślnie";
            }else{
                text = "Nie udało się zaktualizować wyniku meczu!" + result;
            }
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}
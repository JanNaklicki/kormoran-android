package com.example.administrator.kormorantest;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;


public class TournamentListFragment extends android.app.Fragment {
    ListView listViewTournaments;
    ArrayList<Tournament> tournaments = new ArrayList<>();
    public TournamentListFragment(){
        //required empty constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_turnament_list, container,false);
        listViewTournaments = rootView.findViewById(R.id.listViewTournaments);
        getActivity().getActionBar().setTitle("Turnieje");
        getActivity().getActionBar().setSubtitle(null);
        new Api().execute();
        listViewTournaments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                android.app.FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                android.app.Fragment TournamentFragment = new TournamentFragment();
                ((TournamentFragment) TournamentFragment).tournamentId = tournaments.get(i).mId;
                ((TournamentFragment) TournamentFragment).tournamentRepName = tournaments.get(i).mName;
                ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
                ft.replace(R.id.content_frame, TournamentFragment).addToBackStack("match").commit();
                ((MainActivity) getActivity()).changeDrawerAvailibility(false);
            }
        });
        return rootView;
    }
    private class Tournament {
        String mId;
        String mName;
        String mState;
        int mImage;
        int mStateImage;

        public Tournament(String Id, String Name, String State, int image, int StateImage){
            this.mId = Id;
            this.mName = Name;
            this.mState = State;
            this.mImage = image;
            this.mStateImage = StateImage;
        }
    }
    String statePl;
    int imagePl;
    int circlePl;
    //TODO: create interface to handle with LoginApi class;
    //      create function check(username, password) for user validation
    private class LoginApi extends AsyncTask<String, Integer, Boolean> {
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
        protected Boolean doInBackground(String... pass) {
            String username = pass[0];
            String password = pass[1];
            try {
                String Request = "http://kormoran.educationhost.cloud/api/administrate.php";
                URL url = new URL(Request);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                String auth = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = auth.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int status = con.getResponseCode();
                if (status == 200) {
                    return true;
                }
                if(status == 400){
                    return false;
                }
                if(status == 403){
                    return false;
                }
                if(status == 405){
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

        }
    }

    private class Api extends AsyncTask<URL, Integer, ArrayList<Tournament>> {
        protected ArrayList<Tournament> doInBackground(URL... urls) {
            String name;
            String game;
            String state;
            String rep_name;
            ArrayList<Tournament> completedList = new ArrayList<>();
            ArrayList<Tournament> activeTournamentsList = new ArrayList<>();
            ArrayList<Tournament> readyToPlayTournamentsList = new ArrayList<>();
            ArrayList<Tournament> finishedTournamentsList = new ArrayList<>();
            try {
                URL url = new URL("http://kormoran.educationhost.cloud/api/tournaments.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int status = con.getResponseCode();
                if (status == 200) {
                    InputStream is = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String responseString;
                    StringBuilder sb = new StringBuilder();
                    while ((responseString = reader.readLine()) != null) {
                        sb = sb.append(responseString);
                    }
                    String rawData = sb.toString();
                    JSONObject data = new JSONObject(rawData);
                    JSONArray tournamentsArray = data.optJSONArray("tournaments");
                    for (int i = 0; i < tournamentsArray.length(); i++) {
                        JSONObject tournament = (JSONObject) tournamentsArray.get(i);
                        name = tournament.optString("name", "");
                        game = tournament.optString("game", "");
                        state = tournament.optString("state", "");
                        rep_name = tournament.optString("rep_name", "");
                        switch (game) {
                            case "football":
                                imagePl = R.drawable.football;
                                break;
                            case "handball":
                                imagePl = R.drawable.handball;
                                break;
                            case "volleyball":
                                imagePl = R.drawable.volleyball;
                                break;
                            case "basketball":
                                imagePl = R.drawable.basketball;
                                break;
                            case "tugofwar":
                                imagePl = R.drawable.football2;
                                break;
                        }
                        switch (state) {
                            case "active":
                                statePl = "Trwający";
                                circlePl = R.drawable.filled_circle_2;
                                activeTournamentsList.add(new Tournament(name, rep_name, statePl, imagePl, circlePl));
                                break;

                            case "ready-to-play":
                                statePl = "Oczekujący";
                                circlePl = R.drawable.filled_circle;
                                readyToPlayTournamentsList.add(new Tournament(name, rep_name, statePl, imagePl, circlePl));
                                break;

                            case "finished":
                                statePl = "Zakończony";
                                circlePl = R.drawable.filled_circle_3;
                                finishedTournamentsList.add(new Tournament(name, rep_name, statePl, imagePl, circlePl));
                                break;
                        }
                    }
                    completedList.addAll(activeTournamentsList);
                    completedList.addAll(readyToPlayTournamentsList);
                    completedList.addAll(finishedTournamentsList);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch(NetworkOnMainThreadException e) {
                e.printStackTrace();
            } finally {
                //connection.disconnect();
            }
            return completedList;
        }

        protected void onPostExecute(ArrayList<Tournament> result) {
            listViewTournaments.setAdapter(new TournamentAdapter(getActivity(), result));
            tournaments = result;
        }
    }

    public class TournamentAdapter extends BaseAdapter{
        Context mContext;
        ArrayList<Tournament> mTournaments;
        LayoutInflater mInflater;

        public TournamentAdapter(Context c, ArrayList<Tournament> tournaments){
            mContext = c;
            mTournaments = tournaments;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        public int getCount(){
            return mTournaments.size();
        }
        public Object getItem(int position){
            return mTournaments.get(position);
        }
        public long getItemId(int position){
            return position;
        }
        public View getView(int position, View convertView, ViewGroup parent){
            View view = mInflater.inflate(R.layout.tournament_view_item, null);
            TextView textViewName = (TextView) view.findViewById(R.id.tournamentName);
            TextView textViewState = (TextView) view.findViewById(R.id.tournamentState);
            ImageView imageViewTournamentImage = (ImageView) view.findViewById(R.id.tournamentImage);
            ImageView imageViewStateImage = (ImageView) view.findViewById(R.id.tournamentStateImage);

            Tournament currentTournament = mTournaments.get(position);
            textViewName.setText(currentTournament.mName);
            textViewState.setText(currentTournament.mState);
            imageViewTournamentImage.setImageResource(currentTournament.mImage);
            imageViewStateImage.setImageResource(currentTournament.mStateImage);

            return view;
        }
    }
}

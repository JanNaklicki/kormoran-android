package com.example.administrator.kormorantest;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import android.support.v7.app.AlertDialog;

import javax.net.ssl.HttpsURLConnection;


public class TournamentFragment extends android.app.Fragment {

    String tournamentId;
    String tournamentRepName;
    ArrayList<Match> matches;
    ListView listViewMatches;
    public TournamentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_tournament, container,false);
        listViewMatches = rootView.findViewById(R.id.listViewMatches);
        getActivity().getActionBar().setTitle(tournamentRepName);
        getActivity().getActionBar().setSubtitle(null);
        new Api().execute();
        if(isLoginDataStored()) {
            listViewMatches.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    android.app.FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    android.app.Fragment MatchFragment = new MatchFragment();
                    ((MatchFragment) MatchFragment).matchId = matches.get(i).mId;
                    ((MatchFragment) MatchFragment).tournamentId = tournamentId;
                    ((MatchFragment) MatchFragment).tournamentRepName = tournamentRepName;
                    ((MatchFragment) MatchFragment).team1 = matches.get(i).mTeamName1;
                    ((MatchFragment) MatchFragment).team2 = matches.get(i).mTeamName2;
                    ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
                    ft.replace(R.id.content_frame, MatchFragment).addToBackStack("match").commit();
                    ((MainActivity) getActivity()).changeDrawerAvailibility(false);
                    getActivity().getActionBar().setSubtitle(matches.get(i).mTeamName1 + " vs " + matches.get(i).mTeamName2);
                }
            });
            listViewMatches.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final View sender = view;
                    final String matchId = matches.get(i).mId;
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(), R.style.Theme_AppCompat_Dialog_Alert);
                    builder.setTitle("Zmień status meczu");
                    builder.setPositiveButton("Oczekujący", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setMatchStatusToReady(sender, matchId);
                        }
                    });
                    builder.setNegativeButton("Trwa", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setMatchStatusToActive(sender, matchId);
                        }
                    });
                    builder.setNeutralButton("Zakończony", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setMatchStatusToFinished(sender, matchId);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }
            });
        }
        else {
            notPermittedToast();
        }
        return rootView;
    }

    public void setMatchStatusToReady(View Sender, String matchId){
        new ApiUpdateState(Sender,"ready-to-play").execute(tournamentId, matchId);
    }
    public void setMatchStatusToActive(View Sender, String matchId){
        new ApiUpdateState(Sender, "active").execute(tournamentId, matchId);
    }
    public void setMatchStatusToFinished(View Sender, String matchId){
        new ApiUpdateState(Sender, "finished").execute(tournamentId, matchId);
    }

    private class Match {
        String mId;
        String mTeamName1;
        String mTeamName2;
        String mState;
        String mScores;

        private Match(String Id, String TeamName1, String TeamName2, String State, String Scores) {
            this.mId = Id;
            this.mTeamName1 = TeamName1;
            this.mTeamName2 = TeamName2;
            this.mState = State;
            this.mScores = Scores;
        }
    }

    private String getCredentials(String type){
        SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        if(type.equals("login")) return sharedPref.getString("login", "");
        else if(type.equals("password")) return sharedPref.getString("password", "");
        else return "Error occurred";
    }

    private class Api extends AsyncTask<URL, Integer, ArrayList<Match>> {
        @Override
        protected ArrayList<Match> doInBackground(URL... urls) {
            String state;
            String team_1;
            String team_2;
            String winner;
            String match_id;
            String points_team_1;
            String points_team_2;
            ArrayList<Match> completedList = new ArrayList<>();
            ArrayList<Match> activeMatchesList = new ArrayList<>();
            ArrayList<Match> readyToPlayMatchesList = new ArrayList<>();
            ArrayList<Match> finishedMatchesList = new ArrayList<>();
            try {
                String Request = "http://kormoran.educationhost.cloud/api/matches.php?tournament=";
                Request = Request.concat(tournamentId);
                URL url = new URL(Request);
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
                    JSONArray tournamentsArray = data.optJSONArray("matches");
                    for (int i = 0; i < tournamentsArray.length(); i++) {
                        JSONObject tournament = (JSONObject) tournamentsArray.get(i);
                        state = tournament.optString("state", "");
                        team_1 = tournament.optString("team_1", "");
                        team_2 = tournament.optString("team_2", "");
                        match_id = tournament.optString("match_id");
                        points_team_1 = tournament.optString("points_team_1", "");
                        points_team_2 = tournament.optString("points_team_2", "");
                        String points;
                        if(points_team_1.equals("null")) points = "";
                        else points =  points_team_1 + " - " + points_team_2;
                        if(state.equals("active")) activeMatchesList.add(new Match(match_id, team_1, team_2, state, points));
                        if(state.equals("ready-to-play")) readyToPlayMatchesList.add(new Match(match_id, team_1, team_2, state, points));
                        if(state.equals("finished")) finishedMatchesList.add(new Match(match_id, team_1, team_2, state, points));
                    }
                    completedList.addAll(activeMatchesList);
                    completedList.addAll(readyToPlayMatchesList);
                    completedList.addAll(finishedMatchesList);
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
            }
            return completedList;
        }
        @Override
        protected void onPostExecute(ArrayList<Match> result) {
            listViewMatches.setAdapter(new MatchAdapter(getActivity(), result));
            matches = result;
        }
    }

    private class ApiUpdateState extends AsyncTask<String, Integer, Integer> {

        private View sender;
        private String desiredState;

        public ApiUpdateState(View Sender, String DesiredState){
            this.sender = Sender;
            this.desiredState = DesiredState;
        }

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
        protected Integer doInBackground(String... parameters) {
            String username = getCredentials("login");
            String password = getCredentials("password");
            String name = parameters[0];
            String state = this.desiredState;
            String id = parameters[1];
            try {
                String Request = "http://kormoran.educationhost.cloud/api/matches.php";
                URL url = new URL(Request);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);
                String auth =
                        "{" +
                            "\"username\":\"" + username + "\"," +
                            "\"password\":\"" + hash(password) + "\"," +
                            "\"tournament\":\"" + name + "\"," +
                            "\"id\":\"" + id + "\"," +
                            "\"state\":\"" + state + "\"" +
                        "}";
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
            if (result == 200) {
                switch (this.desiredState) {
                    case "ready-to-play":
                        sender.setBackgroundColor(getResources().getColor(R.color.match_pending));
                        break;
                    case "active":
                        sender.setBackgroundColor(getResources().getColor(R.color.match_playing));
                        break;
                    case "finished":
                        sender.setBackgroundColor(getResources().getColor(R.color.match_finished));
                        break;
                    default:
                        sender.setBackgroundColor(getResources().getColor(R.color.match_finished));

                }
            }
            showDialog(result);
        }


        private void showDialog(Integer result){
            Context context = getActivity().getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            CharSequence text;
            if(result == 200){
                text = "Status meczu zaktualizowany pomyślnie";
            }else{
                text = "Nie udało się zaktualizować statusu meczu!" + result;
            }
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    public boolean isLoginDataStored(){
        SharedPreferences sharedPref = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        String loginPref = sharedPref.getString("login", "");
        String passwordPref = sharedPref.getString("password", "");
        return (!loginPref.equals("") && !passwordPref.equals(""));
    }

    private void notPermittedToast(){
        Context context = getActivity().getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        CharSequence text = "Musisz być zalogowany aby modyfikować mecze";
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public class MatchAdapter extends BaseAdapter {
        Context mContext;
        ArrayList<Match> mMatches;
        LayoutInflater mInflater;

        private MatchAdapter(Context c, ArrayList<Match> matches){
            mContext = c;
            mMatches = matches;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        public int getCount(){
            return mMatches.size();
        }
        public Object getItem(int position){
            return mMatches.get(position);
        }
        public long getItemId(int position){
            return position;
        }
        public View getView(int position, View convertView, ViewGroup parent){
            View view = mInflater.inflate(R.layout.match_view_item, null);
            TextView teamName1 = view.findViewById(R.id.teamName1);
            TextView teamName2 = view.findViewById(R.id.teamName2);
            TextView scores = view.findViewById(R.id.matchScores);

            Match currentMatch = mMatches.get(position);
            teamName1.setText(currentMatch.mTeamName1);
            teamName2.setText(currentMatch.mTeamName2);
            scores.setText(currentMatch.mScores);
            if(currentMatch.mState.equals("ready-to-play")) view.setBackgroundResource(R.color.match_pending);
            if(currentMatch.mState.equals("active")) view.setBackgroundResource(R.color.match_playing);
            if(currentMatch.mState.equals("finished")) view.setBackgroundResource(R.color.match_finished);

            return view;
        }
    }
}

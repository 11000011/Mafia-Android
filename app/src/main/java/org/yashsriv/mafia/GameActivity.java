package org.yashsriv.mafia;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

public class GameActivity extends AppCompatActivity {

    Game game;
    WebClient wc;
    String server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent intent = getIntent();
        server = intent.getStringExtra(MainActivity.SERVER);
        String username = intent.getStringExtra(MainActivity.NICK);
        game = new Game(username);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Sec-Websocket-Protocol", username);
        URI u;
        toast("Welcome to mafia");
        try {
            u = new URI(server);
        } catch (URISyntaxException e) {
            // TODO: Decline Message
            u = null;
            e.printStackTrace();
        }
        wc = new WebClient(u, headers);
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            wc.connect();
        } else {
            // TODO: Display internet connction error
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void handleMessage(final String message) {
        if (message.matches("#NAMES:.*")) {
            String names[] = message.split("#NAMES:")[1].split(",");
            Hashtable<String, Boolean> namesArray = game.getUsers();
            for (String name : names) {
                namesArray.put(name, true);
            }
        }

        if (message.matches("#TYPE:.*")) {
            switch (message.split(":")[1]) {
                case "Victim":
                    game.setType(Game.TYPE.VICTIM);
                    toast("You are a normal citizen");
                    break;
                case "Mafia":
                    game.setType(Game.TYPE.MAFIA);
                    toast("You are a mafia member");
                    wc.send("#LOADED_MAFIA_JS");
                    break;
                case "Detective":
                    game.setType(Game.TYPE.DETECTIVE);
                    toast("You are a responsible detective");
                    wc.send("#LOADED_DETECTIVE_JS");
                    break;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.type_label)).setText(message.split(":")[1]);
                }
            });

        }

        if (message.matches("#VOTE:.*")) {
            String users[] = message.split("#VOTE:")[1].split(":");
            game.getVoteState().put(users[0], users[1]);
            updateVotingSystem(users[0], users[1]);  // voter, votee
        }

        if (message.matches("#KILLED:.*")) {
            game.getUsers().put(message.split("#KILLED:")[1], false);
            if (game.getTeamNames().contains(message.split("#KILLED:")[1])) {
                game.getTeamNames().remove(message.split("#KILLED:")[1]);
            }
            showToast(message.split("#KILLED:")[1]);
            updateActivity();
        }

        if (message.matches("#ELIMINATED:.*")) {
            String userEliminated = message.split(":")[1];
            Boolean wasMafia = Boolean.valueOf(message.split(":")[2]);

            game.getUsers().put(userEliminated, false);
            if (game.getTeamNames().contains(userEliminated)) {
                game.getTeamNames().remove(userEliminated);
            }

            showToast(userEliminated, wasMafia);
            updateActivity();
        }

        if (message.matches("#VOTE_ANON") || message.matches("#VOTE_OPEN")) {
            toast("Voting Round!!!!!");
            startVotingFragment();
        }

        if (message.matches("#DISCUSSION:.*")) {
            toast("Discussion Round");
            String users[] = message.split("#DISCUSSION:")[1].split(",");
            startDiscussionFragment(users[0], users[1]);  // Highest, Second Highest
        }

        if (message.matches("#WIN:.*")) {
            String winners = message.split(":")[1];
            toast(winners + " WIN!!!");
            finish();
        }

    }

    void handleDetectiveMessage(String message) {

        if (message.matches("#DETECTIVE_NAMES:.*")) {
            String names[] = message.split("#DETECTIVE_NAMES:")[1].split(",");
            ArrayList<String> namesArray = game.getTeamNames();
            Collections.addAll(namesArray, names);
            updateActivity();
        }

        if (message.matches("#DETECTIVE_VOTE")) {
            toast("Detective Voting Round");
            startVotingFragment();
        }

        if (message.matches("#DETECTION_RESULT:.*")) {
            String detective_result = message.split("#DETECTION_RESULT")[1];
            showDetectiveToast(detective_result.split(":")[0], Boolean.valueOf(detective_result.split(":")[1]));
        }

    }

    void handleMafiaMessage(String message) {

        if (message.matches("#MAFIA_NAMES:.*")) {
            String names[] = message.split("#MAFIA_NAMES:")[1].split(",");
            ArrayList<String> namesArray = game.getTeamNames();
            Collections.addAll(namesArray, names);
            updateActivity();
        }

        if (message.matches("#MAFIA_VOTE")) {
            toast("Mafia voting round");
            startVotingFragment();
        }

    }

    public class WebClient extends WebSocketClient {

        WebClient(URI serverURI, Map<String, String> httpHeaders) {
            super(serverURI, new Draft_17(), httpHeaders, 0);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("opened connection");
            initActivity();
        }

        @Override
        public void onMessage(String message) {

            System.out.println(message);

            handleMessage(message);

            if (game.getType() == Game.TYPE.DETECTIVE) {
                handleDetectiveMessage(message);
            }
            if (game.getType() == Game.TYPE.MAFIA) {
                handleMafiaMessage(message);
            }

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            toast("Closed connection \t" + code + "\t" + reason);
        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void toast(final String message) {
        final Context c = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(c, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showToast(final String username) {
        final Context c = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence text = username + " was killed in the previous round.";
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(c, text, duration).show();
            }
        });

    }

    public void showToast(final String username, final Boolean wasMafia) {
        final Context c = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence text = username + " was killed in the previous round. He was " + (wasMafia ? "" : "not ") + "a mafia member.";
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(c, text, duration).show();
            }
        });

    }

    public void showDetectiveToast(final String username, final Boolean wasMafia) {
        final Context c =this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence text = username + " is " + (wasMafia ? "" : "not ") + "a mafia member.";
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(c, text, duration).show();
            }
        });
    }

    public void initActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView username = (TextView) findViewById(R.id.username_label);
                username.setText(game.getUsername());
            }
        });

    }

    public void updateActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.team_members)).setText("Team: " + game.getTeamNames().toString());
            }
        });
    }

    public void startVotingFragment() {

        final RadioGroup rg = new RadioGroup(this); //create the RadioGroup
        rg.setId(100 + 1);
        String message = "";
        Enumeration<String> en = game.getUsers().keys();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (game.getUsers().get(key)) {
                RadioButton rb = new RadioButton(this);
                rb.setText(key);
                rb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Is the button now checked?
                        boolean checked = ((RadioButton) v).isChecked();
                        if (checked) {
                            System.out.println("Vote Sent - " + ((RadioButton) v).getText().toString());
                            wc.send("#VOTE:" + ((RadioButton) v).getText());
                        }
                    }
                });
                Enumeration<String> votes = game.getVoteState().keys();
                String attackers = "";
                while (votes.hasMoreElements()) {
                    String k = (String) votes.nextElement();
                    if (game.getVoteState().get(k).contentEquals(key)) {
                        attackers += k + ", ";
                    }
                }
                message += key + " : " + attackers + "\n";
                rg.addView(rb);
            }
        }
        final String strMessage = message;
        final TextView textView = new TextView(this);
        textView.setText(strMessage);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.voting_state);
                linearLayout.removeAllViews();
                linearLayout.addView(textView);
                ((LinearLayout) findViewById(R.id.voting)).addView(rg);
                new CountDownTimer(15000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        ((TextView) findViewById(R.id.timer)).setText("" + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        ((LinearLayout) findViewById(R.id.voting)).removeAllViews();
                        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.voting_state);
                        linearLayout.removeAllViews();
                        ((TextView) findViewById(R.id.timer)).setText("");
                        game.clearVoteState();
                        wc.send("#DONE_VOTING");
                    }
                }.start();
            }
        });


    }

    public void updateVotingSystem(String voter, String votee) {

        String message = "";
        Enumeration<String> en = game.getUsers().keys();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();

            Enumeration<String> votes = game.getVoteState().keys();
            String attackers = "";
            while (votes.hasMoreElements()) {
                String k = (String) votes.nextElement();
                if (game.getVoteState().get(k).contentEquals(key)) {
                    attackers += k + ", ";
                }
            }
            message += key + " : " + attackers + "\n";
        }
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.voting_state);
        final TextView textView = new TextView(this);
        textView.setText(message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayout.removeAllViews();
                linearLayout.addView(textView);
            }
        });
    }

    public void startDiscussionFragment(String user1, String user2) {
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.discuss);

        final TextView discussion = new TextView(this);
        discussion.setText("It's time to discuss. " + user1 + " and " + user2 + " are under the most suspicion. They speak first, then press the button when you've spoken to your satisfaction");
        final Button b = new Button(this);
        b.setText("Done");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearLayout.removeAllViews();
                    }
                });
                wc.send("#DONE_DISCUSSION");
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayout.addView(discussion);
                linearLayout.addView(b);
            }
        });

    }
}
package org.yashsriv.mafia;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
            // display error
            System.out.println("No internet connection");
        }
    }

    void handleDetectiveMessage(String message) {
        if (message.matches("#DETECTIVE_NAMES:")) {
            String names[] = message.split("#DETECTIVE_NAMES:")[1].split(",");
            ArrayList<String> namesArray = game.getTeamNames();
            Collections.addAll(namesArray, names);
        }
        if (message.matches("#DETECTIVE_VOTE")) {
            startVotingFragment();
        }
        if (message.matches("#DETECTION_RESULT:")) {
            String detective_result = message.split("#DETECTION_RESULT")[1];
            showDetectiveToast(detective_result.split(":")[0], detective_result.split(":")[1]);
        }
    }

    void handleMafiaMessage(String message) {
        if (message.matches("#MAFIA_NAMES:")) {
            String names[] = message.split("#MAFIA_NAMES:")[1].split(",");
            ArrayList<String> namesArray = game.getTeamNames();
            Collections.addAll(namesArray, names);
        }
        if (message.matches("#MAFIA_VOTE")) {
            startVotingFragment();
        }
    }

    public class WebClient extends WebSocketClient {

        public WebClient(URI serverURI, Map<String, String> httpHeaders) {
            super(serverURI, new Draft_17(), httpHeaders, 0);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println( "opened connection" );
        }

        @Override
        public void onMessage(String message) {
            System.out.println(message);
            if (message.matches("#NAMES:.*")) {
                String names[] = message.split("#NAMES:")[1].split(",");
                Hashtable<String, Boolean> namesArray = game.getUsers();
                for (String name : names) {
                    namesArray.put(name, true);
                }
            }
            if (message.matches("#TYPE:")) {
                switch (message.split(":")[1]) {
                    case "Victim":
                        game.setType(Game.TYPE.VICTIM);
                        break;
                    case "Mafia":
                        game.setType(Game.TYPE.MAFIA);
                        wc.send("#LOADED_MAFIA_JS");
                        break;
                    case "Detective":
                        game.setType(Game.TYPE.DETECTIVE);
                        wc.send("#LOADED_DETECTIVE_JS");
                        break;
                }
            }
            if (message.matches("#VOTE:")) {
                String users[] = message.split("#VOTE:")[1].split(":");
                if (!(game.getRound() == Game.ROUND.ANON || game.getRound() == Game.ROUND.WAIT || game.getRound() == Game.ROUND.DISCUSS)) {
                    updateVotingSystem(users[0], users[1]);  // voter, votee
                }
            }

            if (message.matches("#KILLED:")) {
                game.getUsers().put(message.split("#KILLED:")[1], false);
                if (game.getTeamNames().contains(message.split("#KILLED:")[1])) {
                    game.getTeamNames().remove(message.split("#KILLED:")[1]);
                }
                showToast(message.split("#KILLED:"));
            }

            if (message.matches("#ELIMINATED:")) {
                String userEliminated = message.split(":")[1];
                Boolean wasMafia = Boolean.valueOf(message.split(":")[2]);

                game.getUsers().put(userEliminated, false);
                if (game.getTeamNames().contains(userEliminated)){
                    game.getTeamNames().remove(userEliminated);
                }

                showToast(userEliminated, wasMafia);
            }

            if (message.matches("#VOTE_ANON") || message.matches("#VOTE_OPEN")) {
                startVotingFragment();
            }

            if (message.matches("#DISCUSSION:")) {
                String users[] = message.split("#DISCUSSION:")[1].split(",");
                startDiscussionFragment(users[0], users[1]);  // Highest, Second Highest
            }

            if (game.getType() == Game.TYPE.DETECTIVE) {
                handleDetectiveMessage(message);
            }
            if (game.getType() == Game.TYPE.MAFIA) {
                handleMafiaMessage(message);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println( "closed connection \t" + code + "\t" + reason );
        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }
    }
}

package org.yashsriv.mafia;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Created by yash on 1/10/16.
 */

public class Game {

    public static enum TYPE {
        VICTIM, DETECTIVE, MAFIA
    }
    public static enum ROUND {
        WAIT, MAFIA, DETECTIVE, ANON, DISCUSS, OPEN
    }

    private String username;

    private TYPE type;

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    private Hashtable<String, Boolean> users;

    public Hashtable<String, Boolean> getUsers() {
        return users;
    }

    private ROUND round;

    public ROUND getRound() {
        return round;
    }

    public void setRound(ROUND round) {
        this.round = round;
    }

    private Hashtable<String, String> voteState;

    public Hashtable<String, String> getVoteState() {
        return voteState;
    }

    private ArrayList<String> teamNames;

    public ArrayList<String> getTeamNames() {
        return teamNames;
    }

    public void setTeamNames(ArrayList<String> teamNames) {
        this.teamNames = teamNames;
    }

    public Game(String username) {
        this.username = username;
        type = TYPE.VICTIM;
        round = ROUND.WAIT;
        users = new Hashtable<String, Boolean>();
        voteState = new Hashtable<String, String>();
        teamNames = new ArrayList<String>();
    }
}

package org.yashsriv.mafia;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public final static String NICK = "org.yashsriv.mafia.NICK";
    public final static String SERVER = "org.yashsriv.mafia.SERVER";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {

        }
    }

    public void startGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        EditText editText = (EditText) findViewById(R.id.nickname_set);
        String nickname = editText.getText().toString();
        intent.putExtra(NICK, nickname);
        editText = (EditText) findViewById(R.id.server_set);
        String server = editText.getText().toString();
        intent .putExtra(SERVER, server);
        startActivity(intent);
    }
}
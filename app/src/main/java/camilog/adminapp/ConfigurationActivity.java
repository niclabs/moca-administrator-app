package camilog.adminapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import camilog.adminapp.db.ElectionManager;
import camilog.adminapp.elections.Candidate;
import camilog.adminapp.elections.Election;
import camilog.adminapp.elections.ElectionHolder;
import camilog.adminapp.serverapi.BBServer;

/**
 * Created by stefano on 04-09-15.
 * Modified by diego on 21-01-16.
 */
public class ConfigurationActivity extends Activity {

    private final String addCandidateTitle = "Add candidate";
    private final String addCandidateNameMessage = "Candidate name";
    private ElectionHolder _electionHolder;
    private Election _election;
    private ElectionManager _electionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_layoutv2);
        try{
            initElectionManager();
            initElectionHolder();
            initElection();
            setupLayout();
            addOnClickListeners();
        }catch(ElectionHolder.ElectionNotFoundException e){
            Log.i("jiji", "Error loading elections: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupLayout(){
        TextView tv = (TextView) findViewById(R.id.bb_server_edit);
        tv.setText(_election.getBBServer());
    }

    private void initElection() throws ElectionHolder.ElectionNotFoundException{
        _election = _electionHolder.getElectionById(getIntent().getExtras().getLong(ElectionActivity.ELECTION_INFORMATION_ID));
    }

    private void initElectionHolder(){
        _electionHolder = ElectionHolder.getElectionHolder(_electionManager);
    }
    private void initElectionManager(){
        _electionManager = new ElectionManager(getApplicationContext());
    }

    private void updateElection(){
        _election.setBBServer(((TextView) findViewById(R.id.bb_server_edit)).getText().toString());
    }

    private void saveElectionToDb(){
        updateElection();
        _electionManager.updateElection(_election);
    }

    private void addOnClickListeners(){
        Button saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveElectionToDb();
            }
        });

        Button addCandidateButton = (Button) findViewById(R.id.new_election_add_candidate_button);
        addCandidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayAddCandidateDialog();
            }
        });

        //Button that will trigger connection to BB and download public authority key
        Button authKeyButton = (Button) findViewById(R.id.get_auth_key_button);
        authKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    public void run() {
                        try {
                            downloadAuthkey();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    //Download the public authority key from the BB in order to use it in the qr code creation
    //TODO: falta implementar caso en que llave aun no existe en el BB
    private void downloadAuthkey() throws IOException {
        String server = _election.getBBServer();
        BBServer bbServer = new BBServer(server);
        String response = bbServer.doJSONGETRequest(bbServer.getAddress() + "/" + bbServer.getAUTHORITY_PUBLIC_KEY_SUBDOMAIN() + "/" + bbServer.getALL_DOCS_SUBDOMAIN());
        File publicKeyDir = getApplicationContext().getDir("publicAuthKey", Context.MODE_PRIVATE);
        File publicKeyFile = new File(publicKeyDir, "publicAuthKey.key");

        if (publicKeyFile.exists()) {
            publicKeyFile.delete();
        }

        publicKeyFile.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(publicKeyFile, true));
        writer.write(response);
        writer.close();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeUI();
            }
        });

    }

    private void changeUI() {
        Toast toast = Toast.makeText(this, "Public Authority Key recorded successfully!", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, Gravity.CENTER_HORIZONTAL, 400);
        toast.show();
    }

    private void displayAddCandidateDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText candidateNameInput = new EditText(this);
        alert.setTitle(addCandidateTitle);
        alert.setMessage(addCandidateNameMessage);
        alert.setView(candidateNameInput);
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                _election.addCandidate(new Candidate(candidateNameInput.getText().toString()));
            }
        });
        alert.show();
    }
}

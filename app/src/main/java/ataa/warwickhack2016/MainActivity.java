package ataa.warwickhack2016;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Our main activity class, opening the right instance of the app
 */
public class MainActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    TextView textView;
    AdjustEnvironment adjustEnvironment;
    public static int studentID;
    public static boolean adjustWiFi;
    public static boolean adjustSound;
    public static boolean adjustBrightness;
    SharedPreferences settings;
    static SharedPreferences.Editor editor;
    String link;
    Button lecnoteButton;

    public static void saveSettings(int sstudentID, boolean aadjustWiFi, boolean aadjustSound, boolean aadjustBrightness)
    {
        adjustBrightness = aadjustBrightness;
        adjustWiFi = aadjustWiFi;
        adjustSound = aadjustSound;
        studentID = sstudentID;

        editor.putInt("studentID",studentID);
        editor.putBoolean("adjustWiFi",adjustWiFi);
        editor.putBoolean("adjustSound",adjustSound);
        editor.putBoolean("adjustBrightness",adjustBrightness);

        editor.commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // TODO -> EXIT
        // Just initialisation of the activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lecnoteButton = (Button)findViewById(R.id.lecnoteButton);
        lecnoteButton.setEnabled(false);
        textView = (TextView)findViewById(R.id.welcomeMessage);

        // Restore preferences
        settings = getPreferences(Context.MODE_PRIVATE);
        editor = settings.edit();
        studentID = settings.getInt("studentID", 0);
        adjustWiFi = settings.getBoolean("adjustWiFi", true);
        adjustSound = settings.getBoolean("adjustSound", true);
        adjustBrightness = settings.getBoolean("adjustBrightness", true);




        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        textView.setText("LOADING");

        final Intent intent = this.getIntent();

        // Checks whether the app was started by:
        //  - reading a tag  (which means we should communicate with the database and change phone settings)
        //  - the user (which means the welcome info and settings activity should start)

        if (intent.getAction().contains("NDEF")) {
            //TODO: connect to the database
            //TODO: check if the app has been set up aka studentID !=null ?

            // raw message from tag
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (rawMsgs != null) {

                // The tag
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                // its records
                NdefRecord[] records = Ndef.get(tag).getCachedNdefMessage().getRecords();

                // the info on it in byte shape
                byte[] payload = records[0].getPayload();

                // Get the Text Encoding
                String textEncoding;

                if ((payload[0] & 128) == 0)
                    textEncoding = "UTF-8";
                else
                    textEncoding = "UTF-16";

                // Get the Language Code
                int languageCodeLength = payload[0] & 0063;

                try {
                    // the configured message on the tag
                    String roomID = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);

                    SqlActivity sql = new SqlActivity(MainActivity.this);

                    int moduleID = 0;

                    moduleID = sql.getModule(Integer.parseInt(roomID));

                    textView.setText("Welcome to " + sql.getModuleName(moduleID) + "! Your lecturer is " + sql.getLecturerName(moduleID));

                    sql.setPurpose(studentID);

                    link = sql.getLecnotes(moduleID);

                    lecnoteButton.setEnabled(true);
                }

                //should not happen if the tag is configured properly
                catch (UnsupportedEncodingException e) {
                    //Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e) {
                    System.out.println("invalid tag " + e.toString());
                }
            }

        } // end of if

        else
        {
            Intent configIntent = new Intent(MainActivity.this, ConfigActivity.class);
            startActivity(configIntent);
        }

    }

    public void showLecnotes(View view)
    {
        if(link!=null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(browserIntent);
        }
    }

}

package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    int count=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.d(TAG, "Can create a ServerSocket");

        } catch (IOException e) {

            Log.d(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.edit_text);

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");// This is one way to reset the input box.
                    TextView tv = (TextView) findViewById(R.id.textView1);

                    tv.setMovementMethod(new ScrollingMovementMethod());
                    tv.append("\t"+msg);
                    tv.append("\n");
                    Log.d(TAG,msg);

                     findViewById(R.id.button1).setOnClickListener(
                          new OnPTestClickListener(tv, getContentResolver()));


                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");// This is one way to reset the input box.
                TextView tv = (TextView) findViewById(R.id.textView1);

                tv.setMovementMethod(new ScrollingMovementMethod());
                tv.append("\t"+msg);
                tv.append("\n");


                findViewById(R.id.button1).setOnClickListener(
                        new OnPTestClickListener(tv, getContentResolver()));

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                Log.d("SetOnclickListener","Successful");

            }
        });

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {

            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }
        private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");



            @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try {
                while (true) {
                    Socket socket = serverSocket.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = in.readLine();
                    Log.d("T", message);

                    in.close();
                    socket.close();
                    publishProgress(message);

                    count++;
                    ContentValues contentValues1 = new ContentValues();
                    contentValues1.put("key",Integer.toString(count));
                    contentValues1.put("value",message);

                    getContentResolver () . insert ( mUri ,
                            contentValues1 ) ;

                    
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            Log.d("StringReceived", strReceived);


            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append("\t\n"+strReceived);
            tv.append("\n");
            Log.d("Displayed", "DisplayedMessage");

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override

        protected Void doInBackground(String... msgs) {
            try {
                Log.d(TAG, "Reached ClientTask Method1");


                String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};


                for (int i = 0; i < remotePort.length; i++) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));

                    String msgToSend = msgs[0];
                    PrintWriter out =
                            new PrintWriter(socket.getOutputStream(), true);
                    out.write(msgToSend);
                    out.flush();
                    out.close();
                    socket.close();


                }

            } catch(UnknownHostException e){
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch(IOException e){
                    Log.e(TAG, "ClientTask socket IOException");
                }

            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}

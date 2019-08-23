package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
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

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    int seqId=0;
    private final Uri mUri=buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
       final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());




        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         *
         * https://developer.android.com/reference/android/widget/Button
         */


        /*References for the code::
        * The following complete code right from creating a server socket and creating a Server Task
        * as well as Client Task has been taken from::
        * PA1: SimpleMessengerActivity.java
        */


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            serverSocket.setReuseAddress(true);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        Log.d(TAG, "Hellooo");


        /*References for the code::
         * The following code for creating an OnClickListener for button has been taken from:
         * https://developer.android.com/reference/android/widget/Button
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);
        Log.d(TAG, "Before onClick");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "in onClick()");
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                //tv.append(" " + msg); // This is one way to display a string.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                // Code here executes on main thread after user presses button
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /*References for the code::
     * The following complete code of creating a Server Task:
     * PA1: SimpleMessengerActivity.java
     */

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while(true) {

                /*
                 * TODO: Fill in your server code that receives messages and passes them
                 * to onProgressUpdate().
                 * Code referred from :1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                 *                     2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                 */
                String input;
                String ackName="token";
                try {
                    Socket clientSocket = serverSocket.accept();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    input = br.readLine();
                    System.out.println("And then here?"+input);

                    publishProgress(input);

                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream());

                out.println(ackName);
                out.flush();
                    //                out.close();
                    //if (isCancelled()) break;
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");
                }
               // return null;
            }


        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
           // TextView localTextView = (TextView) findViewById(R.id.local_text_display);
           // localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            /*References for the code::
             * The following code for ContentValues has been taken from:
             * OnPTestClickListener.java
             */

            ContentValues cv = new ContentValues();
            cv.put(KEY_FIELD, Integer.toString(seqId) );
            cv.put(VALUE_FIELD, strReceived);
            System.out.println("Finally here::"+strReceived);
            getContentResolver().insert(mUri, cv);
            System.out.println("And now I am inserted::");
            seqId++;
            return;
        }
    }

    /*References for the code::
     * The following complete code of creating a Client Task:
     * PA1: SimpleMessengerActivity.java
     */

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                /*if (msgs[1].equals(REMOTE_PORT0))
                    remotePort = REMOTE_PORT1;*/
            for(int i =0; i<remotePort.length; i++) {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort[i]));

                String msgToSend = msgs[0];
                System.out.println("Am I here??"+msgs[0]);
                String input;

                /*
                 * TODO: Fill in your client code that sends out a message.
                 * Code for PrintWriter out and acknowledgement:
                 * 1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                 * 2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                 */
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                out.println(msgToSend);
                out.flush();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                input=br.readLine();
                if(input!=null && input.equalsIgnoreCase("token")) {
                    socket.close();
                }
                //socket.close();
            }

                
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


    /*References for the code::
     * The following code for buildUri has been taken from:
     * OnPTestClickListener.java
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}

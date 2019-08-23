package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity {
    //    int i=0;
    String clientPort="";
    String ack="";

    static int counter=0;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    ArrayList<String> serverlist=new ArrayList<String>();
    CopyOnWriteArrayList<Message> list= new CopyOnWriteArrayList<Message>();
    static final int SERVER_PORT = 10000;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    int lastPID=0;

    static int seqId=0;
    private final Uri mUri=buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    public int getIndex(String[] arr, String value){
        for(int i =0; i<arr.length; i++){
            if(arr[i]==value){
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        serverlist.add(REMOTE_PORT0);
        serverlist.add(REMOTE_PORT1);
        serverlist.add(REMOTE_PORT2);
        serverlist.add(REMOTE_PORT3);
        serverlist.add(REMOTE_PORT4);

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
        clientPort=myPort;
//        System.out.println("CLIENTPORT::"+clientPort);
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
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


        /*References for the code::
         * The following code for creating an OnClickListener for button has been taken from:
         * https://developer.android.com/reference/android/widget/Button
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
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

    /*
     *Under ServerTask, I am doing three things
     *1.Suggesting a proposal ID against every message and inserting each message's data in the arrayList.
     *2.Updating the maximum pID against the messages.
     *3.Failure Handling of the messages for which the maxPID is not decided and their status remains unchanged.
     * */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            //List<Message> list= new ArrayList<Message>();
            int id=0;
            BufferedReader br=null;
            ServerSocket serverSocket = sockets[0];
            long timeOut=5500;
            while(true) {
//                synchronized(this) {
                /*
                 * TODO: Fill in your server code that receives messages and passes them
                 * to onProgressUpdate().
                 * Code referred from :1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                 *                     2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                 */

                String input="";
                String ackName = "token";
                try {

                    ArrayList<Message> temp = new ArrayList<Message>();
                    temp.addAll(list);
                    Collections.sort(temp);

                    list.clear();            // 1st time list is locked
                    list.addAll(temp);
                    for(Message g: list) {
                        if ((System.currentTimeMillis() - g.millis) < timeOut){
                            break;
                        }
                        if ((System.currentTimeMillis() - g.millis) > timeOut && g.status.equals("NEW")) {
                            // System
                            g.setStatus("DELIVERED");
                            publishProgress(g.message);
                        }
                    }
                    for (Message m : list) {


                        if (m.status.equals("NEW")) {
                            break;
                        }
                        if (m.status.equals("PROPOSED")) {
                            m.setStatus("DELIVERED");
                            publishProgress(m.message);
                        }
                    }
                    serverSocket.setSoTimeout(1000);
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(1000);

                    try{
                        br = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                        input = br.readLine();
                    }catch (SocketTimeoutException e) {
                        // System.out.println("I am here inside socket:::");
                        // serverlist.remove(server);
                        Log.e(TAG, "SocketTimeoutException");
//                        e.printStackTrace();
                    } catch (StreamCorruptedException e){
                        Log.e(TAG, "StreamCorruptedException");
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ServerTask IOException");
                    }
                    PrintWriter out =
                            new PrintWriter(clientSocket.getOutputStream());
                    String[] parts = input.split("#");
                    int length = parts.length;
                    if (length == 2) {
                        String status = parts[0];
                        String message = parts[1];
                        counter+=1;
                        ack = String.valueOf(counter+ "." + Integer.parseInt(parts[0]));//(int)(Arrays.asList(remotePort).indexOf(clientPort)+1);
                        //long millis=System.currentTimeMillis();
//                        Log.d("Suggestion",String.valueOf(counter)+" "+message);
                        list.add(new Message(message, Double.parseDouble(ack), id, "NEW",System.currentTimeMillis()));
                        out.println(ack + "#" + id);
                        out.flush();
                        id++;
                    }
                    if (length == 3) {
                        String status = parts[0];
                        double proposed = Double.parseDouble(parts[1]);
                        int counterMax=(int)Math.round(proposed);
                        counter=Math.max(counterMax,(counter+1));
                        //int ID= Integer.parseInt(parts[2]);
                        String rec = parts[2];
                        //  PrintWriter out=new PrintWriter(clientSocket.getOutputStream());
                        for (Message m : list) {
                            if (m.id==Integer.parseInt(rec)) {
                                m.setStatus(status);
                                m.setPID(proposed);
                            }
                        }

                        ArrayList<Message> temp2 = new ArrayList<Message>();
                        temp2.addAll(list);
                        Collections.sort(temp2);

                        list.clear();            // 1st time list is locked
                        list.addAll(temp2);

                        int x = 0;
                        for (Message m : list) {


                            if (m.status.equals("NEW")) {
                                break;
                            }
                            if (m.status.equals(status)) {
                                m.setStatus("DELIVERED");
//                                System.out.println("SHRUTI" + m.message+" "+m.pID);
                                publishProgress(m.message);
                            }
                        }

                        out.println("PA2B");
                        out.flush();
                    }
                } catch (SocketTimeoutException e) {
                    // System.out.println("I am here inside socket:::");
                    // serverlist.remove(server);
                    Log.e(TAG, "SocketTimeoutException");
//                    e.printStackTrace();
                } catch (StreamCorruptedException e){
                    Log.e(TAG, "StreamCorruptedException");
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");
                }

//                }

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
            // System.out.println(seqId+" "+strReceived);
            cv.put(KEY_FIELD, Integer.toString(seqId++) );
            cv.put(VALUE_FIELD, strReceived);
            getContentResolver().insert(mUri, cv);

            return;
        }
    }

    /*References for the code::
     * The following complete code of creating a Client Task:
     * PA1: SimpleMessengerActivity.java
     */
    /*
     *Under ClientTask, I am doing two things majorly
     *1.Sending the message.
     *2.Deciding the final maximum pID for each message.
     *3.Failure Handling.
     * */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        // for every message, one clientTask is created and that task is or that message is reflected on five AVDs(including the sender). So, for every message
        // there are five server threads. We have to get response(proposal IDs) from all these five threads and choose the maximum one amongst them. Send it back to the server, and store
        // it in the hashmap of that AVD/ server for which the clientTask was created.
        @Override
        protected Void doInBackground(String... msgs) {
            String port=msgs[1];
            // System.out.println("Port is:::"+port);
            int k =0;
            boolean isAlive=false;
            int[] ID=new int[5];
            double maxValue=0.0;
            String Status="NEW";
            BufferedReader br = null;
            String server="";
            String input="";
            double[] suggested=new double[5];
            int failedIndex=-1, id=-1;
            String serverID="";
            try {
                
                for(int i =0; i<serverlist.size(); i++) {
                    server=serverlist.get(i);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(serverlist.get(i)));
                    socket.setSoTimeout(500);
                    String msgReceived = msgs[0];
                    String msgToSend=msgReceived;
                    //String input;
//                    System.out.println(msgReceived);

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     * Code for PrintWriter out and acknowledgement:
                     * 1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                     * 2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                     */
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    out.println(i+"#"+msgToSend);
                    out.flush();
                    try {
                        br = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        input = br.readLine();
                        String[] parts = input.split("#");
                        ack = parts[0];
                        //  System.out.println("ack:::::"+ack);
//                        String[] subParts=ack.split("\\.");
//                        // System.out.println("serverID::::1"+subParts[0]);
//                        //  System.out.println("serverID::::2"+subParts[1]);
//                        serverID=subParts[1];
                        // System.out.println("serverID::::"+serverID);
                        id = Integer.parseInt(parts[1]);

                        ID[i]=id;

                        suggested[i]=Double.parseDouble(ack);
                    }catch (SocketTimeoutException e) {
                        //System.out.println("I am here inside socket:::");
                        //failedIndex=i;
                        //serverlist.remove(server);
                        isAlive=false;
                        Log.e(TAG, "SocketTimeoutException");
//                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    }  catch (EOFException e){
                        Log.e(TAG, "EOF exception");
                    } catch (StreamCorruptedException e){
                        Log.e(TAG, "StreamCorruptedException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask IOException");
                    }catch(NullPointerException e){
                        Log.e(TAG, "Exception");
                    }

                    
                    if(ack!=null){
                        socket.close();
                    }
                }
                k=0;
                double max= Double.MIN_VALUE;
                for(int p=0;p<suggested.length;p++){
                    if(suggested[p]>max)
                        max=suggested[p];
                }
               // System.out.println(max+" "+msgs[0]);
                for(int j=0; j<serverlist.size(); j++) {
                    server=serverlist.get(j);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(serverlist.get(j)));
                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    try {
                        br = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                        Status = "PROPOSED";
                        out.println(Status + "#" + max + "#" + ID[j]);
                        out.flush();
                        while (!socket.isClosed()) {
                            if (br.readLine().equals("PA2B")) {
                                socket.close();
                            }
                        }
                    }catch (SocketTimeoutException e) {
                        // System.out.println("I am here inside socket:::");
                        //serverlist.remove(server);
                        //isAlive=false;
                        Log.e(TAG, "SocketTimeoutException");
//                        e.printStackTrace();
                    }catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    }catch (EOFException e){
                        Log.e(TAG, "EOF exception");
                    }catch (StreamCorruptedException e){
                        Log.e(TAG, "StreamCorruptedException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask IOException");
                    }catch(NullPointerException e){
                        Log.e(TAG, "Exception");
                    }
                }
                //isAlive=true;
            } catch (SocketTimeoutException e) {
                // System.out.println("I am here inside socket:::");
//                serverlist.remove(server);
                //isAlive=false;
                Log.e(TAG, "SocketTimeoutException");
//                e.printStackTrace();
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            }catch (EOFException e){
                Log.e(TAG, "EOF exception");
            }catch (StreamCorruptedException e){
                Log.e(TAG, "StreamCorruptedException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask IOException");
            }catch(NullPointerException e){
                Log.e(TAG, "Exception");
            }

            return null;
        }
    }

    /*
     * 1. This class plays a vital role. I have overridden the compareTo, comparing messages on the basis of their pID for their easy sort.
     */

    static class Message implements Comparable<Message>{
        // private final Object lock = new Object();

        String message;
        double pID;
        int id;
        String status;
        long millis;

        Message(String message, double pID, int id, String status, long millis){

            this.message = message;
            this.pID = pID;
            this.id = id;
            this.status = status;
            this.millis=millis;

        }
        t

        @Override
        public  int compareTo(Message another) {

            if (this.pID > another.pID)
                return 1;
            else if (this.pID < another.pID)
                return -1;
            else
                return 0;

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

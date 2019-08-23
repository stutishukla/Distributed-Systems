package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class SimpleDhtProvider extends ContentProvider {
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    ArrayList<HashValues> chord = new ArrayList<HashValues>();
    ArrayList<String> allPortInfo=new ArrayList<String>();
    Map<String,HashValues> map=new HashMap<String,HashValues>();
    static String currentPort=null;
    int aliveNodes = 0;
    static final int SERVER_PORT = 10000;
    static int insertFlag=0;

   /*Reference for deleteing a file:
     https://stackoverflow.com/questions/14737996/android-deleting-a-file-from-internal-storage*/
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File directory = getContext().getFilesDir();
        int count = 0;
        if ((allPortInfo.size() <= 1 && selection.equals("*")) || selection.equals("@")) {
            File[] contents = directory.listFiles();
            for (int i = 0; i < contents.length; i++) {
                System.out.println("getting filename for delete::" + contents[i].getName());
                File f = new File(directory, contents[i].getName());
                if (f.delete()) {
                    count++;
                }
            }
        } else if (allPortInfo.size() > 1 && selection.equals("*")) {
            String id6 = "SIXTH";
            try {
                for (int i = 0; i < allPortInfo.size(); i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(allPortInfo.get(i)) * 2);
                    String input;
                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    out.println(id6 + "#");
                    out.flush();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    input = br.readLine();
//                    count=Integer.parseInt(input);
                    if (input != null) {
                        socket.close();
                        count+=Integer.parseInt(input);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            File file = new File(directory, selection);
            if(file.delete())
                count++; }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        /* References for the code of insert::
         *  The following code of inserting into the internal storage has been taken from:
         *  1. https://developer.android.com/reference/java/io/FileInputStream
         *  2. PA1 template
         *  The code for getting the key and value pair has been taken from:
         *  OnPTestClickListener.java
         */
        System.out.println("Inside insert:::"+currentPort);
        boolean flag=false;
        String key = (String) values.get("key");
        System.out.println("Keyyy"+key);
        String val = (String) values.get("value");

        if(allPortInfo.size()<=1) {
            insertion(key,val);
        }
        else {
            try {
                String portToInsert="";
                String id3="THIRD";
                String pred="", predHash="";
                String currentHash=genHash(currentPort);
                String keyHash = genHash(key);
                int index = allPortInfo.indexOf(currentPort);
                String zerothHash=genHash(allPortInfo.get(0));
                String lastHash=genHash(allPortInfo.get(allPortInfo.size()-1));

                if ((keyHash.compareTo(zerothHash) <=0) || keyHash.compareTo(lastHash)>0)
                {
                    if(index==0) {
                        //insert into zeroth Port
                        insertion(key,val);
                        return uri;
                    }
                    else{
                        portToInsert=allPortInfo.get(0);
                    }
                    flag=true;


                }

                if(flag==false) {

                    for (int i = 1; i < allPortInfo.size(); i++) {
                        currentHash=genHash(allPortInfo.get(i));
                        pred=allPortInfo.get(i-1);
                        predHash = genHash(pred);
                        if ((keyHash.compareTo(currentHash) <=0) && keyHash.compareTo(predHash)>0) {
                            if (index == i) {
                                //insert into directly
                                insertion(key,val);
                                return uri;
                                //break;
                            } else {
                                portToInsert=allPortInfo.get(i);
                            }
                        }
                    }
                }
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(portToInsert)*2);
                String input;
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                out.println(id3+ "#" +key+ "#"+val);
                out.flush();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                input = br.readLine();
                if (input != null) {
                    socket.close();
                    return uri;
                }
            } catch (NoSuchAlgorithmException e) {
                // e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        currentPort=portStr;
        System.out.println("myport::" + myPort);
        final String portToSend = String.valueOf(Integer.parseInt(myPort) / 2);

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
        }
        Log.d(TAG, "Hellooo");
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPort);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         */

        /* References for the code of query::
         *  The following code of querying from the internal storage has been taken from:
         *  1. https://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
         *  2.https://developer.android.com/reference/java/io/File
         *  The code for reading into the cursor and adding a new row has been taken from:
         *  1. https://developer.android.com/reference/android/database/MatrixCursor.html
         */

        try {

            String[] columns = {"key", "value"};
            MatrixCursor matrixCursor = new MatrixCursor(columns);

            File directory = getContext().getFilesDir();
            boolean flag=false;
            if ((allPortInfo.size() <= 1 && selection.equals("*")) || selection.equals("@")) {
                File[] contents = directory.listFiles();
                for (int i = 0; i < contents.length; i++) {
                    File f = new File(directory, contents[i].getName());
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
                    String line = bufferedReader.readLine();
                    // System.out.println("getting filename" + last);
                    String[] row = {contents[i].getName(), line};
                    matrixCursor.addRow(row);
                }
            }
            else if(allPortInfo.size() > 1 && selection.equals("*")){
                String id5="FIFTH";
                System.out.println("In FIFTH in Query()");
                for(int i =0; i<allPortInfo.size(); i++){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(allPortInfo.get(i)) * 2);
                    String input;
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    out.println(id5 + "#");
                    System.out.println("Sending FIFTH for port: "+allPortInfo.get(i));
                    out.flush();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    input = br.readLine();
                    System.out.println("Received input from server:" +input);
                    if (input != null && !input.isEmpty()) {
                        socket.close();
                        String[] keyValue=input.split("#");
                        for(int j=0; j<keyValue.length; j++) {
                            String[] parts=keyValue[j].split("-");
                            String[] row = {parts[0], parts[1]};
                            // MatrixCursor matrixCursor=new MatrixCursor(columns);
                            Log.e(TAG, "After instantiating the cursor..");
                            matrixCursor.addRow(row);
                        }
                    }
                    else if (input.isEmpty()){
                        socket.close();
                    }

                }

            }
            else {

                if (allPortInfo.size() <= 1) {
                    String line = querying(selection);
                    String[] row = {selection, line};
                    Log.e(TAG, "After instantiating the cursor..");
                    matrixCursor.addRow(row);
                    return matrixCursor;
                }
                else{
                    String portToInsert = "";
                String id4 = "FOURTH";
                String pred = "", predHash = "", currentHash = "";
                String selectHash = genHash(selection);

                int index = allPortInfo.indexOf(currentPort);
                String zerothHash = genHash(allPortInfo.get(0));
                String lastHash = genHash(allPortInfo.get(allPortInfo.size() - 1));

                if ((selectHash.compareTo(zerothHash) <= 0) || selectHash.compareTo(lastHash) > 0) {
                    if (index == 0) {
                        String line = querying(selection);
                        String[] row = {selection, line};
                        Log.e(TAG, "After instantiating the cursor..");
                        matrixCursor.addRow(row);
                        return matrixCursor;
                    } else {
                        portToInsert = allPortInfo.get(0);
                    }
                    flag = true;
                }

                if (flag == false) {

                    for (int i = 1; i < allPortInfo.size(); i++) {
                        currentHash = genHash(allPortInfo.get(i));
                        pred = allPortInfo.get(i - 1);
                        predHash = genHash(pred);
                        if ((selectHash.compareTo(currentHash) <= 0) && selectHash.compareTo(predHash) > 0) {
                            if (index == i) {
                                String line = querying(selection);
                                String[] row = {selection, line};
                                Log.e(TAG, "After instantiating the cursor..");
                                matrixCursor.addRow(row);
                                return matrixCursor;
                            } else {
                                portToInsert = allPortInfo.get(i);
                            }
                        }
                    }
                }
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(portToInsert) * 2);
                String input;
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                out.println(id4 + "#" + selection);
                out.flush();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                input = br.readLine();
                if (input != null ) {
                    socket.close();
                    String[] row = {selection, input};
                    Log.e(TAG, "After instantiating the cursor..");
                    matrixCursor.addRow(row);
                }

            }
            }
            return matrixCursor;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    private void addToMap(ArrayList<HashValues> arr){
        for(int i=0;i<arr.size(); i++){
            map.put(arr.get(i).gethashNo(),arr.get(i));
            String key=arr.get(i).gethashNo();
            System.out.println("HAHAHA::"+key);
        }
    }

    private void insertion(String key, String val){
        FileOutputStream outputStream;
        Context context = getContext();
        try {
            outputStream = context.openFileOutput(key, context.MODE_PRIVATE);
            outputStream.write(val.getBytes());
            outputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private String querying(String selection){
        File directory = getContext().getFilesDir();
        File file = new File(directory, selection);
        FileInputStream f = null;
        String line="";
        try {
            f = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(f));
            line = bufferedReader.readLine();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    private String queryForStar() throws IOException {
        File directory = getContext().getFilesDir();
        File[] contents = directory.listFiles();
        String result="";
        for (int i = 0; i < contents.length; i++) {
            //System.out.println(":::" + contents[i].getAbsoluteFile());
            File f = new File(directory, contents[i].getName());
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(f));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String line = bufferedReader.readLine();
            result=result+contents[i].getName()+"-"+line+"#";
        }
        return result;
    }

    private int deleteForStar(){
        int count=0;
        File directory = getContext().getFilesDir();
        File[] contents = directory.listFiles();
        for (int i = 0; i < contents.length; i++) {
           // System.out.println("getting filename for delete::" + contents[i].getName());
            File f = new File(directory, contents[i].getName());
            if (f.delete()) {
                count++;
            }
        }
        return count;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {

                /*
                 * TODO: Fill in your server code that receives messages and passes them
                 * to onProgressUpdate().
                 * Code referred from :1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                 *                     2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                 */
                String input;
                String allPorts = "";
                String portNo,identifier;
                String ackName = "token";
                try {
                    Socket clientSocket = serverSocket.accept();
                   // System.out.println("Inside server task");
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    input = br.readLine();
                    Log.d(TAG, "input received in server is: "+input);
                    String[] parts=input.split("#");
                    identifier=parts[0];
                   // System.out.println("Inside server, identifier:"+identifier);

                    if(identifier.equals("FIRST")) {
                        portNo=parts[1];
                        aliveNodes++;
                       // System.out.println("ALIVE NODE COUNT:::" + aliveNodes);
                        String hash = genHash(portNo);
                       // System.out.println("And then here?" + hash);
                        // insertFlag=1;

                        chord.add(new HashValues(portNo, hash, null, null));
                        Collections.sort(chord);
                        if (aliveNodes == 1) {
                           // System.out.println("1 node only" + aliveNodes);
                           // System.out.println("::" + chord.get(0).getPred() + " " + chord.get(0).getSucc() + "///" + chord.get(0).getPort());
                            chord.get(0).setSucc(null);
                            chord.get(0).setPred(null);
                        } else if (aliveNodes == 2) {
                            chord.get(0).setSucc(chord.get(1).gethashNo());
                            chord.get(0).setPred(chord.get(1).gethashNo());
                            chord.get(1).setSucc(chord.get(0).gethashNo());
                            chord.get(1).setPred(chord.get(0).gethashNo());
                           // System.out.println("::" + chord.get(0).getPred() + " " + chord.get(0).getSucc() + "///" + chord.get(0).getPort());
                           // System.out.println("::" + chord.get(1).getPred() + " " + chord.get(1).getSucc() + "///" + chord.get(1).getPort());
                        } else {
                            for (int i = 0; i < chord.size(); i++) {
                                if (i == 0) {
                                    chord.get(i).setSucc(chord.get(i + 1).gethashNo());
                                    chord.get(i).setPred(chord.get(chord.size() - 1).gethashNo());
                                    // System.out.println("in here");
                                } else if (i == chord.size() - 1) {
                                    chord.get(i).setSucc(chord.get(0).gethashNo());
                                    chord.get(i).setPred(chord.get(i - 1).gethashNo());
                                   // System.out.println("::" + "LAST::" + chord.get(i).getPred() + " " + chord.get(i).getSucc() + "///" + chord.get(i).getPort());
                                } else {
                                    chord.get(i).setSucc(chord.get(i + 1).gethashNo());
                                    chord.get(i).setPred(chord.get(i - 1).gethashNo());
                                    //System.out.println("::" + "IN BETWEEN::" + chord.get(i).getPred() + " " + chord.get(i).getSucc() + "///" + chord.get(i).getPort());
                                }

                            }


                        }
                        //CODE 7Apr2019
                        for(int i=0;i<chord.size();i++){
                            allPorts=allPorts+chord.get(i).getPort()+"#";
                        }
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println(allPorts);
                        out.flush();
                    }
                    else if(identifier.equals("SECOND")){
                        allPortInfo.clear();
                        for(int i=1;i<parts.length;i++){
                            allPortInfo.add(parts[i]);
                        }
                        System.out.println("In second update::"+Arrays.toString(allPortInfo.toArray()));
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println("PA3");
                        out.flush();
                    }
                    else if(identifier.equals("THIRD")){
                        insertion(parts[1],parts[2]);
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println("insert");
                        out.flush();
                    }
                    else if(identifier.equals("FOURTH")){

                        String line=querying(parts[1]);
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println(line);
                        out.flush();
                    }
                    else if(identifier.equals("FIFTH")){
                        System.out.println("In FIFTH in serverTask");
                         String result=queryForStar();
                         System.out.println("FIFTH RESULT::::"+result);
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println(result);

                        out.flush();
                    }
                    else if(identifier.equals("SIXTH")){
                        int result=deleteForStar();
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println(result);
                        out.flush();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();
            //TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            // remoteTextView.append(strReceived + "\t\n");
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


            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                /*if (msgs[1].equals(REMOTE_PORT0))
                    remotePort = REMOTE_PORT1;*/
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT0));

                String msgToSend = String.valueOf(Integer.parseInt(msgs[0]) / 2);
                System.out.println("Am I here??" + msgToSend);
                String input,input2;
                String id1="FIRST";
                String id2="SECOND";

                /*
                 * TODO: Fill in your client code that sends out a message.
                 * Code for PrintWriter out and acknowledgement:
                 * 1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                 * 2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                 */
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                out.println(id1+ "#" + msgToSend);
                out.flush();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                input = br.readLine();
                System.out.println(input+"input at Clientask::");
                if (input != null) {
                    socket.close();
                    String[] portInfo=input.split("#");
                    for(int i =0; i<portInfo.length; i++) {
                        Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(portInfo[i])*2);
                        String portToSend = input;
                        System.out.println("Inside socket2" + portToSend);
                        PrintWriter out1 = new PrintWriter(socket1.getOutputStream());

                        out1.println(id2+ "#" +portToSend);
                        out1.flush();
                        BufferedReader br1 = new BufferedReader(
                                new InputStreamReader(socket1.getInputStream()));
                        input2 = br1.readLine();
                        if (input2 != null) {
                            socket1.close();
                        }

                    }
                }



            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }


    static class HashValues implements Comparable<HashValues> {
        String portNo;
        String hashNo;
        String pred;
        String succ;

        HashValues(String portNo, String hashNo, String pred, String succ) {
            this.portNo = portNo;
            this.hashNo = hashNo;
            this.pred = pred;
            this.succ = succ;

        }

        @Override
        public int compareTo(HashValues o) {
            // TODO Auto-generated method stub
            if (this.hashNo.compareTo(o.hashNo) > 0) {
                return 1;
            } else if (this.hashNo.compareTo(o.hashNo) < 0) {
                return -1;
            } else {
                return 0;
            }
        }

        void setSucc(String succ) {

            this.succ = succ;

        }

        void setPred(String pred) {

            this.pred = pred;

        }

        String getSucc() {
            return this.succ;
        }

        String gethashNo() {
            return this.hashNo;
        }

        String getPred() {
            return this.pred;
        }
        String getPort() {
            return this.portNo;
        }
    }
}

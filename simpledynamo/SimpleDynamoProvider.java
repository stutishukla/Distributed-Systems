package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.FileNameMap;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
//9-05-2018
import static android.content.ContentValues.TAG;


public class SimpleDynamoProvider extends ContentProvider {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    boolean halt=false;
    //ArrayList<HashValues> chord = new ArrayList<HashValues>();
    ArrayList<String> allPortInfo=new ArrayList<String>(Arrays.asList("5562", "5556", "5554", "5558", "5560"));

    Map<String,String> tmap=new HashMap<String, String>();
    static String currentPort=null;
    int aliveNodes = 0;
    static final int SERVER_PORT = 10000;
    static int insertFlag=0;

    /*Reference for deleteing a file:
      https://stackoverflow.com/questions/14737996/android-deleting-a-file-from-internal-storage

      In delete, I am handling all the cases, *, @, and for single key deletion, replication is being taken care of.*/
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File directory = getContext().getFilesDir();
        int count = 0;
        if ((allPortInfo.size() <= 1 && selection.equals("*")) || selection.equals("@")) {
            File[] contents = directory.listFiles();
            for (int i = 0; i < contents.length; i++) {
                if(!contents[i].getName().equalsIgnoreCase("Stuti")) {
                    System.out.println("getting filename for delete::" + contents[i].getName());
                    File f = new File(directory, contents[i].getName());
                    if (f.delete()) {
                        count++;
                    }
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
            String[] replicas=null;
            File file = new File(directory, selection);
            if(file.delete())
                count++;
            if(currentPort.equals("5556")){
                replicas= new String[]{"5554", "5558"};
            }
            else if(currentPort.equals("5554")){
                replicas=new String[]{"5558", "5560"};
            }
            else if(currentPort.equals("5558")){
                replicas=new String[]{"5560", "5562"};
            }
            else if(currentPort.equals("5560")){
                replicas=new String[]{"5562","5556"};
            }
            else if(currentPort.equals("5562")){
                replicas=new String[]{"5556","5554"};
            }
            for (int i = 0; i < replicas.length; i++) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(replicas[i]) * 2);
                    socket.setSoTimeout(500);
                    String input;
                    String id2="SECOND";
                    String msgToSend=id2+ "#" +selection;
                    System.out.println("Inside delete::"+msgToSend);
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    out.println(msgToSend);
                    out.flush();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    input = br.readLine();
                    if (input != null) {
                        socket.close();
                    }
                }catch(SocketTimeoutException e){
                    System.out.println("Inside SocketTimeOutException for delete");
                }catch(NullPointerException e){
                    System.out.println("Inside NullPointer for delete");
                }catch (IOException e) {
                    System.out.println("Inside IOException for delete");
                }
            }
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    /*In insert, I am handling all the cases, *, @, and for single key deletion, replication is being taken care of.*/
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
        while(halt){
            //busy wait.
            Log.d(TAG, "Busy Waiting in Insert()");
        }
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
                String[] portToInsert=new String[3];
                String id3="THIRD";
                String pred="", predHash="";
                String currentHash=genHash(currentPort);
                String keyHash = genHash(key);
                int index = allPortInfo.indexOf(currentPort);
                String zerothHash=genHash(allPortInfo.get(0));
                String lastHash=genHash(allPortInfo.get(allPortInfo.size()-1));
                if ((keyHash.compareTo(zerothHash) <=0) || keyHash.compareTo(lastHash)>0)
                {
                    portToInsert[0]=allPortInfo.get(0);
                    portToInsert[1]=allPortInfo.get(1);
                    portToInsert[2]=allPortInfo.get(2);
                    flag=true;
                }

                if(flag==false) {

                    for (int i = 1; i < allPortInfo.size(); i++) {
                        currentHash=genHash(allPortInfo.get(i));
                        pred=allPortInfo.get(i-1);
                        predHash = genHash(pred);
                        if ((keyHash.compareTo(currentHash) <=0) && keyHash.compareTo(predHash)>0) {
                            if (i == 1 || i == 2) {
                                //insert into directly
                                portToInsert[0]=allPortInfo.get(i);
                                portToInsert[1]=allPortInfo.get(i+1);
                                portToInsert[2]=allPortInfo.get(i+2);
                            }
                            else if(i==3){
                                portToInsert[0]=allPortInfo.get(i);
                                portToInsert[1]=allPortInfo.get(i+1);
                                portToInsert[2]=allPortInfo.get(0);
                            }
                            else if(i==4){
                                portToInsert[0]=allPortInfo.get(i);
                                portToInsert[1]=allPortInfo.get(0);
                                portToInsert[2]=allPortInfo.get(1);
                            }
                            break;
                        }
                    }
                }
                for(int i=0; i<portToInsert.length; i++) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(portToInsert[i]) * 2);
                        socket.setSoTimeout(500);
                        String input;
                        PrintWriter out = new PrintWriter(socket.getOutputStream());

                        out.println(id3 + "#" + key + "#" + val + "#" + portToInsert[0]);
                        out.flush();
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        input = br.readLine();
                        if (input != null) {
                            socket.close();
                        }
                    }catch(SocketTimeoutException e){
                        System.out.println("Inside Socket Exception!!");
                    }catch(UnknownHostException e){
                        System.out.println("Inside UnknownHostException!!");
                    } catch(StreamCorruptedException e){
                        System.out.println("Inside StreamCorruptedException!!");
                    }catch(EOFException e){
                        System.out.println("Inside EOFException!!");
                    }
                    catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("Inside IOException!!");
                    }
                }return uri;
            } catch (NoSuchAlgorithmException e) {
                // e.printStackTrace();
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
             *
             * References: https://stackoverflow.com/questions/16237950/android-check-if-file-exists-without-creating-a-new-one
             *
             * In OnCreate(), I am taking care of crash recovery by checking the status of avd using a dummy file "Stuti".
             */
            File directory = getContext().getFilesDir();
            String temp="Stuti";
            boolean check = new File(directory, temp).exists();
            if(!check){
                System.out.println("First Time Alive!!");
                String val="first";
                FileOutputStream outputStream;
                outputStream = getContext().openFileOutput("Stuti", getContext().MODE_PRIVATE);
                outputStream.write(val.getBytes());
                outputStream.close();
            }
            else{
                File[] contents = directory.listFiles();
                System.out.println("I crashed!!"+currentPort);
                for (int i = 0; i < contents.length; i++) {
                    if(!contents[i].getName().equalsIgnoreCase("Stuti")) {
                        File f = new File(directory, contents[i].getName());
                        f.delete();
                    }
                }
                System.out.println("is port mein hai::"+currentPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, myPort);
            }
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
        while(halt){
            //busy wait.
            Log.d(TAG, "Busy Waiting in query()");
        }
        try {
            String max = "0", result = "";

            String[] columns = {"key", "value"};
            MatrixCursor matrixCursor = new MatrixCursor(columns);

            File directory = getContext().getFilesDir();
            boolean flag = false;
            if ((allPortInfo.size() <= 1 && selection.equals("*")) || selection.equals("@")) {
                File[] contents = directory.listFiles();
                for (int i = 0; i < contents.length; i++) {
                    try {
                        if (!contents[i].getName().equalsIgnoreCase("Stuti")) {
                            File f = new File(directory, contents[i].getName());
                            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
                            String line = bufferedReader.readLine();
                            String[] parts = line.split("#");
                            String[] row = {contents[i].getName(), parts[0]};
                            matrixCursor.addRow(row);
                        }
                    } catch (UnknownHostException e) {
                        System.out.println("Inside UnknownHostException!!");
                    } catch (StreamCorruptedException e) {
                        System.out.println("Inside StreamCorruptedException!!");
                    } catch (FileNotFoundException e) {
                        System.out.println("Inside FileNotFound!!");
                    } catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("Inside IOException!!");
                    }
                }
            } else if (allPortInfo.size() > 1 && selection.equals("*")) {
                String id5 = "FIFTH";
                System.out.println("In FIFTH in Query()");
                for (int i = 0; i < allPortInfo.size(); i++) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(allPortInfo.get(i)) * 2);
                        socket.setSoTimeout(500);

                        String input;
                        PrintWriter out = new PrintWriter(socket.getOutputStream());

                        out.println(id5 + "#");
                        System.out.println("Sending FIFTH for port: " + allPortInfo.get(i));
                        out.flush();
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        input = br.readLine();
                        System.out.println("Received input from server:" + input);
                        if (input != null && !input.isEmpty()) {
                            socket.close();
                            String[] keyValue = input.split("#");
                            for (int j = 0; j < keyValue.length; j++) {
                                String[] parts = keyValue[j].split("-");
                                String[] row = {parts[0], parts[1]};
                                // MatrixCursor matrixCursor=new MatrixCursor(columns);
                                Log.e(TAG, "After instantiating the cursor..");
                                matrixCursor.addRow(row);
                            }
                        } else if (input == null || input.isEmpty()) {
                            socket.close();
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Inside Socket Exception!!");
                    } catch (UnknownHostException e) {
                        System.out.println("Inside UnknownHostException!!");
                    } catch (StreamCorruptedException e) {
                        System.out.println("Inside StreamCorruptedException!!");
                    } catch (EOFException e) {
                        System.out.println("Inside EOFException!!");
                    } catch (NullPointerException e) {
                        System.out.println("Inside NullPointer!!");

                    } catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("Inside IOException!!");
                    }

                }

            } else {

                if (allPortInfo.size() <= 1) {
                    String line = querying(selection);
                    String[] row = {selection, line};
                    // MatrixCursor matrixCursor=new MatrixCursor(columns);
                    Log.e(TAG, "After instantiating the cursor..");
                    matrixCursor.addRow(row);
                    return matrixCursor;
                } else {
                    String portToInsert[] = new String[3];
                    String id4 = "FOURTH";
                    String pred = "", predHash = "", currentHash = "";
                    String selectHash = genHash(selection);

                    int index = allPortInfo.indexOf(currentPort);
                    String zerothHash = genHash(allPortInfo.get(0));
                    String lastHash = genHash(allPortInfo.get(allPortInfo.size() - 1));

                    if ((selectHash.compareTo(zerothHash) <= 0) || selectHash.compareTo(lastHash) > 0) {

                        portToInsert[0] = allPortInfo.get(0);
                        portToInsert[1] = allPortInfo.get(1);
                        portToInsert[2] = allPortInfo.get(2);
                        flag = true;
                    }

                    if (flag == false) {

                        for (int i = 1; i < allPortInfo.size(); i++) {
                            currentHash = genHash(allPortInfo.get(i));
                            pred = allPortInfo.get(i - 1);
                            predHash = genHash(pred);
                            if ((selectHash.compareTo(currentHash) <= 0) && selectHash.compareTo(predHash) > 0) {
                                if (i == 1 || i == 2) {
                                    //insert into directly
                                    portToInsert[0] = allPortInfo.get(i);
                                    portToInsert[1] = allPortInfo.get(i + 1);
                                    portToInsert[2] = allPortInfo.get(i + 2);
                                } else if (i == 3) {
                                    portToInsert[0] = allPortInfo.get(i);
                                    portToInsert[1] = allPortInfo.get(i + 1);
                                    portToInsert[2] = allPortInfo.get(0);
                                } else if (i == 4) {
                                    portToInsert[0] = allPortInfo.get(i);
                                    portToInsert[1] = allPortInfo.get(0);
                                    portToInsert[2] = allPortInfo.get(1);
                                }
                                break;
                            }
                        }
                    }
                    for (int i = 0; i < portToInsert.length; i++) {
                        try {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(portToInsert[i]) * 2);
                            socket.setSoTimeout(500);
                            String input;
                            PrintWriter out = new PrintWriter(socket.getOutputStream());

                            out.println(id4 + "#" + selection);
                            out.flush();
                            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));
                            input = br.readLine();
                            if (!input.equals("null") && input != null && !input.isEmpty()) {
                                socket.close();
                                System.out.println("double double::" + input);
                                String[] parts = input.split("#");
                                if (Integer.parseInt(parts[1]) > Integer.parseInt(max))
                                    max = parts[1];
                                tmap.put(parts[1], parts[0]);
                                
                            }
                        } catch (SocketTimeoutException e) {
                            System.out.println("Inside Socket Exception!!");
                        } catch (NullPointerException e) {
                            System.out.println("Inside NullPointerException!!");
                        } catch (UnknownHostException e) {
                            System.out.println("Inside UnknownHostException!!");
                        } catch (StreamCorruptedException e) {
                            System.out.println("Inside StreamCorruptedException!!");
                        } catch (EOFException e) {
                            System.out.println("Inside EOFException!!");
                        } catch (IOException e) {
                            // e.printStackTrace();
                            System.out.println("Inside IOException!!");
                        }
                    }
                    result = tmap.get(max);
                    System.out.println("Royally fuc**::" + result);
                    String[] row = {selection, result};
                    matrixCursor.addRow(row);
                }
            }
            return matrixCursor;
        }
        catch (NoSuchAlgorithmException e) {
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

    private void insertion(String key, String val){
        FileOutputStream outputStream;
        Context context = getContext();
        // File file = new File(context.getFilesDir(), key);
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
        String[] parts=null;
        String line="";
        try {
            f = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(f));
            line = bufferedReader.readLine();
            parts=line.split("#");
            return parts[0]+"#"+parts[2];
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String queryForStar() throws IOException {

        File directory = getContext().getFilesDir();
        File[] contents = directory.listFiles();
        String result="";
        for (int i = 0; i < contents.length; i++) {
            if(!contents[i].getName().equalsIgnoreCase("Stuti")) {
                File f = new File(directory, contents[i].getName());
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new FileReader(f));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String line = bufferedReader.readLine();
                String[] parts=line.split("#");
                result = result + contents[i].getName() + "-" + parts[0] + "#";
            }
        }
        return result;
    }

    private int deleteForStar(){
        int count=0;
        File directory = getContext().getFilesDir();
        File[] contents = directory.listFiles();
        for (int i = 0; i < contents.length; i++) {
            System.out.println("getting filename for delete::" + contents[i].getName());
            if(!contents[i].getName().equalsIgnoreCase("Stuti")) {
                File f = new File(directory, contents[i].getName());
                if (f.delete()) {
                    count++;
                }
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
                String valToInsert = "";
                String portNo,identifier;
                String ackName = "token";
                int versionNo;
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(500);
                    System.out.println("Inside server task");
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    input = br.readLine();
                    Log.d(TAG, "input received in server is: "+input);
                    String[] parts=input.split("#");
                    identifier=parts[0];
                    System.out.println("Inside server, identifier:"+identifier);

                    while (halt){
                        // busy waiting
                        // check if this is necessary
                        Log.d(TAG, "Busy Waiting in ServerTask() of port: "+currentPort);
                    }

                    if(identifier.equals("FIRST")){
                        File directory = getContext().getFilesDir();
                        File[] contents = directory.listFiles();
                        String result="";
                        for (int i = 0; i < contents.length; i++) {
                            if(!contents[i].getName().equalsIgnoreCase("Stuti")) {
                                File f = new File(directory, contents[i].getName());
                                BufferedReader bufferedReader = null;
                                try {
                                    bufferedReader = new BufferedReader(new FileReader(f));
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                String line = bufferedReader.readLine();
                                String[] check=line.split("#");
                                System.out.println("Hi::"+check[0]+"Ahaa::"+check[2]+"for server::"+check[1]);
                                if(check[1].equalsIgnoreCase(parts[1])){
                                    System.out.println("check1::"+check[1]+"parts1::"+parts[1]);
                                    result = result + contents[i].getName() + "-" + check[0] + "-"+ check[1]+"-"+ check[2]+"#";
                                }

                            }
                        }
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());
                        System.out.println("RESULT:::"+result);
                        out.println(result);
                        out.flush();
                    }
                    else if(identifier.equals("SECOND")){
                        int count=0;
                        System.out.println("key::"+parts[1]);

                        File directory = getContext().getFilesDir();
                        File file = new File(directory, parts[1]);
                        file.delete();
                        count++;
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());
                        out.println(count);
                        out.flush();
                    }

                    else if(identifier.equals("THIRD")){
                        while(halt){
                            //do nothing
                            Log.d(TAG, "Busy Waiting in THIRD");
                        }
                        File directory = getContext().getFilesDir();
                        boolean check = new File(directory, parts[1]).exists();
                        if(!check) {
                            valToInsert = parts[2] + "#" + parts[3] + "#" + 1;
                            System.out.println("first time insert:::" + valToInsert);
                        }
                        else{
                            File f = new File(directory, parts[1]);
                            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
                            String line = bufferedReader.readLine();
                            System.out.println("cause of failure1:::" + line);
                            String[] another=line.split("#");
                            versionNo=Integer.parseInt(another[2])+1;
                            valToInsert=parts[2]+"#"+parts[3]+"#"+versionNo;
                            System.out.println("DONGDONG:::" + valToInsert);

                        }

                        insertion(parts[1],valToInsert);
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println("insert");
                        out.flush();
                    }
                    else if(identifier.equals("FOURTH")){
                        while(halt){
                            //busy wait
                            Log.d(TAG, "Busy Waiting in FOURTH");
                        }
                        String line=querying(parts[1]);
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream());

                        out.println(line);
                        out.flush();
                    }
                    else if(identifier.equals("FIFTH")){
                        System.out.println("In FIFTH in serverTask");
                        while(halt){
                            //busy wait
                            Log.d(TAG, "Busy Waiting in FIFTH");
                        }
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

                }catch(SocketTimeoutException e){
                    System.out.println("Inside Server SocketException!!");
                }
                catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");
                }
                // return null;
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
    /*ClientTask is taking care of crash recovery. It takes data from its forward two avds and backward two avds on recovery. */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            //  try {
                    /*if (msgs[1].equals(REMOTE_PORT0))
                        remotePort = REMOTE_PORT1;*/
            halt=true;
            int counter = 1;
            String[] forward = null;
            String[] backward = null;
            switch (currentPort) {
                case "5562":
                    System.out.println("In herere::"+currentPort);
                    forward = new String[]{"5556", "5554"};
                    backward = new String[]{"5558", "5560"};
                    break;

                case "5556":
                    System.out.println("In herere2::"+currentPort);
                    forward = new String[]{"5554", "5558"};
                    backward = new String[]{"5560", "5562"};
                    break;

                case "5554":
                    System.out.println("In herere3::"+currentPort);
                    forward = new String[]{"5558", "5560"};
                    backward = new String[]{"5562", "5556"};
                    break;

                case "5558":
                    System.out.println("In herere4::"+currentPort);
                    forward = new String[]{"5560", "5562"};
                    backward = new String[]{"5556", "5554"};
                    break;

                case "5560":
                    System.out.println("In herere5::"+currentPort);
                    forward = new String[]{"5562", "5556"};
                    backward = new String[]{"5554", "5558"};
                    break;
            }
            for (int i = 0; i < forward.length; i++) {
                try{
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(forward[i]) * 2);
                    String msgToSend = currentPort;
                    System.out.println("Am I here??" + msgToSend);
                    String input;
                    String id1 = "FIRST";

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     * Code for PrintWriter out and acknowledgement:
                     * 1.https://docs.oracle.com/javase/tutorial/networking/socketsclientServer.html
                     * 2.https://www.youtube.com/watch?v=MshSvgwBmU4&t=1s
                     */
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    input = br.readLine();
                    if (!input.equals("null") && input != null && !input.isEmpty()) {
                        socket.close();
                        Log.d(TAG, "Printing input val: " + forward[i] + "::" + input);
                        String[] keyValue = input.split("#");

                        if (counter == 1) {
                            for (int j = 0; j < keyValue.length; j++) {
                                String[] parts = keyValue[j].split("-");
                                String valToInsert = parts[1] + "#" + parts[2] + "#" + parts[3];
                                insertion(parts[0], valToInsert);
                            }
                            counter++;
                            System.out.println("COUNTER VALUE ON EXIT::" + counter);
                        } else {
                            for (int j = 0; j < keyValue.length; j++) {
                                String valToInsert = "";
                                String[] parts = keyValue[j].split("-");
                                File directory = getContext().getFilesDir();
                                boolean check = new File(directory, parts[0]).exists();
                                if (check) {
                                    File f = new File(directory, parts[0]);
                                    BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
                                    String line = bufferedReader.readLine();
                                    String[] another = line.split("#");
                                    if (Integer.parseInt(parts[3]) > Integer.parseInt(another[2])) {
                                        valToInsert = parts[1] + "#" + parts[2] + "#" + parts[3];
                                        insertion(parts[0], valToInsert);
                                    }
                                } else {
                                    valToInsert = parts[1] + "#" + parts[2] + "#" + parts[3];
                                    insertion(parts[0], valToInsert);
                                }
                            }

                        }

                    }
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                }catch(NullPointerException e){
                    e.printStackTrace();
                }catch (FileNotFoundException e) {
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace(); }
            }
            for (int j = 0; j < backward.length; j++) {
                try{
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(backward[j]) * 2);
                    String msgToSend = currentPort;
                    String id1 = "FIRST";
                    String input;

                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    out.println(id1 + "#" + backward[j]);
                    out.flush();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    input = br.readLine();
                    String valToInsert = "";
                    if (!input.equals("null") && input != null && !input.isEmpty()) {
                        socket.close();
                        String[] keyValue = input.split("#");
                        for (int i = 0; i < keyValue.length; i++) {
                            String[] parts = keyValue[i].split("-");
                            valToInsert = parts[1] + "#" + parts[2] + "#" + parts[3];
                            insertion(parts[0], valToInsert);
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }catch (NullPointerException e) {
                    e.printStackTrace();
                }catch (FileNotFoundException e) {
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace(); }
            }
            halt=false;
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
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

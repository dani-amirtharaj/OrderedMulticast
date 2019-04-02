package edu.buffalo.cse.cse486586.groupmessenger2;

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
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the launcher Activity for the project assignment.
 *
 * @author Daniel Amirtharaj
 */
public class GroupMessengerActivity extends Activity {

    private static final int SERVER_PORT = 10000;
    private List<Socket> socketsAll; //Socket list for storing active sockets.
    private String myPort;
    Comparator queueComparator = new QueueComparator();
    private final PriorityQueue<Message> queue = new PriorityQueue<Message>(10, queueComparator);
    private static final ReentrantLock lock = new ReentrantLock();
    private int proposedCnt = 0;
    private int agreedCnt = 0;

    /*
     * Comparator for specifying the order of priority used in the PriorityQueue.
     * Agreed count for a message is set to -1 by default. If agreed count exists,
     * it will be used to denote the ordering of the PriorityQueue, else, the
     * proposed count will be used.
     *
     * https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html
     */
    private class QueueComparator implements Comparator<Message> {
        @Override
        public int compare(Message lhs, Message rhs) {
            int lCount, rCount;
            lCount = lhs.getAgreedCnt() < 0 ? lhs.getProposedCnt() : lhs.getAgreedCnt();
            rCount = rhs.getAgreedCnt() < 0 ? rhs.getProposedCnt() : rhs.getAgreedCnt();
            if (lCount < rCount) {
                return -1;
            } else if (lCount > rCount) {
                return 1;
            }
            return 0;
        }
    }

    /*
     * Message Object used to store the Message string received
     * along with other MetaData such as proposed count, agreed
     * count, and the sender of the message.
     */
    private class Message {
        private final String message;
        private int proposedCnt;
        private int agreedCnt;

        private Message(String message) {
            this.message = message;
            this.proposedCnt = 0;
            this.agreedCnt = -1;
        }

        public void setAgreedCnt(int agreedCnt) {
            this.agreedCnt = agreedCnt;
        }

        public void setProposedCnt(int proposedCnt) {
            this.proposedCnt = proposedCnt;
        }

        public int getAgreedCnt() {
            return agreedCnt;
        }

        public int getProposedCnt() {
            return proposedCnt;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * Gets port number that AVD uses to connect to the host machine.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr)) * 2);

        /*
         * Creates a server socket and creates an AsyncTask to run the server thread.
         */
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (
                IOException e) {
            Log.e(TAG, e.toString());
            return;
        }

        EditText editText = (EditText) findViewById(R.id.editText1);

        /*
         * Registers OnSendListener for "button4" in the layout, which is the "Send" button.
         * OnSendListener is called everytime the button is clicked, to multicast messages to
         * all other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(
                new OnSendListener(editText, socketsAll));
    }

    /*
     * ServerTask for the server thread, listens and accepts any connection request.
     * Messages from other AVDs are received here.
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private static final String KEY_FIELD = "key";
        private static final String VALUE_FIELD = "value";
        private int keycounter = 0;
        private final ContentResolver mContentResolver = getContentResolver();
        private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        private final ContentValues mContentValues = new ContentValues();

        /* Referred the OnPTestClickListener Class given in the template*/
        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... servSocket) {
            ServerSocket serverSocket = servSocket[0];

            /*
             * 1. Referred Java tutorials for server side socket programming
             * to use try with resources to close sockets, readers and writers automatically,
             * to accept a clients request for communication with the accept() method,
             * to get input and output socket streams and create a reader and writer on them
             * to read from socket (readLine) and write to socket (println)
             * URL : https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
             *
             * 2. Referred Android AsyncTask class API reference for publishing progress to onProgressUpdate at
             * URL : https://developer.android.com/reference/android/os/AsyncTask#publishProgress(Progress...)
             *
             * 3. Referred Java tutorial on Runnable and Threads for creating multiple threads to handle
             * multiple clients at
             * https://docs.oracle.com/javase/tutorial/networking/sockets/examples/KKMultiServer.java
             * https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/networking/sockets/
             * examples/KKMultiServerThread.java
             * https://docs.oracle.com/javase/tutorial/essential/concurrency/runthread.html
             * https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/concurrency/
             * examples/HelloRunnable.java
             *
             *
             * 4. Referred Section 15.4.3 Ordered multicast for implementing the ISIS algorithm.
             *
             * 5. Referred Java docs for lock usage at
             * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/ReentrantLock.html
             */
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> {
                        Message message = null;
                        try (
                                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
                        ) {
                            String received;
                            while ((received = in.readLine()) != null) {
                                message = new Message(received);

                                /* Calculate and send proposed count to sender. */
                                lock.lock();
                                try {
                                    proposedCnt = Math.max(proposedCnt, agreedCnt) + 1;
                                    out.println(proposedCnt * 100000 + Integer.parseInt(myPort));
                                    message.setProposedCnt(proposedCnt);
                                    queue.add(message);
                                } finally {
                                    lock.unlock();
                                }

                                String agreement = in.readLine();

                                /* Handling failure of other AVD. */
                                if (agreement == null || !clientSocket.isConnected()) {

                                    lock.lock();
                                    try {
                                        queue.remove(message);
                                        if (!queue.isEmpty()) {
                                            persistMessage(message);
                                        }
                                    } finally {
                                        lock.unlock();
                                    }

                                    break;
                                }

                                /* Process received agreed count and persist message to DB. */
                                int agreedInt = Integer.parseInt(agreement.substring(0, agreement.length() - 5));
                                lock.lock();
                                try {
                                    message.setAgreedCnt(Integer.parseInt(agreement));
                                    if (queue.remove(message)) {
                                        if (!queue.add(message)) {
                                        }
                                    }
                                    if (agreedCnt < agreedInt) {
                                        agreedCnt = agreedInt;
                                    }
                                    persistMessage(message);
                                } finally {
                                    lock.unlock();
                                }

                            }
                            clientSocket.close();
                        } catch (IOException e) {

                            lock.lock();
                            try {
                                queue.remove(message);
                                if (!queue.isEmpty()) {
                                    persistMessage(message);
                                }
                            } finally {
                                lock.unlock();
                            }

                        }
                    }).start();

                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        private void persistMessage(Message message) {
            while (queue.peek().getAgreedCnt() > 0) {
                message = queue.poll();
                publishProgress(message.getMessage());
                if (queue.isEmpty()) {
                    break;
                }
            }
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The key and value to be persisted are set to the content value object.
             * The content value and the uri are used to pass data to the content provider through the content resolver.
             * Referred the following documents,
             * https://developer.android.com/guide/topics/providers/content-provider-basics
             * https://developer.android.com/reference/android/content/ContentResolver
             *
             */
            String strReceived = strings[0].trim();
            mContentValues.put(VALUE_FIELD, strReceived);
            mContentValues.put(KEY_FIELD, Integer.toString(keycounter++));
            mContentResolver.insert(mUri, mContentValues);
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}

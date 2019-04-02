package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;
import android.view.View;
import android.os.AsyncTask;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class OnSendListener implements View.OnClickListener {

    private final EditText editText;
    private List<Socket> sockets;
    private static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};

    public OnSendListener(EditText _et, List<Socket> _sockets) {
        editText = _et;
        sockets = _sockets;
    }

    /*
     * Callback method to respond to a button click. Gets message string from editText,
     * clears the view and creates a new thread to send messages.
     */
    @Override
    public void onClick(View view) {
        String message = editText.getText().toString();
        editText.setText("");
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            /*
             * 1. Referred Java tutorials for client side socket programming
             * to use try with resources to close sockets, readers and writers automatically,
             * to get input and output socket streams and create a reader and writer on them
             * to read from socket (readLine) and write to socket (println)
             * URL : https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
             *
             * 2. Referred to Java docs for setting socket timeouts both at create and later at
             * https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
             * https://docs.oracle.com/javase/7/docs/api/java/net/InetSocketAddress.html
             */

            /* Setup connection with other AVDS for the first time. */
            if (sockets == null) {
                List<Socket> socketList = new ArrayList<>();
                for (String string : REMOTE_PORTS) {
                    Socket socket = null;
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(string)), 5000);
                        socket.setSoTimeout(0);
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    socketList.add(socket);
                }
                sockets = socketList;
            }

            /* Multicast messages to other AVDs */
            int num = 0;
            List<Socket> remList = new ArrayList<>();
            for (Socket socket : sockets) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msgToSend = msgs[0];
                    out.println(msgToSend);
                    String isisMsg = in.readLine();
                    if (isisMsg == null) {
                        remList.add(socket);
                        continue;
                    }
                    num = Math.max(Integer.parseInt(isisMsg), num);
                } catch (IOException e) {
                    remList.add(socket);
                    Log.e(TAG, e.toString());
                }
            }

            sockets.removeAll(remList);
            remList.clear();

            /* Multicast Accepted proposal counts to AVDs*/
            for (Socket socket : sockets) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(num);
                } catch (IOException e) {
                    remList.add(socket);
                    Log.e(TAG, e.toString());
                }
            }
            sockets.removeAll(remList);
            remList.clear();
            return null;
        }
    }
}

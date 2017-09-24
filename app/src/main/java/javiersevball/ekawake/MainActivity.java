/*
    Copyright 2017 Javier Sevilla

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package javiersevball.ekawake;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity
{
    private static final String LAST_MAC_ADDRESS_KEY = "javiersevball.ekawake.LAST_MAC_ADDRESS";

    // AsyncTask worker for sending the requested magic packets to the destination host
    private class SendMagicPacketsTask extends AsyncTask<SendMagicPacketsTaskParams, Void, Boolean> {
        // Method invoked on the UI thread before the task is executed
        protected void onPreExecute()
        {
            updateUiElements(true);
        }

        // Invoked on the background thread immediately after onPreExecute() finishes executing
        protected Boolean doInBackground(SendMagicPacketsTaskParams... params)
        {
            boolean allPacketsSentOk;
            Boolean result;

            allPacketsSentOk =
                    sendMagicPackets(params[0].getDestinationMacAddress(), params[0].getNumberOfPacketsToSend());
            result = (allPacketsSentOk) ? Boolean.TRUE : Boolean.FALSE;

            return result;
        }

        // Method executed on the UI thread after the background computation finishes
        protected void onPostExecute(Boolean result)
        {
            updateUiElements(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart()
    {
        SharedPreferences sharedPref;
        EditText eTextMacAddress;
        String lastMacAddressValue;

        super.onStart();

        // Read the last destination MAC address introduced by the user
        eTextMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        lastMacAddressValue = sharedPref.getString(LAST_MAC_ADDRESS_KEY, null);

        if (lastMacAddressValue != null)
        {
            eTextMacAddress.setText(lastMacAddressValue);
        }
    }

    // SEND button clicked
    public void sendButtonClicked(View view)
    {
        int numberOfPackets;
        String destMacAddress;
        SendMagicPacketsTaskParams sendTaskParams;
        SharedPreferences sharedPref;
        SharedPreferences.Editor sharedPrefEditor;

        Log.i("MainActivity", "SEND button pressed.");

        destMacAddress = readMacAddress();
        numberOfPackets = readNumberOfPackets();

        // If the destination MAC address format is OK, save it
        if (destMacAddress != null)
        {
            sharedPref = getPreferences(Context.MODE_PRIVATE);
            sharedPrefEditor = sharedPref.edit();
            sharedPrefEditor.putString(LAST_MAC_ADDRESS_KEY, destMacAddress);
            sharedPrefEditor.commit();
        }

        // Start sending the magic packets
        if (destMacAddress != null && numberOfPackets > 0)
        {
            sendTaskParams = new SendMagicPacketsTaskParams(destMacAddress, numberOfPackets);
            new SendMagicPacketsTask().execute(sendTaskParams);
        }
    }

    // Sets the visibility and the enable status of the UI elements according to the "sending" status.
    private void updateUiElements(boolean sendingStatus)
    {
        EditText macAddressEditText, numOfPacketsEditText;
        Button sendButton;
        ProgressBar sendingPacketsProgressBar;
        TextView sendingPacketsTextView;

        macAddressEditText = (EditText)findViewById(R.id.editTextMacAddress);
        numOfPacketsEditText = (EditText)findViewById(R.id.editTextNumberOfPackets);
        sendButton = (Button)findViewById(R.id.sendPacketsButton);
        sendingPacketsProgressBar = (ProgressBar)findViewById(R.id.sendingProgressBar);
        sendingPacketsTextView = (TextView)findViewById(R.id.sendingTextView);

        if (sendingStatus)
        {
            macAddressEditText.setEnabled(false);
            numOfPacketsEditText.setEnabled(false);
            sendButton.setEnabled(false);
            sendingPacketsProgressBar.setVisibility(View.VISIBLE);
            sendingPacketsTextView.setVisibility(View.VISIBLE);
        }
        else
        {
            macAddressEditText.setEnabled(true);
            numOfPacketsEditText.setEnabled(true);
            sendButton.setEnabled(true);
            sendingPacketsProgressBar.setVisibility(View.INVISIBLE);
            sendingPacketsTextView.setVisibility(View.INVISIBLE);
        }
    }

    // Read and check the MAC address
    private String readMacAddress()
    {
        EditText eTextMacAddress;
        String destMacAddress;
        Toast wrongMacToast;

        wrongMacToast = Toast.makeText(getApplicationContext(), "Wrong MAC address", Toast.LENGTH_SHORT);

        eTextMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        destMacAddress = eTextMacAddress.getText().toString();
        if (!destMacAddress.matches("^([0-9A-Fa-f]{2}:){5}([0-9A-Fa-f]{2})$"))
        {
            destMacAddress = null;
            wrongMacToast.show();
        }

        return destMacAddress;
    }

    // Read and check the number of packets value
    private int readNumberOfPackets()
    {
        int numberOfPackets;
        EditText eTextNumOfPackets;
        Toast wrongNumOfPacketsToast;

        wrongNumOfPacketsToast = Toast.makeText(getApplicationContext(), "Wrong number of packets", Toast.LENGTH_SHORT);

        eTextNumOfPackets = (EditText)findViewById(R.id.editTextNumberOfPackets);
        try
        {
            numberOfPackets = Integer.parseInt(eTextNumOfPackets.getText().toString());

            if (numberOfPackets <= 0)
            {
                wrongNumOfPacketsToast.show();
            }
        }
        catch (NumberFormatException e)
        {
            numberOfPackets = -1;
            wrongNumOfPacketsToast.show();
        }

        return numberOfPackets;
    }

    // Converts a string mac address to its RAW representation (hexadecimal)
    private byte[] macAddressToRaw(String macAddress)
    {
        byte[] rawMacAddress;
        String[] macAddressSplitted;

        rawMacAddress = new byte[6];
        macAddressSplitted = macAddress.split(":");

        for (int i = 0; i < macAddressSplitted.length; i++)
        {
            rawMacAddress[i] = Integer.decode("0x" + macAddressSplitted[i]).byteValue();
        }

        return rawMacAddress;
    }

    // Start sending the requested magic packets
    private boolean sendMagicPackets(String destMacAddress, int numberOfPackets)
    {
        InetAddress broadcastAddress;
        DatagramSocket udpSocket;
        DatagramPacket magicPacket;
        byte[] magicPacketPayload, rawDestMacAddress;
        boolean allPacketsSentOk = false;

        Log.i("MainActivity", "Start sending magic packets...");

        try
        {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            rawDestMacAddress = macAddressToRaw(destMacAddress);

            // Create the magic packet
            magicPacketPayload = new byte[102];
            for (int i = 0; i < 6; i++)
            {
                magicPacketPayload[i] = (byte)0xFF;
            }
            for (int i = 6; i < magicPacketPayload.length; i++)
            {
                magicPacketPayload[i] = rawDestMacAddress[i % rawDestMacAddress.length];
            }

            magicPacket = new DatagramPacket(magicPacketPayload, 0, magicPacketPayload.length, broadcastAddress, 9);

            // Send the magic packets
            udpSocket = new DatagramSocket();
            udpSocket.setBroadcast(true);

            for (int i = 0; i < numberOfPackets; i++)
            {
                udpSocket.send(magicPacket);
                Thread.sleep(1000);
            }

            udpSocket.close();
            allPacketsSentOk = true;

            Log.i("MainActivity", "All packets were sent!");
        }
        catch (Exception e)
        {
            Log.e("MainActivity", "An error ocurred while sending the magic packets: " + e.getMessage());
        }

        return allPacketsSentOk;
    }
}

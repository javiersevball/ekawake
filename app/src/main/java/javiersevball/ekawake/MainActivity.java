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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.View;


public class MainActivity extends AppCompatActivity
{
    private static final String LAST_MAC_ADDRESS_KEY = "javiersevball.ekawake.LAST_MAC_ADDRESS";

    // Inner watcher for checking the user input
    private class MainActivityTextWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
            EditText eTextMacAddress, eTextNumOfPackets;
            TextInputLayout textLayoutMacAddress, textLayoutNumOfPackets;
            Button sendButton;
            String macAddress;
            int numOfPackets;
            boolean macAddressOk = false, numOfPacketsOk = false;

            eTextMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
            eTextNumOfPackets = (EditText)findViewById(R.id.editTextNumberOfPackets);
            textLayoutMacAddress = (TextInputLayout)findViewById(R.id.textInputLayoutMacAddress);
            textLayoutNumOfPackets = (TextInputLayout)findViewById(R.id.textInputLayoutNumberOfPackets);
            sendButton = (Button)findViewById(R.id.sendPacketsButton);

            // Check the MAC address
            macAddress = eTextMacAddress.getText().toString();
            if (macAddress.matches("^([0-9A-Fa-f]{2}:){5}([0-9A-Fa-f]{2})$"))
            {
                textLayoutMacAddress.setError(null);
                macAddressOk = true;
            }
            else
            {
                textLayoutMacAddress.setError("Invalid MAC");
            }

            // Check the number of packets
            try
            {
                numOfPackets = Integer.parseInt(eTextNumOfPackets.getText().toString());
            }
            catch (NumberFormatException e)
            {
                numOfPackets = -1;
            }

            if (numOfPackets > 0)
            {
                textLayoutNumOfPackets.setError(null);
                numOfPacketsOk = true;
            }
            else
            {
                textLayoutNumOfPackets.setError("Invalid number");
            }

            // Enable or disable the send button according to the user input
            sendButton.setEnabled(macAddressOk && numOfPacketsOk);
        }

        @Override
        public void afterTextChanged(Editable s)
        {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        EditText eTextMacAddress, eTextNumOfPackets;
        SharedPreferences sharedPref;
        String lastMacAddressValue;
        MainActivityTextWatcher textWatcher;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eTextMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        eTextNumOfPackets = (EditText)findViewById(R.id.editTextNumberOfPackets);

        // Read the last valid destination MAC address introduced by the user
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        lastMacAddressValue = sharedPref.getString(LAST_MAC_ADDRESS_KEY,null);

        if (lastMacAddressValue != null)
        {
            eTextMacAddress.setText(lastMacAddressValue);
        }

        // Add events for checking the user input
        textWatcher = new MainActivityTextWatcher();
        eTextMacAddress.addTextChangedListener(textWatcher);
        eTextNumOfPackets.addTextChangedListener(textWatcher);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    // Sets the visibility and the enable status of the UI elements according to the "sending" status
    public void setUiElementsEnabled(boolean sending)
    {
        TextInputLayout textLayoutMacAddress, textLayoutNumOfPackets;
        Button sendButton;
        ProgressBar sendingPacketsProgressBar;
        TextView sendingPacketsTextView;
        boolean userInputEnable;
        int progressVisibility;

        textLayoutMacAddress = (TextInputLayout)findViewById(R.id.textInputLayoutMacAddress);
        textLayoutNumOfPackets = (TextInputLayout)findViewById(R.id.textInputLayoutNumberOfPackets);
        sendButton = (Button)findViewById(R.id.sendPacketsButton);
        sendingPacketsProgressBar = (ProgressBar)findViewById(R.id.sendingProgressBar);
        sendingPacketsTextView = (TextView)findViewById(R.id.sendingTextView);

        userInputEnable = !sending;
        progressVisibility = (sending) ? View.VISIBLE : View.INVISIBLE;

        textLayoutMacAddress.setEnabled(userInputEnable);
        textLayoutNumOfPackets.setEnabled(userInputEnable);
        sendButton.setEnabled(userInputEnable);
        sendingPacketsProgressBar.setVisibility(progressVisibility);
        sendingPacketsTextView.setVisibility(progressVisibility);
    }

    // Send button clicked
    public void sendButtonClicked(View view)
    {
        int numberOfPackets;
        String destMacAddress;
        SendMagicPacketsTaskParams sendTaskParams;
        SharedPreferences sharedPref;
        SharedPreferences.Editor sharedPrefEditor;
        AlertDialog.Builder builder;
        AlertDialog noLanDialog;

        Log.i("MainActivity", "SEND button pressed.");

        destMacAddress = readMacAddress();
        numberOfPackets = readNumberOfPackets();

        // Save the destination MAC address
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putString(LAST_MAC_ADDRESS_KEY, destMacAddress);
        sharedPrefEditor.apply();

        // Check if the device is connected to a LAN network
        if (checkLanConnection())
        {
            sendTaskParams = new SendMagicPacketsTaskParams(destMacAddress, numberOfPackets);
            new SendMagicPacketsTask(this).execute(sendTaskParams);
        }
        else
        {
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Error");
            builder.setMessage("The device is not connected to any LAN.");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                }
            });

            noLanDialog = builder.create();
            noLanDialog.show();
        }
    }

    // Read and check the MAC address
    private String readMacAddress()
    {
        EditText eTextMacAddress;
        String destMacAddress;

        eTextMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        destMacAddress = eTextMacAddress.getText().toString();

        return destMacAddress;
    }

    // Read and check the number of packets
    private int readNumberOfPackets()
    {
        int numberOfPackets;
        EditText eTextNumOfPackets;

        eTextNumOfPackets = (EditText)findViewById(R.id.editTextNumberOfPackets);

        try
        {
            numberOfPackets = Integer.parseInt(eTextNumOfPackets.getText().toString());

        }
        catch (NumberFormatException e)
        {
            numberOfPackets = -1;
        }

        return numberOfPackets;
    }

    // Check if the device is connected to any LAN
    private boolean checkLanConnection()
    {
        boolean activeNetworkIsLan;
        ConnectivityManager cManager;
        NetworkInfo netInfo;

        cManager =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        netInfo = cManager.getActiveNetworkInfo();

        activeNetworkIsLan = netInfo.getType() == ConnectivityManager.TYPE_WIFI;
        activeNetworkIsLan |= netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

        return activeNetworkIsLan && netInfo.isConnected();
    }
}

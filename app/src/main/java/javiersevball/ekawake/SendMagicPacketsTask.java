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

import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


// AsyncTask worker for sending the requested magic packets to the destination host
class SendMagicPacketsTask extends AsyncTask<SendMagicPacketsTaskParams, Void, Boolean>
{
    private MainActivity mainActivity;

    SendMagicPacketsTask(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
    }

    // Method invoked on the UI thread before the task is executed
    protected void onPreExecute()
    {
        mainActivity.setUiElementsEnabled(true);
    }

    // Invoked on the background thread immediately after onPreExecute() finishes executing
    protected Boolean doInBackground(SendMagicPacketsTaskParams... params)
    {
        boolean allPacketsSentOk;
        Boolean result;

        allPacketsSentOk =
                sendMagicPackets(
                        params[0].getDestinationMacAddress(),
                        params[0].getNumberOfPacketsToSend());
        result = (allPacketsSentOk) ? Boolean.TRUE : Boolean.FALSE;

        return result;
    }

    // Method executed on the UI thread after the background computation finishes
    protected void onPostExecute(Boolean result)
    {
        mainActivity.setUiElementsEnabled(false);
    }

    // Converts a string mac address to its RAW representation
    private byte[] convertMacAddressToRaw(String macAddress)
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
            rawDestMacAddress = convertMacAddressToRaw(destMacAddress);

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

            magicPacket =
                    new DatagramPacket(magicPacketPayload, 0, magicPacketPayload.length, broadcastAddress, 9);

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

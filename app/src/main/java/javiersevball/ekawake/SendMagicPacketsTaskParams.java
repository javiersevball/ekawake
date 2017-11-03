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


// Class that contains the parameters required by the SendMagicPacketsTask
class SendMagicPacketsTaskParams {
    private String destinationMacAddress;
    private int numberOfPacketsToSend;

    SendMagicPacketsTaskParams()
    {
        destinationMacAddress = null;
        numberOfPacketsToSend = -1;
    }

    SendMagicPacketsTaskParams(String macAddr, int numOfPackets)
    {
        destinationMacAddress = macAddr;
        numberOfPacketsToSend = numOfPackets;
    }

    String getDestinationMacAddress()
    {
        return destinationMacAddress;
    }

    int getNumberOfPacketsToSend()
    {
        return numberOfPacketsToSend;
    }

    void setDestinationMacAddress(String macAddress)
    {
        destinationMacAddress = macAddress;
    }

    void setNumberOfPacketsToSend(int numOfPackets)
    {
        numberOfPacketsToSend = numOfPackets;
    }
}


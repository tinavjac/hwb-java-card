
/*
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
 */

package hwbsampleclient;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.oracle.javacard.ams.config.AID;

public class HWBClientAPDUs extends HWBSampleClient{

    static final CommandAPDU selectAppletAPDU = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID.from(sAID_AppletInstance).toBytes(), 256);

    /**
     * Fill in this method with communication with the applet and processing responses.
     * @param channel allows sending APDUs and receives responses via .transmit method
     */
    @Override
    void interactWithApplet(CardChannel channel) throws CardException {
            System.out.println("Select:");
            printAPDU(selectAppletAPDU);
            ResponseAPDU responseSelect = channel.transmit(selectAppletAPDU);
            printAPDU(responseSelect);
            System.out.println();

            // int Le = 0x10;
            // byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
            // CommandAPDU apduWithData    = new CommandAPDU(0x80, 0x00, 0x00, 0x00, data, Le);
            // CommandAPDU apduWithoutData = new CommandAPDU(0x80, 0x00, 0x00, 0x00, Le);
            // ...
            // ResponseAPDU response = channel.transmit(apdu);
    }

    public static void main(String[] args) {
        // give installation parameters to the simulator (physical card needs to get them separately)
        // byte[] installParams = { /**0x01, 0x02, 0x03, 0x04...**/ };
        byte[] installParams = {0x01, 0x02, 0x03, 0x04};
        new HWBClientAPDUs().setupApplet(args, installParams);
    }
}

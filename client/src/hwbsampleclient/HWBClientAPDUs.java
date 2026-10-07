
/*
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
 */
package hwbsampleclient;

import java.util.Arrays;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.oracle.javacard.ams.config.AID;

public class HWBClientAPDUs extends HWBSampleClient {

    static final CommandAPDU selectAppletAPDU = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID.from(sAID_AppletInstance).toBytes(), 256);

    /**
     * Fill in this method with communication with the applet and processing
     * responses.
     *
     * @param channel allows sending APDUs and receives responses via .transmit
     * method
     */
    @Override
    void interactWithApplet(CardChannel channel) throws CardException {
        sendAndCheck(channel, selectAppletAPDU, 0x9000, null, "Select applet");

        byte[] expectedName = { 'J', 'A', 'C', 'H', 'Y', 'M' };
        byte[] data = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A,
            0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14
        };

        sendAndCheck(channel, new CommandAPDU(0x80, 0x00, 0x00, 0x00, expectedName.length),
                0x9000, expectedName, "Read name");
        sendAndCheck(channel, new CommandAPDU(0x80, 0x02, 0x00, 0x00, data),
                0x9000, new byte[0], "Store data");
        sendAndCheck(channel, new CommandAPDU(0x80, 0x04, 0x00, 0x00, data.length),
                0x9000, data, "Read stored data");
                
        sendAndCheck(channel, new CommandAPDU(0x00, 0x00, 0x00, 0x00, expectedName.length),
                0x6E00, null, "Reject unsupported CLA");
        sendAndCheck(channel, new CommandAPDU(0x80, 0x7F, 0x00, 0x00, expectedName.length),
                0x6D00, null, "Reject unsupported INS");
        sendAndCheck(channel, new CommandAPDU(0x80, 0x02, 0x00, 0x00, new byte[21]),
                0x6700, null, "Reject oversized input");

        // Simulator resends INS=0x04 with the corrected Le
        // after the applet throws 6Cxx, so the client sees the data and 9000 instead??? 
        //   
        // sendAndCheck(channel, new CommandAPDU(0x80, 0x04, 0x00, 0x00, data.length - 1),
        //         0x6C00 | data.length, null, "Report correct Le");
    }

    private void sendAndCheck(CardChannel channel, CommandAPDU command, int expectedStatus,
            byte[] expectedData, String testName) throws CardException {
        System.out.println(testName + ":");
        printAPDU(command);
        ResponseAPDU response = channel.transmit(command);
        printAPDU(response);

        if (response.getSW() != expectedStatus) {
            throw new CardException(testName + " failed: expected SW "
                    + String.format("%04X", expectedStatus) + ", got "
                    + String.format("%04X", response.getSW()));
        }
        if (expectedData != null && !Arrays.equals(response.getData(), expectedData)) {
            throw new CardException(testName + " failed: unexpected response data");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // give installation parameters to the simulator (physical card needs to get them separately)
        // byte[] installParams = { /**0x01, 0x02, 0x03, 0x04...**/ };
        byte[] installParams = {0x01, 0x02, 0x03, 0x04};
        new HWBClientAPDUs().setupApplet(args, installParams);
    }
}

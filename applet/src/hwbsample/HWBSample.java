/**
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 */


package hwbsample;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * Applet class
 *
 * @author <user>
 */

public class HWBSample extends Applet {

    private static final short MAX_DATA_LENGTH = 20;

    private final byte[] storedData = new byte[MAX_DATA_LENGTH];
    private final byte[] name = { 'J', 'A', 'C', 'H', 'Y', 'M' };
    private short storedLength;

    /**
     * Installs this applet.
     *
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HWBSample(bArray, bOffset, bLength);
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected HWBSample(byte[] bArray, short bOffset, byte bLength) {
        register(bArray,((short)(bOffset + 1)), bArray[bOffset]);
    }

    /**
     * Processes an incoming APDU.
     *
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */

    // Note: Old java does not support @Override
    public void process(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();

        if (selectingApplet()) {
            return;
        }

        if (apduBuffer[ISO7816.OFFSET_CLA] != (byte) 0x80) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (apduBuffer[ISO7816.OFFSET_INS]) {
            case 0x00:
                sendName(apdu, name, (short) name.length);
                return;
            case 0x02:
                receiveData(apdu);
                return;
            case 0x04:
                sendStoredData(apdu);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void sendName(APDU apdu, byte[] data, short length) {
        apdu.setOutgoing();
        apdu.setOutgoingLength(length);
        apdu.sendBytesLong(data, (short) 0, length);
    }

    private void receiveData(APDU apdu) {
        short receivedLength = apdu.setIncomingAndReceive();
        short incomingLength = apdu.getIncomingLength();
        if (incomingLength > MAX_DATA_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        byte[] apduBuffer = apdu.getBuffer();
        short dataOffset = apdu.getOffsetCdata();
        while (receivedLength < incomingLength) {
            short bytesRead = apdu.receiveBytes((short) (dataOffset + receivedLength));
            if (bytesRead <= 0) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            receivedLength += bytesRead;
        }
        if (receivedLength != incomingLength) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        Util.arrayCopyNonAtomic(apduBuffer, dataOffset, storedData, (short) 0, incomingLength);
        storedLength = incomingLength;
    }

    private void sendStoredData(APDU apdu) {
        short expectedLength = apdu.setOutgoing();
        if (expectedLength != storedLength) {
            ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00 | storedLength));
        }
        apdu.setOutgoingLength(storedLength);
        apdu.sendBytesLong(storedData, (short) 0, storedLength);
    }
}
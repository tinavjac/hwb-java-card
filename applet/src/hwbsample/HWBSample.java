/**
 * Copyright (c) 1998, 2025, Oracle and/or its affiliates. All rights reserved.
 *
 */


package hwbsample;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;

/**
 * Applet class
 *
 * @author <user>
 */

public class HWBSample extends Applet {

    private static final short MAX_DATA_LENGTH = 20;
    private static final byte PIN_TRY_LIMIT = 3;
    private static final byte MAX_PIN_LENGTH = 8;
    
    private static final short SW_PIN_VERIFICATION_REQUIRED = (short) 0x6301;
    private static final short SW_VERIFICATION_FAILED = (short) 0x6300;

    private final byte[] storedData = new byte[MAX_DATA_LENGTH];
    private final byte[] name = { 'J', 'A', 'C', 'H', 'Y', 'M' };
    private final OwnerPIN pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_LENGTH);
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
        short installEnd = (short) (bOffset + (bLength & 0xFF));
        if (bOffset >= installEnd) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        short aidLength = (short) (bArray[bOffset] & 0xFF);
        short privilegesOffset = (short) (bOffset + 1 + aidLength);
        if (privilegesOffset >= installEnd) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short privilegesLength = (short) (bArray[privilegesOffset] & 0xFF);
        short pinLengthOffset = (short) (privilegesOffset + 1 + privilegesLength);
        if (pinLengthOffset >= installEnd) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short pinOffset = (short) (pinLengthOffset + 1);
        short pinLength = (short) (bArray[pinLengthOffset] & 0xFF);

        if (pinLength == 0 || pinLength > MAX_PIN_LENGTH || pinOffset + pinLength > installEnd) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        pin.update(bArray, pinOffset, (byte) pinLength);
        register(bArray, (short) (bOffset + 1), (byte) aidLength);
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
            case 0x20:
                verifyPin(apdu);
                return;
            case 0x02:
                requireVerifiedPin();
                receiveData(apdu);
                return;
            case 0x04:
                requireVerifiedPin();
                sendStoredData(apdu);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    public boolean select() {
        return pin.getTriesRemaining() > 0;
    }

    public void deselect() {
        pin.reset();
    }

    private void requireVerifiedPin() {
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
    }

    private void verifyPin(APDU apdu) {
        short receivedLength = apdu.setIncomingAndReceive();
        short incomingLength = apdu.getIncomingLength();
        if (incomingLength == 0 || incomingLength > MAX_PIN_LENGTH) {
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

        if (!pin.check(apduBuffer, dataOffset, (byte) incomingLength)) {
            ISOException.throwIt(SW_VERIFICATION_FAILED);
        }
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

    private void sendName(APDU apdu, byte[] data, short length) {
        apdu.setOutgoing();
        apdu.setOutgoingLength(length);
        apdu.sendBytesLong(data, (short) 0, length);
    }

}
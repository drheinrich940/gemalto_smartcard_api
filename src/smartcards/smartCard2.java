package smartcards;

import javax.smartcardio.*;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Guillaume
 */
public class smartCard2 {
    private static CardTerminal terminal;
    private static Card carte;
    private static int i;
    private static String texte = new String();

    static public List<CardTerminal> getTerminals() throws CardException {
        return TerminalFactory.getDefault().terminals().list();

    }

    static public String toString(byte[] byteTab) {
        String texte = "";
        String hexNombre;
        for (i = 0; i < byteTab.length; i++) {
            hexNombre = "";
            hexNombre = Integer.toHexString(byteTab[i]);
            if (hexNombre.length() == 1) {
                texte += " 0" + hexNombre;
            } else {
                texte += " " + hexNombre;
            }
        }
        return texte;
    }

    static public int authCSCDefault(CardChannel channel, int CSCid) throws InvalidSecretCodeException, UnknownModeException, InvalidLcValueException, MaxPresentationExceededException, InvalidP2ParameterException, InvalidInstructionByteException, UnknownException {

        byte[] bytes;
        if (CSCid == 0) {
            CSCid = 0x07;
            bytes = ByteBuffer.allocate(4).putInt(0xAAAAAAAA).array();
        } else if (CSCid == 1) {
            CSCid = 0x39;
            bytes = ByteBuffer.allocate(4).putInt(0x11111111).array();
        } else if (CSCid == 2) {
            CSCid = 0x3B;
            bytes = ByteBuffer.allocate(4).putInt(0x22222222).array();
        } else return -1;


        CommandAPDU command = new CommandAPDU(0x00, 0x20, 0x00, CSCid, bytes);
        ResponseAPDU r;
        try {
            r = channel.transmit(command);
            texte = toString(r.getData());
            System.out.println(texte);
            int SW1 = r.getSW1();
            if (SW1 == 144) {
                System.out.println("Auth Success");
                return 0;
            } else if (SW1 == 99) throw new InvalidSecretCodeException("Error : Invalid secret code");
            else if (SW1 == 101) throw new UnknownModeException("Error : Requested mode is unknown");
            else if (SW1 == 103) throw new InvalidLcValueException("Error : Invalid Lc value");
            else if (SW1 == 105)
                throw new MaxPresentationExceededException("Error : Maximum secret code presentation exceeded");
            else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
            else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
            else throw new UnknownException("Error : Encountered an unknown response to authentication attempt");
        } catch (CardException e) {
            e.printStackTrace();
            return -1;
        }
    }

    static public int authCSC(CardChannel channel, int CSCid, int password) throws InvalidSecretCodeException, UnknownModeException, InvalidLcValueException, MaxPresentationExceededException, InvalidP2ParameterException, InvalidInstructionByteException, UnknownException, InvalidNumberOfDigitsException {

        if (CSCid == 0) CSCid = 0x07;
        else if (CSCid == 1) CSCid = 0x39;
        else if (CSCid == 2) CSCid = 0x3B;
        else return -1;

        if (utils.countDigits(password) != 6) {
            throw new InvalidNumberOfDigitsException("Error : wrong digit password format");
        } else {
            byte[] bytes = ByteBuffer.allocate(4).putInt(0x22222222).array();

            CommandAPDU command = new CommandAPDU(0x00, 0x20, 0x00, CSCid, bytes);
            ResponseAPDU r;
            try {
                r = channel.transmit(command);
                texte = toString(r.getData());
                System.out.println(texte);
                int SW1 = r.getSW1();
                if (SW1 == 144) {
                    System.out.println("Auth Success");
                    return 0;
                } else if (SW1 == 99) throw new InvalidSecretCodeException("Error : Invalid secret code");
                else if (SW1 == 101) throw new UnknownModeException("Error : Requested mode is unknown");
                else if (SW1 == 103) throw new InvalidLcValueException("Error : Invalid Lc value");
                else if (SW1 == 105)
                    throw new MaxPresentationExceededException("Error : Maximum secret code presentation exceeded");
                else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
                else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
                else throw new UnknownException("Error : Encountered an unknown response to authentication attempt");
            } catch (CardException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    public static int writeCSC(CardChannel channel, int CSCid, int digitPassword) throws InvalidNumberOfDigitsException, InvalidCSCIdException, InvalidLcValueException, InvalidP2ParameterException, InvalidInstructionByteException, MemoryErrorException, SecurityNotSatisfiedException, UnknownException {

        if (CSCid == 0) CSCid = 0x06;
        else if (CSCid == 1) CSCid = 0x38;
        else if (CSCid == 2) CSCid = 0x3A;
        else throw new InvalidCSCIdException("Error : CSC id must be 0, 1 or 2");

        if (utils.countDigits(digitPassword) != 6) {
            throw new InvalidNumberOfDigitsException("Error : wrong digit password format");
        } else {
            byte[] bytes = ByteBuffer.allocate(4).putInt(digitPassword).array();

            CommandAPDU command = new CommandAPDU(0x80, 0xDE, 0x00, CSCid, bytes, 0x04);
            ResponseAPDU r;
            try {
                r = channel.transmit(command);
                texte = toString(r.getData());
                System.out.println(texte);
                int SW1 = r.getSW1();
                if (SW1 == 144) {
                    System.out.println("CSC update successfully executed");
                    return 0;
                }
                else if (SW1 == 101) throw new MemoryErrorException("Error : Encoutered a memory error");
                else if (SW1 == 103) throw new InvalidLcValueException("Error : Invalid Lc value");
                else if (SW1 == 105) throw new SecurityNotSatisfiedException("Error : wrong word balance update order, flag update attempt or security issue");
                else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
                else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
                else throw new UnknownException("Error : Encountered an unknown response to update attempt");
            } catch (CardException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    private static int update(CardChannel channel, int p2, int lc, byte[] data) throws MemoryErrorException, InvalidLcValueException, SecurityNotSatisfiedException, InvalidP2ParameterException, InvalidInstructionByteException, UnknownException {
        CommandAPDU command = new CommandAPDU(0x80, 0xDE, 0x00, p2, data, lc);
        ResponseAPDU r;
        try {
            r = channel.transmit(command);
            texte = toString(r.getData());
            System.out.println(texte);
            int SW1 = r.getSW1();
            if (SW1 == 144) {
                System.out.println("CSC update successfully executed");
                return 0;
            }
            else if (SW1 == 101) throw new MemoryErrorException("Error : Encoutered a memory error");
            else if (SW1 == 103) throw new InvalidLcValueException("Error : Invalid Lc value");
            else if (SW1 == 105) throw new SecurityNotSatisfiedException("Error : wrong word balance update order, flag update attempt or security issue");
            else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
            else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
            else throw new UnknownException("Error : Encountered an unknown response to update attempt");
        } catch (CardException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void main(String[] args) throws CardException {
        List<CardTerminal> terminauxDispos = smartCard2.getTerminals();
        //Premier terminal dispo
        terminal = terminauxDispos.get(0);
        System.out.println(terminal.toString());
        //Connexion à la carte
        carte = terminal.connect("T=0");
        //ATR (answer To Reset)
        System.out.println(toString(carte.getATR().getBytes()));

        CardChannel channel = carte.getBasicChannel();
        //CommandAPDU commande = new CommandAPDU(0x80,0xBE,0x01,0x00,0x04);

        //ResponseAPDU r = channel.transmit(commande);
        //System.out.println("reponse : " + (byte) r.getData()[0]);
        //texte = toString(r.getData());
        //System.out.println(texte);

        int authResult = -1;
        try {
            authResult = authCSCDefault(channel, 0);
        } catch (InvalidSecretCodeException | UnknownModeException | InvalidLcValueException | MaxPresentationExceededException | InvalidP2ParameterException | InvalidInstructionByteException | UnknownException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(authResult);

        try {
            int writeResult = writeCSC(channel, 0, 123456);
            System.out.println("Wire result = " + writeResult);
        } catch (InvalidNumberOfDigitsException | InvalidCSCIdException | InvalidLcValueException | InvalidP2ParameterException | InvalidInstructionByteException | MemoryErrorException | SecurityNotSatisfiedException | UnknownException e) {
            System.out.println(e.getMessage());
        }

        carte.disconnect(false);

        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        /*try {
            writeCSC(channel, 0, 123456);
        } catch (InvalidNumberOfDigitsException | InvalidCSCIdException e) {
            e.printStackTrace();
        }*/

    }

    static class InvalidNumberOfDigitsException extends Exception {
        public InvalidNumberOfDigitsException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class InvalidCSCIdException extends Exception {
        public InvalidCSCIdException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class InvalidSecretCodeException extends Exception {
        public InvalidSecretCodeException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class UnknownModeException extends Exception {
        public UnknownModeException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class InvalidLcValueException extends Exception {
        public InvalidLcValueException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class MaxPresentationExceededException extends Exception {
        public MaxPresentationExceededException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class InvalidP2ParameterException extends Exception {
        public InvalidP2ParameterException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class InvalidInstructionByteException extends Exception {
        public InvalidInstructionByteException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class MemoryErrorException extends Exception {
        public MemoryErrorException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class SecurityNotSatisfiedException extends Exception {
        public SecurityNotSatisfiedException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class UnknownException extends Exception {
        public UnknownException(String errorMessage) {
            super(errorMessage);
        }
    }
}
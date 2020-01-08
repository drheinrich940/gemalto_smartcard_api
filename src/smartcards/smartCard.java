package smartcards;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 * @author Guillaume
 */
public class smartCard {
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
            byte[] bytes = ByteBuffer.allocate(4).putInt(password).array();
            return verify(channel, CSCid, bytes);
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
            return update(channel, CSCid, 0x04, bytes);
        }
    }

    private static int read(CardChannel channel, int p2, int le) throws UnknownModeException, InvalidLenghtOfExpectedDataException, SecurityNotSatisfiedException, InvalidP2ParameterException, InvalidInstructionByteException, UnknownException {
        CommandAPDU command = new CommandAPDU(0x80, 0xBE, 0x00, p2, le);
        ResponseAPDU r;
        try {
            r = channel.transmit(command);
            texte = toString(r.getData());
            System.out.println(texte);
            int SW1 = r.getSW1();
            if (SW1 == 144) {
                System.out.println("Read successfully executed");
                return 0;
            } else if (SW1 == 101) throw new UnknownModeException("Error : Encoutered a memory error");
            else if (SW1 == 103)
                throw new InvalidLenghtOfExpectedDataException("Error : Invalid lenght of expected data");
            else if (SW1 == 105)
                throw new SecurityNotSatisfiedException("Error : wrong word balance update order, flag update attempt or security issue");
            else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
            else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
            else throw new UnknownException("Error : Encountered an unknown response to update attempt");

        } catch (CardException e) {
            e.printStackTrace();
            return -1;
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
                System.out.println("Update successfully executed");
                return 0;
            } else if (SW1 == 101) throw new MemoryErrorException("Error : Encoutered a memory error");
            else if (SW1 == 103) throw new InvalidLcValueException("Error : Invalid Lc value");
            else if (SW1 == 105)
                throw new SecurityNotSatisfiedException("Error : wrong word balance update order, flag update attempt or security issue");
            else if (SW1 == 107) throw new InvalidP2ParameterException("Error : Invalid Lc value");
            else if (SW1 == 109) throw new InvalidInstructionByteException("Error : Invalid Lc value");
            else throw new UnknownException("Error : Encountered an unknown response to update attempt");
        } catch (CardException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static int verify(CardChannel channel, int p2, byte[] data) throws InvalidSecretCodeException, UnknownModeException, InvalidLcValueException, MaxPresentationExceededException, InvalidP2ParameterException, InvalidInstructionByteException, UnknownException {
        CommandAPDU command = new CommandAPDU(0x00, 0x20, 0x00, p2, data);
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

    public static void main(String[] args) throws CardException {
        List<CardTerminal> terminauxDispos = smartCard.getTerminals();
        terminal = terminauxDispos.get(0);
        System.out.println(terminal.toString());
        carte = terminal.connect("T=0");
        System.out.println(toString(carte.getATR().getBytes()));
        CardChannel channel = carte.getBasicChannel();

        runTest2(channel);

        carte.disconnect(false);

    }

    private static void runTest0(CardChannel channel) {
        CommandAPDU commande = new CommandAPDU(0x80, 0xBE, 0x01, 0x00, 0x04);
        ResponseAPDU r = null;
        try {
            r = channel.transmit(commande);
        } catch (CardException e) {
            e.printStackTrace();
        }
        System.out.println("reponse : " + (byte) r.getData()[0]);
        texte = toString(r.getData());
        System.out.println(texte);
    }

    private static void runTest1(CardChannel channel) {
        int authResult = -1;
        try {
            authResult = authCSC(channel, 0, 123456);
        } catch (InvalidSecretCodeException | UnknownModeException | InvalidLcValueException | MaxPresentationExceededException | InvalidP2ParameterException | InvalidInstructionByteException | UnknownException | InvalidNumberOfDigitsException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(authResult);

        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        try {
            int writeResult = writeCSC(channel, 0, 234567);
            System.out.println("Write result = " + writeResult);
        } catch (InvalidNumberOfDigitsException | InvalidCSCIdException | InvalidLcValueException | InvalidP2ParameterException | InvalidInstructionByteException | MemoryErrorException | SecurityNotSatisfiedException | UnknownException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void runTest2(CardChannel channel) {

        int readResult = -1;
        try {
            readResult = read(channel, 0x10, 0x40);
        } catch (UnknownModeException | InvalidLenghtOfExpectedDataException | SecurityNotSatisfiedException | InvalidP2ParameterException | InvalidInstructionByteException | UnknownException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(readResult);
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        int authResult = -1;
        try {
            authResult = authCSC(channel, 0, 234567);
        } catch (InvalidSecretCodeException | UnknownModeException | InvalidLcValueException | MaxPresentationExceededException | InvalidP2ParameterException | InvalidInstructionByteException | UnknownException | InvalidNumberOfDigitsException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(authResult);
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        String original = "drheinrich;passsword";
        byte[] b = original.getBytes();
        System.out.println(toString(original.getBytes()));
        String s = new String(b);
        System.out.println(s);

        int amount = b.length / 4;
        int rest = b.length % 4;
        System.out.println(amount);
        System.out.println(rest);

        byte[][] phrase;

        if (rest == 0) phrase = new byte[amount][4];
        else phrase = new byte[amount + 1][4];
        int from = 0;

        for (int i = 0; i < amount; i++) {
            int to = from + 4;
            //update(channel, 0x10, 0x04, Arrays.copyOfRange(b, from, to));
            System.out.println(toString(Arrays.copyOfRange(b, from, to)));
            System.out.println(new String(Arrays.copyOfRange(b, from, to)));
            from += 4;
        }
        System.out.println(toString(Arrays.copyOfRange(b, from, from+rest)));
        System.out.println(new String(Arrays.copyOfRange(b, from, from+rest)));

        //phrase[0] = Arrays.copyOfRange(b, 0, 3);

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

    static class InvalidLenghtOfExpectedDataException extends Exception {
        public InvalidLenghtOfExpectedDataException(String errorMessage) {
            super(errorMessage);
        }
    }

    static class UnknownException extends Exception {
        public UnknownException(String errorMessage) {
            super(errorMessage);
        }
    }
}
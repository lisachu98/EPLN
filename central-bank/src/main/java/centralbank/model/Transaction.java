package centralbank.model;

import centralbank.util.StringUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class Transaction {
    private String transactionId;
    private String sender;
    private String receiver;
    private float amount;
    private long timestamp;
    private byte[] signature;
    private static int sequence = 0;

    public Transaction(String sender, String receiver, float amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = calculateTransactionId();
    }

    private String calculateTransactionId() {
        sequence++;
        return StringUtil.applySha256(
                sender +
                        receiver +
                        Float.toString(amount) +
                        Long.toString(timestamp) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = sender + receiver + Float.toString(amount) + Long.toString(timestamp);
        signature = StringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = sender + receiver + Float.toString(amount) + Long.toString(timestamp);
        PublicKey senderPub = null;
        try {
            senderPub = StringUtil.getPublicKeyFromString(this.sender);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return StringUtil.verifyECDSASig(senderPub, data, signature);
    }

    public boolean processTransaction() {
        if (verifySignature() == false) {
            System.out.println("Transaction signature failed to verify");
            return false;
        }
        return true;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getReceiver() {
        return receiver;
    }

    public float getAmount() {
        return amount;
    }

    public String getSender() {
        return sender;
    }
}

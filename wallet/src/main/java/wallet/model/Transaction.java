package wallet.model;

import wallet.util.StringUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class Transaction {
    private String transactionId;
    private PublicKey sender;
    private PublicKey receiver;
    private float amount;
    private long timestamp;
    private byte[] signature;
    private static int sequence = 0;

    public Transaction(PublicKey sender, PublicKey receiver, float amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = calculateTransactionId();
    }

    private String calculateTransactionId() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(receiver) +
                        Float.toString(amount) +
                        Long.toString(timestamp) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(receiver) + Float.toString(amount) + Long.toString(timestamp);
        signature = StringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(receiver) + Float.toString(amount) + Long.toString(timestamp);
        return StringUtil.verifyECDSASig(sender, data, signature);
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
}

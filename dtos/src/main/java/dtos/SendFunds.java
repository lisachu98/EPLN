package dtos;

import java.security.PrivateKey;
import java.security.PublicKey;

public class SendFunds {
    private String sender;
    private String senderPrivateKey;
    private String receiver;
    private float amount;

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getSender() {
        return sender;
    }

    public String getSenderPrivateKey() {
        return senderPrivateKey;
    }
}

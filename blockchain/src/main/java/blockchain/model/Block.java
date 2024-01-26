package blockchain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

public class Block {
    private String hash;
    private String previousHash;
    private ArrayList<Transaction> transactions;
    private long timeStamp;
    private int nonce;

    public Block(String previousHash){
        this.transactions = new ArrayList();
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public String calculateHash(){
        String hashData = previousHash + Long.toString(timeStamp) + Integer.toString(nonce);
        for (Transaction transaction : transactions) {
            hashData += transaction.getTransactionId();
        }
        MessageDigest digest = null;
        byte[] bytes = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            bytes = digest.digest(hashData.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
        }
        StringBuffer buffer = new StringBuffer();
        for (byte b : bytes){
            buffer.append(String.format("%02x", b));
        }
        return buffer.toString();
    }

    public String mineBlock(int prefix) {
        String prefixString = new String(new char[prefix]).replace('\0', '0');
        while (!hash.substring(0, prefix).equals(prefixString)) {
            nonce++;
            hash = calculateHash();
        }
        return hash;
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }
}

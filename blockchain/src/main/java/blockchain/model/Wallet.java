package blockchain.model;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

import blockchain.util.StringUtil;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

public class Wallet {
    private String accountId;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private float balance;

    public Wallet() {
        this.balance = 0;
        generateKeyPair();
        this.accountId = StringUtil.getStringFromKey(publicKey).substring(0,9);
        System.out.println("Wallet initialized");
    }

    public Wallet(String accountId) {
        this.balance = 0;
        this.accountId = accountId;
        System.out.println("Wallet initialized");
    }

    public String getAccountId() {
        return accountId;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public void credit(float amount) {
        this.balance += amount;
    }

    public boolean debit(float amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public void generateKeyPair(){
        //Security.addProvider(new BouncyCastleProvider());
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            keyGen.initialize(256, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
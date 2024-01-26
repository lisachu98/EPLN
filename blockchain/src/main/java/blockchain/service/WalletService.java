package blockchain.service;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

import blockchain.util.StringUtil;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import blockchain.model.Transaction;

@Service
public class WalletService {
    private String accountId;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private float balance;
    @Autowired
    private SimpMessagingTemplate template;

    @PostConstruct
    public void initializeWallet() {
        this.balance = 0;
        generateKeyPair();
        this.accountId = StringUtil.getStringFromKey(publicKey);
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
        Security.addProvider(new BouncyCastleProvider());
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction sendFunds(PublicKey receiver, float amount) {
        if (getBalance() < amount) {
            System.out.println("Insufficient funds");
            return null;
        }

        Transaction transaction = new Transaction(publicKey, receiver, amount);
        transaction.generateSignature(privateKey);

        boolean debitSuccessful = debit(amount);

        if (!debitSuccessful) {
            return null;
        }
        String transactionJson = StringUtil.getJson(transaction);
        template.convertAndSend("/topic/transaction", transactionJson);
        System.out.println("Transaction sent to " + StringUtil.getStringFromKey(receiver) + " from " + StringUtil.getStringFromKey(publicKey) + " for " + amount + " coins");
        return transaction;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
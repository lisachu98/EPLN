package centralbank.model;

import centralbank.util.StringUtil;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

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

    public Wallet(String accountId, int n) {
        this.balance = 0;
        this.accountId = accountId;
        String pub = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEbbkHdn3quLw1SRoJfWA27hKglgnO52LWHsW0SIRxfgdJXfVwibqSFsj9DolwdqeAZ+dq+wND2chvEcgbZ7/pQ==";
        String priv = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAqXjloUeQ9A9uUc6GqP29tLHs6euCk/tWs7MZQZnE5dA==";
        pub = pub.substring(0, pub.length() - accountId.length() - 3) + accountId + pub.substring(pub.length() - 3);
        priv = priv.substring(0, priv.length() - accountId.length() - 3) + accountId + priv.substring(priv.length() - 3);
        try {
            this.publicKey = StringUtil.getPublicKeyFromString(pub.replace(" ", "+"));
            this.privateKey = StringUtil.getPrivateKeyFromString(priv.replace(" ", "+"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
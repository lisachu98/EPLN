package blockchain.service;

import blockchain.config.PrintStompSessionHandler;
import blockchain.config.WebSocketClient;
import blockchain.model.Block;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

import blockchain.model.Transaction;
import blockchain.model.Wallet;
import blockchain.util.StringUtil;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BlockchainService {
    private String name;
    private ArrayList<Block> blockchain, centralchain;
    private ArrayList<Wallet> wallets;
    private Wallet wallet;
    private ArrayList<Transaction> mempool;
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private Environment environment;
    private Map<String, WebSocketClient> webSocketClients = new HashMap<>();
    private Set<String> nodes;

    @PostConstruct
    public void initializeBlockchain() {
        this.blockchain = new ArrayList<>();
        this.wallets = new ArrayList<>();
        createGenesisBlock();
        this.mempool = new ArrayList<>();
        this.name = environment.getProperty("spring.application.name");
        this.nodes = new HashSet<>();
        this.wallet = new Wallet(this.name.toLowerCase(), 1);
        this.centralchain = new ArrayList<>();
        Block genesis = new Block();
        centralchain.add(genesis);
        Wallet wallet1 = new Wallet("Max " + this.name.toLowerCase(), 1);
        Wallet wallet2 = new Wallet("Pawel " + this.name.toLowerCase(), 1);
        Wallet wallet3 = new Wallet("Jarek " + this.name.toLowerCase(), 1);
        wallets.add(wallet1);
        wallets.add(wallet2);
        wallets.add(wallet3);
        System.out.println(this.name);
        System.out.println("Wallet 1 pub: " + StringUtil.getStringFromKey(this.wallet.getPublicKey()) + " pri: " + StringUtil.getStringFromKey(wallet1.getPrivateKey()));
        for (Wallet wallet : wallets) {
            System.out.println("Wallet pub: " + StringUtil.getStringFromKey(wallet.getPublicKey()) + " pri: " + StringUtil.getStringFromKey(wallet.getPrivateKey()));
        }
        System.out.println("Blockchain initialized");
    }

    private void createGenesisBlock() {
        Block genesis = new Block();
        blockchain.add(genesis);
    }

    public synchronized void proofOfAuthority(){
        long seed = blockchain.get(blockchain.size() - 1).getHash().hashCode();
        Random random = new Random(seed);
        Set<String> nodesTmp = nodes;
        nodesTmp.remove("CENTRALBANK");
        List<String> nodesList = new ArrayList<>(nodesTmp);
        Collections.sort(nodesList);
        int randomInt = random.nextInt(nodesList.size());
        if (nodesList.get(randomInt).equalsIgnoreCase(name)) {
            System.out.println("I'm the validator!");
            Block block = mintBlock();
            String blockJson = StringUtil.getJson(block);
            template.convertAndSend("/topic/blocks", blockJson);
        }
        else System.out.println("Time for a new block!");
    }

    public synchronized Block mintBlock() {
        Block block = new Block(blockchain.get(blockchain.size() - 1).getHash());
        ArrayList<Transaction> blockTransactions = new ArrayList<>(mempool);
        block.setTransactions(blockTransactions);
        block.mineBlock();
        return block;
    }

    public int getMempool() {
        return mempool.size();
    }
    public ArrayList<Transaction> getMempoolTransactions() {
        return mempool;
    }

    public synchronized void addBlock(Block block) {
        blockchain.add(block);
        System.out.println("Block added and blockchain is valid: " + isChainValid());
    }

    public void addCentralBlock(Block block) {
        centralchain.add(block);
        System.out.println("Central block added and central blockchain is valid: " + isCentralChainValid());
    }

    public synchronized void addTransaction(Transaction transaction) {
        PublicKey receiver = null;
        try {
            receiver = StringUtil.getPublicKeyFromString(transaction.getReceiver());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        if(!transaction.processTransaction()) {
//            System.out.println("Transaction failed to process. Discarded.");
//            return;
//        }
        Wallet receiverWallet = findWalletByPublicKey(receiver);
        if (receiverWallet != null) {
            receiverWallet.credit(transaction.getAmount());
            System.out.println("User " + StringUtil.getStringFromKey(receiverWallet.getPublicKey()) + " received " + transaction.getAmount() + " coins");
        }
        mempool.add(transaction);
        System.out.println("Transaction added");
        if(mempool.size() == 100){
            proofOfAuthority();
            mempool.clear();
        }
    }

    public void addCentralTransaction(Transaction transaction) {
        if (transaction.getReceiver().equalsIgnoreCase(name)) {
            wallet.credit(transaction.getAmount());
            System.out.println("Got " + transaction.getAmount() + " coins from " + transaction.getSender());
        }
    }

    public boolean isChainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                return false;
            }
        }
        return true;
    }

    public boolean isCentralChainValid() {
        for (int i = 1; i < centralchain.size(); i++) {
            Block currentBlock = centralchain.get(i);
            Block previousBlock = centralchain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                return false;
            }
        }
        return true;
    }

    public boolean areBalancesValid() {
        for (Wallet wallet : wallets) {
            float balance = 0;
            for (Block block : blockchain) {
                for (Transaction transaction : block.getTransactions()) {
                    if (transaction.getSender().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                        balance -= transaction.getAmount();
                    }
                    if (transaction.getReceiver().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                        balance += transaction.getAmount();
                    }
                }
            }
            for (Block block : centralchain) {
                for (Transaction transaction : block.getTransactions()) {
                    if (transaction.getSender().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                        balance -= transaction.getAmount();
                    }
                    if (transaction.getReceiver().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                        balance += transaction.getAmount();
                    }
                }
            }
            for (Transaction transaction : mempool) {
                if (transaction.getSender().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                    balance -= transaction.getAmount();
                }
                if (transaction.getReceiver().equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
                    balance += transaction.getAmount();
                }
            }
            if (balance != wallet.getBalance()) {
                return false;
            }
        }

        return true;
    }

    public Wallet findWalletByPublicKey(PublicKey publicKey) {
        if (this.wallet.getPublicKey().equals(publicKey)) {
            return this.wallet;
        }

        return wallets.stream()
                .filter(wallet -> publicKey.equals(wallet.getPublicKey()))
                .findFirst()
                .orElse(null);
    }

    public synchronized Transaction sendFunds(String senderPubStr, String senderPriStr, String receiverStr, float amount) {
        PublicKey receiver;
        PublicKey senderPub;
        PrivateKey senderPri;
        try {
            receiver = StringUtil.getPublicKeyFromString(receiverStr);
            senderPub = StringUtil.getPublicKeyFromString(senderPubStr);
            senderPri = StringUtil.getPrivateKeyFromString(senderPriStr);
        } catch (Exception e) {
            System.out.println("Error processing keys: " + e.getMessage());
            return null;
        }
        Wallet senderWallet = findWalletByPublicKey(senderPub);
        if (senderWallet == null) {
            System.out.println("Wallet not found");
            return null;
        }
        if (senderWallet.getBalance() < amount) {
            System.out.println("Insufficient funds");
            return null;
        }

        Transaction transaction = new Transaction(senderPubStr, receiverStr, amount);
        transaction.generateSignature(senderPri);

        boolean debitSuccessful = senderWallet.debit(amount);

        if (!debitSuccessful) {
            return null;
        }
        String transactionJson = StringUtil.getJson(transaction);
        template.convertAndSend("/topic/transactions", transactionJson);
        System.out.println("Transaction sent to " + StringUtil.getStringFromKey(receiver) + " from " + StringUtil.getStringFromKey(senderPub) + " for " + amount + " coins");
        return transaction;
    }

    public synchronized Transaction sendBankFunds(String senderPubStr, String senderPriStr, String receiverStr, float amount) {
        Wallet senderWallet;
        if (senderPubStr.equals(StringUtil.getStringFromKey(wallet.getPublicKey()))) {
            senderWallet = wallet;
            if (senderWallet.getBalance() < amount) {
                System.out.println("Insufficient funds");
                return null;
            }
        }else return null;

        Transaction transaction = new Transaction(senderPubStr, receiverStr, amount);

        boolean debitSuccessful = senderWallet.debit(amount);

        if (!debitSuccessful) {
            return null;
        }
        String transactionJson = StringUtil.getJson(transaction);
        template.convertAndSend("/topic/centraltransactions", transactionJson);
        System.out.println("Bank transaction sent to " + receiverStr + " from " + senderPubStr + " for " + amount + " coins");
        return transaction;
    }

    public void sendFundsTest(int n) {
        String pub = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEbbkHdn3quLw1SRoJfWA27hKglgnO52LWHsW0SIRxfgdJXfVwibqSFsj9DolwdqeAZ+dq+wND2chvEcgbZ7/pQ==";
        String priv = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAqXjloUeQ9A9uUc6GqP29tLHs6euCk/tWs7MZQZnE5dA==";
        String[] names = {"Max", "Pawel", "Jarek"};
        Random random = new Random();
        for (int i = 0; i < 500; i++) {
            String name = names[random.nextInt(names.length)] + " " + this.name.toLowerCase();
            String pubSend = pub.substring(0, pub.length() - this.name.length() - 3) + this.name.toLowerCase() + pub.substring(pub.length() - 3);
            String privSend = priv.substring(0, priv.length() - this.name.length() - 3) + this.name.toLowerCase() + priv.substring(priv.length() - 3);
            String pubRec = pub.substring(0, pub.length() - name.length() - 3) + name + pub.substring(pub.length() - 3);
            pubSend = pubSend.replace(" ", "+");
            privSend = privSend.replace(" ", "+");
            pubRec = pubRec.replace(" ", "+");
            sendFunds(pubSend, privSend, pubRec, random.nextInt(1000) + 1);
            try {
                Thread.sleep(random.nextInt(100)+ 50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        int personalCount = 0;
        int bankCount = 0;
        Set<String> nodesTmp = new HashSet<>(nodes);
        nodesTmp.remove("CENTRALBANK");
        ArrayList<String> nodes = new ArrayList<>(nodesTmp);
        Set<String> namesSet = new HashSet<>(Arrays.asList(names));
        for (int i = 0; i < n; i++) {
            int chance = random.nextInt(100);
            if (chance<10){
                String name = nodes.get(random.nextInt(nodes.size()));
                String pubSend = pub.substring(0, pub.length() - this.name.length() - 3) + this.name.toLowerCase() + pub.substring(pub.length() - 3);
                String privSend = priv.substring(0, priv.length() - this.name.length() - 3) + this.name.toLowerCase() + priv.substring(priv.length() - 3);
                String pubRec = pub.substring(0, pub.length() - name.length() - 3) + name.toLowerCase() + pub.substring(pub.length() - 3);
                pubSend = pubSend.replace(" ", "+");
                privSend = privSend.replace(" ", "+");
                pubRec = pubRec.replace(" ", "+");
                sendBankFunds(pubSend, privSend, pubRec, random.nextInt(100) + 1);
                bankCount++;
            }
            else {
                String nameSend;
                String nameRec;
                do {
                    nameSend = names[random.nextInt(names.length)] + " " + this.name.toLowerCase();
                    nameRec = names[random.nextInt(names.length)] + " " + nodes.get(random.nextInt(nodes.size())).toLowerCase();
                }while (nameSend.equals(nameRec));
                String pubSend = pub.substring(0, pub.length() - nameSend.length() - 3) + nameSend + pub.substring(pub.length() - 3);
                String privSend = priv.substring(0, priv.length() - nameSend.length() - 3) + nameSend + priv.substring(priv.length() - 3);
                String pubRec = pub.substring(0, pub.length() - nameRec.length() - 3) + nameRec + pub.substring(pub.length() - 3);
                pubSend = pubSend.replace(" ", "+");
                privSend = privSend.replace(" ", "+");
                pubRec = pubRec.replace(" ", "+");
                sendFunds(pubSend, privSend, pubRec, random.nextInt(10) + 1);
                personalCount++;
            }
            try {
                Thread.sleep(random.nextInt(100)+ 50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Sent " + n + " transactions: " + personalCount + " personal and " + bankCount + " bank");
        System.out.println("Are balances valid: " + areBalancesValid());
        System.out.println("Blockchain is valid: " + isChainValid());
    }

    public void writeTransactionsToCSV() {
        System.out.println("Writing transactions to CSV file");
        try (FileWriter csvWriter = new FileWriter("blockchain.csv")) {
            csvWriter.append("Transaction ID,Sender,Receiver,Amount\n");

            List<Transaction> allTransactions = new ArrayList<>();

            for (Block block : centralchain) {
                allTransactions.addAll(block.getTransactions());
            }

            for (Block block : blockchain) {
                allTransactions.addAll(block.getTransactions());
            }

            allTransactions.sort(Comparator.comparing(Transaction::getTimestamp));

            for (Transaction transaction : allTransactions) {
                csvWriter.append(transaction.getTransactionId())
                        .append(',')
                        .append(transaction.getSender())
                        .append(',')
                        .append(transaction.getReceiver())
                        .append(',')
                        .append(Float.toString(transaction.getAmount()))
                        .append('\n');
            }

            csvWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to CSV file", e);
        }
    }

    public boolean authenticate(String publicKey, String privateKey) {
        PublicKey pubKey;
        PrivateKey priKey;
        try {
            pubKey = StringUtil.getPublicKeyFromString(publicKey);
            priKey = StringUtil.getPrivateKeyFromString(privateKey);
        } catch (Exception e) {
            System.out.println("Error processing keys: " + e.getMessage());
            return false;
        }
        for (Wallet wallet : wallets) {
            if (wallet.getPublicKey().equals(pubKey) && wallet.getPrivateKey().equals(priKey)) {
                return true;
            }
        }
        return false;
    }

    public void connectToNode(String nodeUrl) {
        if (webSocketClients.containsKey(nodeUrl)) {
            //System.out.println("Already connected to " + nodeUrl);
            return;
        }
        WebSocketClient webSocketClient = new WebSocketClient(nodeUrl, new PrintStompSessionHandler(this));
        webSocketClients.put(nodeUrl, webSocketClient);
    }
    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void discoverAndConnectToNodes(){
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                nodes.add(instance.getServiceId());
                String nodeUrl = "ws://localhost:"+Integer.toString(instance.getUri().getPort())+"/blockchain";
                connectToNode(nodeUrl);
            }
        }
    }
    public String getName() {
        return name;
    }
    public float getWalletBalance(String publicKey) {
        PublicKey pubKey;
        try {
            pubKey = StringUtil.getPublicKeyFromString(publicKey);
        } catch (Exception e) {
            System.out.println("Error processing keys: " + e.getMessage());
            return 0;
        }
        Wallet wallet = findWalletByPublicKey(pubKey);
        if (wallet == null) {
            return 0;
        }
        return wallet.getBalance();
    }
    public String getPublicKey() {
        return wallet.getAccountId();
    }
    public float getBalance() {
        return wallet.getBalance();
    }
    public ArrayList<Block> getBlockchain() {
        return blockchain;
    }
    public String getAccountId(String publicKey) {
        PublicKey pubKey;
        try {
            pubKey = StringUtil.getPublicKeyFromString(publicKey);
        } catch (Exception e) {
            System.out.println("Error processing keys: " + e.getMessage());
            return null;
        }
        Wallet wallet = findWalletByPublicKey(pubKey);
        if (wallet == null) {
            return null;
        }
        return wallet.getAccountId();
    }
}

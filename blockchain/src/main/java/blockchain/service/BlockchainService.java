package blockchain.service;

import blockchain.config.PrintStompSessionHandler;
import blockchain.config.WebSocketClient;
import blockchain.model.Block;

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
    private int port;
    private ArrayList<Block> blockchain;
    private ArrayList<Wallet> wallets;
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
        //this.port = Integer.parseInt(Objects.requireNonNull(environment.getProperty("local.server.port")));

        Wallet wallet1 = new Wallet();
        wallet1.setBalance(100);
        wallets.add(wallet1);
        System.out.println("Wallet 1 pub: " + StringUtil.getStringFromKey(wallet1.getPublicKey()) + " pri: " + StringUtil.getStringFromKey(wallet1.getPrivateKey()));
        System.out.println("Blockchain initialized");
    }

    private void createGenesisBlock() {
        Block genesis = new Block();
        blockchain.add(genesis);
    }

    public void prooffOfAuthority(){
        long seed = blockchain.get(blockchain.size() - 1).getHash().hashCode();
        Random random = new Random(seed);
        int randomInt = random.nextInt(nodes.size());
        List<String> nodesList = new ArrayList<>(nodes);
        if (nodesList.get(randomInt).equalsIgnoreCase(name)) {
            System.out.println("I'm the validator!");
            Block block = mintBlock();
            String blockJson = StringUtil.getJson(block);
            template.convertAndSend("/topic/blocks", blockJson);
        }
        else System.out.println("Time for a new block!");
    }

    public Block mintBlock() {
        Block block = new Block(blockchain.get(blockchain.size() - 1).getHash());
        block.setTransactions(mempool);
        block.mineBlock();
        return block;
    }

    public int getMempool() {
        return mempool.size();
    }

    public void addBlock(Block block) {
        blockchain.add(block);
        System.out.println("Block added and blockchain is valid: " + isChainValid());
    }

    public void addTransaction(Transaction transaction) {
        PublicKey receiver = null;
        try {
            receiver = StringUtil.getPublicKeyFromString(transaction.getReceiver());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Wallet receiverWallet = findWalletByPublicKey(receiver);
        if (receiverWallet != null) {
            receiverWallet.credit(transaction.getAmount());
            System.out.println("User " + StringUtil.getStringFromKey(receiverWallet.getPublicKey()) + " received " + transaction.getAmount() + " coins");
        }
        mempool.add(transaction);
        System.out.println("Transaction added");
        if(mempool.size() == 5){
            prooffOfAuthority();
            mempool.clear();
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

    public Wallet findWalletByPublicKey(PublicKey publicKey) {
        return wallets.stream()
                .filter(wallet -> publicKey.equals(wallet.getPublicKey()))
                .findFirst()
                .orElse(null);
    }

    public Transaction sendFunds(String senderPubStr, String senderPriStr, String receiverStr, float amount) {
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
}

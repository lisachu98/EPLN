package centralbank.service;

import centralbank.config.PrintStompSessionHandler;
import centralbank.config.WebSocketClient;
import centralbank.model.Block;
import centralbank.model.Transaction;
import centralbank.model.Wallet;
import centralbank.util.StringUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

@Service
public class CentralBankService {
    private String name;
    private ArrayList<Block> blockchain;
    private ArrayList<String> banks;
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
        this.banks = new ArrayList<>();
        createGenesisBlock();
        this.mempool = new ArrayList<>();
        this.name = environment.getProperty("spring.application.name");
        this.nodes = new HashSet<>();
        this.wallet = new Wallet("centralbank");
        this.wallet.setBalance(1000);
        System.out.println("Central Bank initialized");
    }

    private void createGenesisBlock() {
        Block genesis = new Block();
        blockchain.add(genesis);
    }

    public void centralizedLedger(){
        System.out.println("Time for a new block in CB!");
        Block block = mintBlock();
        String blockJson = StringUtil.getJson(block);
        template.convertAndSend("/topic/centralblocks", blockJson);
        blockchain.add(block);
        System.out.println("Block added and blockchain is valid: " + isChainValid());
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

    public void addTransaction(Transaction transaction) {
        if (transaction.getReceiver().equalsIgnoreCase(this.name)) wallet.credit(transaction.getAmount());
        System.out.println("User " + transaction.getReceiver() + " received " + transaction.getAmount() + " coins" + " from " + transaction.getSender());
        mempool.add(transaction);
        System.out.println("Transaction added");
        if(mempool.size() == 12){
            centralizedLedger();
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

    public void issueFunds(float amount) {
        for (String bank : banks) {
            Transaction transaction = new Transaction("Central Bank", bank, amount);
            String transactionJson = StringUtil.getJson(transaction);
            template.convertAndSend("/topic/centraltransactions", transactionJson);
            this.wallet.debit(amount);
            System.out.println("Money issued to " + bank + " for " + amount + " coins");
        }
    }

    public void connectToNode(String nodeUrl, String nodeName) {
        if (webSocketClients.containsKey(nodeUrl)) {
            //System.out.println("Already connected to " + nodeUrl);
            return;
        }
        banks.add(nodeName);
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
                connectToNode(nodeUrl, instance.getServiceId().toLowerCase());
            }
        }
    }
}

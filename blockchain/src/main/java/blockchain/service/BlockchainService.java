package blockchain.service;

import blockchain.config.PrintStompSessionHandler;
import blockchain.config.WebSocketClient;
import blockchain.model.Block;

import java.util.ArrayList;
import java.util.List;

import blockchain.model.Transaction;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class BlockchainService {
    private List<Block> blockchain;
    private List<WalletService> wallets;
    private List<Transaction> mempool;

    @PostConstruct
    public void initializeBlockchain() {
        new WebSocketClient("ws://localhost:8080/blockchain", new PrintStompSessionHandler());
        this.blockchain = new ArrayList<>();
        createGenesisBlock();
        System.out.println("Blockchain initialized");
    }

    private void createGenesisBlock() {
        Block genesis = new Block("0");
        genesis.mineBlock(4);
        blockchain.add(genesis);
    }

    public void addBlock() {
        Block block = new Block(blockchain.get(blockchain.size() - 1).getHash());
        block.mineBlock(4);
        blockchain.add(block);
        System.out.println("Block added");
    }

    public void addTransaction(Transaction transaction) {
        mempool.add(transaction);
        System.out.println("Transaction added");
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
}

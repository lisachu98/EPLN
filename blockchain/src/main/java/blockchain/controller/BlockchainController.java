package blockchain.controller;

import blockchain.model.Transaction;
import blockchain.service.BlockchainService;
import blockchain.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/blockchain")
public class BlockchainController {
    @Autowired
    private BlockchainService blockchainService;

    @MessageMapping("/addBlock")
    @SendTo("/topic/blockAdded")
    public ResponseEntity<String> addBlock() {
        blockchainService.addBlock();
        blockchainService.isChainValid();
        System.out.println("Blockchain is valid: " + blockchainService.isChainValid());
        return ResponseEntity.ok("Block added");
    }

    @MessageMapping("/transactions")
    public ResponseEntity<String> handleTransaction(String transactionJson) {
        Transaction transaction = StringUtil.getObjectFromJson(transactionJson, Transaction.class);
        transaction.verifySignature();
        blockchainService.addTransaction(transaction);

        return ResponseEntity.ok("Transaction added to mempool");
    }
}

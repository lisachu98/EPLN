package blockchain.controller;

import blockchain.model.Transaction;
import blockchain.service.BlockchainService;
import blockchain.util.StringUtil;
import dtos.SendFunds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/blockchain")
public class BlockchainController {
    @Autowired
    private BlockchainService blockchainService;

//    @MessageMapping("/addBlock")
//    @SendTo("/topic/blockAdded")
//    public ResponseEntity<String> addBlock() {
//        blockchainService.addBlock();
//        blockchainService.isChainValid();
//        System.out.println("Blockchain is valid: " + blockchainService.isChainValid());
//        return ResponseEntity.ok("Block added");
//    }

    @PostMapping("/sendFunds")
    public ResponseEntity<String> sendFunds(@RequestBody SendFunds request) {
        blockchainService.sendFunds(request.getSender(), request.getSenderPrivateKey(), request.getReceiver(), request.getAmount());
        return ResponseEntity.ok("Funds sent");
    }

    @GetMapping("/getMempool")
    public ResponseEntity<String> getMempool() {
        return ResponseEntity.ok("Mempool size: " + blockchainService.getMempool());
    }

    @MessageMapping("/transactions")
    public ResponseEntity<String> handleTransaction(Message<?> transactionJson) {
        Transaction transaction = StringUtil.getObjectFromJson((String) transactionJson.getPayload(), Transaction.class);
        transaction.verifySignature();
        blockchainService.addTransaction(transaction);
        System.out.println("Transaction added to mempool");

        return ResponseEntity.ok("Transaction added to mempool");
    }
}

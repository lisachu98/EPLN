package blockchain.controller;

import blockchain.model.Block;
import blockchain.model.Transaction;
import blockchain.service.BlockchainService;
import blockchain.util.StringUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
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

//    @PostMapping("/blockchain/sendFunds")
//    public ResponseEntity<String> sendFunds(@RequestBody SendFunds request) {
//        blockchainService.sendFunds(request.getSender(), request.getSenderPrivateKey(), request.getReceiver(), request.getAmount());
//        return ResponseEntity.ok("Funds sent");
//    }

    @PostMapping("/blockchain/sendFunds")
    public String sendFunds(@RequestParam("senderPublicKey") String senderPublicKey, @RequestParam("receiverPublicKey") String receiverPublicKey, @RequestParam("privateKey") String privateKey, @RequestParam("amount") float amount) {
        blockchainService.sendFunds(senderPublicKey, privateKey, receiverPublicKey, amount);
        return "redirect:/account";
    }

    @PostMapping("/blockchain/CSVExport")
    public String exportCSV() {
        blockchainService.writeTransactionsToCSV();
        return "redirect:/admin";
    }

    @PostMapping("/blockchain/sendFundsTest")
    public String sendFunds(@RequestParam("testNumber") int n) {
        blockchainService.sendFundsTest(n);
        return "redirect:/admin";
    }

    @PostMapping("/blockchain/sendBankFunds")
    public String sendBankFunds(@RequestParam("senderPublicKey") String senderPublicKey, @RequestParam("receiverPublicKey") String receiverPublicKey, @RequestParam("privateKey") String privateKey, @RequestParam("amount") float amount) {
        blockchainService.sendBankFunds(senderPublicKey, privateKey, receiverPublicKey, amount);
        return "redirect:/admin";
    }

    @GetMapping("/blockchain/getMempool")
    public ResponseEntity<String> getMempool() {
        return ResponseEntity.ok("Mempool size: " + blockchainService.getMempool());
    }

    @MessageMapping("/blockchain/transactions")
    public ResponseEntity<String> handleTransaction(Message<?> transactionJson) {
        Transaction transaction = StringUtil.getObjectFromJson((String) transactionJson.getPayload(), Transaction.class);
        transaction.verifySignature();
        blockchainService.addTransaction(transaction);
        System.out.println("Transaction added to mempool");

        return ResponseEntity.ok("Transaction added to mempool");
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestParam("publicKey") String publicKey, @RequestParam("privateKey") String privateKey, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (publicKey.equals("admin") && privateKey.equals("admin")) {
            model.addAttribute("success", "Login successful.");
            session.setAttribute("publicKey", blockchainService.getPublicKey());
            return "redirect:/admin";
        }
        boolean isAuthenticated = blockchainService.authenticate(publicKey, privateKey);

        if (isAuthenticated) {
            model.addAttribute("success", "Login successful.");
            model.addAttribute("balance", blockchainService.getWalletBalance(publicKey));

            session.setAttribute("publicKey", publicKey);
            System.out.println("Redirecting to account page");
            return "redirect:/account";
        } else {
            System.out.println("Redirecting to login page");
            redirectAttributes.addFlashAttribute("error", "Wrong credentials. Please try again.");
            return "redirect:/";
        }
    }
    @GetMapping("/")
    public String login(Model model) {
        model.addAttribute("serviceName", blockchainService.getName());
        return "login";
    }
    @GetMapping("/account")
    public String account(Model model, HttpSession session) {
        model.addAttribute("publicKey", (String) session.getAttribute("publicKey"));
        model.addAttribute("accountId", blockchainService.getAccountId((String) session.getAttribute("publicKey")));
        model.addAttribute("balance", blockchainService.getWalletBalance((String) session.getAttribute("publicKey")));
        return "account";
    }

    @GetMapping("/admin")
    public String admin(Model model, HttpSession session) {
        model.addAttribute("publicKey", (String) session.getAttribute("publicKey"));
        model.addAttribute("balance", blockchainService.getBalance());
        ArrayList<Block> blockchain = blockchainService.getBlockchain();
        model.addAttribute("name", blockchainService.getName());
        model.addAttribute("blockchain", blockchain);
        model.addAttribute("mempoolSize", blockchainService.getMempool());
        model.addAttribute("mempool", blockchainService.getMempoolTransactions());
        return "admin";
    }
}

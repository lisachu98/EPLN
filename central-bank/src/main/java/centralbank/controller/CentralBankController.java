package centralbank.controller;

import centralbank.model.Block;
import centralbank.service.CentralBankService;
import dtos.SendFunds;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
public class CentralBankController {
    @Autowired
    private CentralBankService centralBankService;

//    @PostMapping("/blockchain/issueFunds")
//    public ResponseEntity<String> sendFunds(@RequestBody SendFunds request) {
//        centralBankService.issueFunds(request.getAmount());
//        return ResponseEntity.ok("Funds issued");
//    }
    @PostMapping("/blockchain/issueFunds")
    public String sendFunds(@RequestParam("amount") float amount, @RequestParam("banks") ArrayList<String> banks) {
        centralBankService.issueFunds(amount, banks);
        return "redirect:/admin";
    }

    @PostMapping("/blockchain/issueFundsTest")
    public String sendFundsTest(@RequestParam("testNumber") int n) {
        centralBankService.issueFundsTest(n);
        return "redirect:/admin";
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestParam("publicKey") String publicKey, @RequestParam("privateKey") String privateKey, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (publicKey.equals("admin") && privateKey.equals("admin")) {
            model.addAttribute("success", "Login successful.");
            return "redirect:/admin";
        }
        return "redirect:/";
    }

    @GetMapping("/")
    public String login(Model model) {
        return "login";
    }
    @GetMapping("/admin")
    public String admin(Model model, HttpSession session) {
        model.addAttribute("publicKey", (String) session.getAttribute("publicKey"));
        model.addAttribute("balance", centralBankService.getBalance());
        ArrayList<Block> blockchain = centralBankService.getBlockchain();
        model.addAttribute("blockchain", blockchain);
        model.addAttribute("mempoolSize", centralBankService.getMempool());
        model.addAttribute("mempool", centralBankService.getMempoolTransactions());
        model.addAttribute("banks", centralBankService.getBanks());
        System.out.println("Banks: " + centralBankService.getBanks());
        return "admin";
    }
}

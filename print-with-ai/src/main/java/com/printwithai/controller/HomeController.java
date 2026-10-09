package com.printwithai.controller;

import com.printwithai.service.KioskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final KioskService kioskService;

    public HomeController(KioskService kioskService) {
        this.kioskService = kioskService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("kiosks", kioskService.getAllKiosks());
        return "index";
    }
}

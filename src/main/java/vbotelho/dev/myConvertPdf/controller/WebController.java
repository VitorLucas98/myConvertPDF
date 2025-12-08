package vbotelho.dev.myConvertPdf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("maxFiles", 100);
        model.addAttribute("allowedFormats", "JPG, JPEG, PNG, GIF, BMP, TIFF");
        return "index";
    }
}

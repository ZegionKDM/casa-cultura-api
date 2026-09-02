package CasaCulturaAPI.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Casa Cultura API";
    }

    @GetMapping("/health")
    public String health() {
        return "Servidor funcionando correctamente";
    }

}
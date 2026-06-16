package pk.pn.pasir_parkovskyi_nikita.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class InfoController {

    @GetMapping("/api/info")
    public Map<String, String> getInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("appName", "Aplikacja Budżetowa");
        info.put("version", "1.0");
        info.put("message", "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!");
        return info;
    }
}

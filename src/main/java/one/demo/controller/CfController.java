package one.demo.controller;

import one.demo.TokenGenerator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/cf")
public class CfController {

    @PostMapping("/token")
    public String getToken(@RequestBody Map<String, Object> data) {
        return TokenGenerator.generateJwtToken(data.get("userId").toString());
    }

}

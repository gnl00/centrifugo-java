package one.demo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.util.StringUtils;

import java.util.Date;

public class TokenGenerator {

    // same with centrifugo/config.json token secret key
    /*
      "client": {
        "token": {
          "hmac_secret_key": "zC5UgLwga3MVv60WS0CEU7BN7C1pa1BCuiChtPkzNvee6yZkIkXNO7WiwMEhAjGmVUXsxCzkxnWJpTERkkQHcA"
        }
      }
    */
    private static final String SECRET = "zC5UgLwga3MVv60WS0CEU7BN7C1pa1BCuiChtPkzNvee6yZkIkXNO7WiwMEhAjGmVUXsxCzkxnWJpTERkkQHcA";

    public static String generateJwtToken(String sub) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer("centrifugo-java-demo")
                .withSubject(StringUtils.hasText(sub) ? sub : "anonymous")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 7)))
                .sign(algorithm);
    }
}

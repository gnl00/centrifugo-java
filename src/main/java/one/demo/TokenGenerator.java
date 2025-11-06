package one.demo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

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

    public static String generateJwtToken() {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        String token = JWT.create()
                .withIssuer("auth0")
                .withSubject("java-demo-1")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 7)))
                .sign(algorithm);
        return token;
    }
}

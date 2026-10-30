package quizrace.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import quizrace.api.user.*;

@Service
public class AutenticacaoService {
    private final UtilizadorRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;
    public AutenticacaoService(UtilizadorRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }
    public Contratos.Token login(Contratos.Login pedido) {
        var user = users.findByEmail(UtilizadorService.normalizarEmail(pedido.email()));
        boolean valida = pedido.palavraPasse().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(pedido.palavraPasse(), user.map(Utilizador::getPasswordHash).orElse(dummyHash));
        if (!valida || user.isEmpty() || !user.get().isActivo())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        var u = user.get();
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(Seguranca.ISSUER).subject(u.getId().toString())
            .issuedAt(now).expiresAt(now.plusSeconds(900)).claim("ver", u.getVersaoAuth()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Contratos.Token(token, "Bearer", 900, Contratos.Publico.de(u));
    }
}

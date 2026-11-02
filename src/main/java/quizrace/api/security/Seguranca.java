package quizrace.api.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import quizrace.api.user.UtilizadorRepository;

@Configuration
public class Seguranca {
    public static final String ISSUER = "quizrace";
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean SecretKeySpec chave(@Value("${quiz.security.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("QUIZ_JWT_SECRET precisa de pelo menos 32 bytes.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder encoder(SecretKeySpec chave) { return new NimbusJwtEncoder(new ImmutableSecret<>(chave)); }
    @Bean JwtDecoder decoder(SecretKeySpec chave) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UtilizadorRepository users) throws Exception {
        return http
            // Apenas Bearer no Authorization; não há credenciais em cookies nem sessão HTTP.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/testes", "/api/v1/testes/**").hasRole("DOCENTE")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint((req, res, ex) -> erro(res, 401, "NAO_AUTENTICADO"))
                .accessDeniedHandler((req, res, ex) -> erro(res, 403, "NAO_AUTORIZADO"))
                .jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                    try {
                        var u = users.findById(UUID.fromString(jwt.getSubject()))
                            .filter(x -> x.isActivo() && Long.valueOf(x.getVersaoAuth()).equals(jwt.getClaim("ver")))
                            .orElseThrow(() -> new IllegalArgumentException("Conta inválida"));
                        return new JwtAuthenticationToken(jwt,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name())), u.getId().toString());
                    } catch (IllegalArgumentException e) {
                        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
                    }
                })))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> erro(res, 401, "NAO_AUTENTICADO"))
                .accessDeniedHandler((req, res, ex) -> erro(res, 403, "NAO_AUTORIZADO")))
            .build();
    }
    private static void erro(jakarta.servlet.http.HttpServletResponse response, int status, String codigo)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensagem\":\"Acesso recusado.\"}");
    }
}

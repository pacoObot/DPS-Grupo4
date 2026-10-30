package quizrace.api.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilizador")
public class Utilizador {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String nome;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Perfil perfil;
    @Column(nullable = false) private boolean activo;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
    @Column(name = "actualizado_em", nullable = false) private Instant actualizadoEm;
    @Column(name = "versao_auth", nullable = false) private long versaoAuth;
    @Version private long versao;

    protected Utilizador() {}
    public Utilizador(String nome, String email, String passwordHash, Perfil perfil) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.email = email;
        this.passwordHash = passwordHash;
        this.perfil = perfil;
        this.activo = true;
        this.criadoEm = this.actualizadoEm = Instant.now();
    }
    public void alterar(Perfil perfil, Boolean activo) {
        if (perfil != null) this.perfil = perfil;
        if (activo != null) this.activo = activo;
        this.versaoAuth++;
        this.actualizadoEm = Instant.now();
    }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Perfil getPerfil() { return perfil; }
    public boolean isActivo() { return activo; }
    public Instant getCriadoEm() { return criadoEm; }
    public long getVersaoAuth() { return versaoAuth; }
}

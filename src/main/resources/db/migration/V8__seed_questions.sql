-- V8: dados iniciais (perguntas de demonstração)
-- O administrador admin/admin é criado pelo código (PasswordHasher) no arranque.
INSERT INTO questions (question_text, category, difficulty, option_a, option_b, option_c, option_d, correct_option, points, time_limit, active) VALUES
('Qual protocolo é orientado à conexão?', 'Redes', 'EASY', 'UDP', 'TCP', 'ICMP', 'ARP', 'B', 10, 15, TRUE),
('Qual linguagem corre sobre a JVM?', 'Java', 'EASY', 'Python', 'Java', 'C', 'PHP', 'B', 10, 15, TRUE),
('O que é uma Thread?', 'Concorrência', 'EASY', 'Um ficheiro', 'Um fluxo de execução', 'Um socket', 'Um IP', 'B', 10, 15, TRUE),
('Estrutura de dados thread-safe para mapas?', 'Concorrência', 'MEDIUM', 'HashMap', 'ArrayList', 'ConcurrentHashMap', 'TreeSet', 'C', 10, 15, TRUE),
('O servidor TCP espera novas conexões com:', 'Redes', 'EASY', 'accept()', 'read()', 'bind()', 'flush()', 'A', 10, 15, TRUE),
('CountDownLatch serve para:', 'Concorrência', 'MEDIUM', 'Ordenar listas', 'Esperar N eventos', 'Ler ficheiros', 'Comprimir dados', 'B', 10, 15, TRUE),
('Race condition ocorre quando:', 'Concorrência', 'MEDIUM', 'O servidor reinicia', 'Threads acedem dados sem sincronização', 'A rede falha', 'O disco enche', 'B', 10, 15, TRUE),
('SELECT ... FOR UPDATE serve para:', 'PostgreSQL', 'MEDIUM', 'Apagar linhas', 'Travar linhas contra escrita concorrente', 'Criar índices', 'Exportar dados', 'B', 10, 15, TRUE),
('Em Clean Architecture, o domain pode depender de:', 'Arquitetura', 'MEDIUM', 'JDBC', 'JavaFX', 'Sockets', 'Nenhuma dessas (é o núcleo puro)', 'D', 10, 15, TRUE),
('O ExecutorService faz parte de qual pacote Java?', 'Concorrência', 'EASY', 'java.util.concurrent', 'java.net', 'java.io', 'java.sql', 'A', 10, 15, TRUE),
('Flyway é utilizado para:', 'PostgreSQL', 'EASY', 'Criar interfaces', 'Gerir migrações da base de dados', 'Compilar Java', 'Gerir threads', 'B', 10, 15, TRUE),
('O cliente NUNCA deve enviar no protocolo:', 'Segurança', 'MEDIUM', 'A opção escolhida', 'O nome da equipa', 'A pontuação', 'O id da pergunta', 'C', 10, 15, TRUE);

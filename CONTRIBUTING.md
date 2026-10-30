# Colaboração

1. Trabalhar a partir do código existente; não alterar o protocolo TCP sem testes e contrato actualizado.
2. Usar Java 21. Executar `mvn clean verify` antes de propor alterações.
3. Para mudanças de persistência, acrescentar migrações Flyway versionadas. Não editar migrações já aplicadas numa instalação partilhada.
4. Manter HTTP/DTOs, serviços e acesso a dados com responsabilidades separadas.
5. Testar regras de autorização no backend. Não expor entidades JPA directamente em respostas públicas.
6. Não versionar segredos, `.env`, downloads, logs, dados de teste ou artefactos de build.
7. Actualizar contrato API, documentação de execução, testes e CHANGELOG com mudanças relevantes.
8. Executar `bash scripts/verificar-api.sh` quando alterar banco, bootstrap ou Docker.
9. Registar comandos e resultados observados; não marcar entregas não verificadas como concluídas.

# Plano de Implementação: Todo List Backend

## Visão Geral

Implementação incremental do backend RESTful de gerenciamento de tarefas em Java 17 com Spring Boot. A ordem das tarefas respeita as dependências entre camadas: configuração → modelo → persistência → negócio → apresentação → testes.

## Tarefas

- [x] 1. Configurar ambientes e DDL
  - [x] 1.1 Criar `src/main/resources/schema.sql` com o DDL PostgreSQL para a tabela `tarefa`
    - Usar `CREATE TABLE IF NOT EXISTS tarefa` com as colunas: `id BIGSERIAL PRIMARY KEY`, `nome VARCHAR(255) NOT NULL`, `descricao TEXT`, `status VARCHAR(20)`, `observacoes TEXT`, `data_criacao DATE`, `data_atualizacao DATE`
    - _Requirements: 5.3_

  - [x] 1.2 Atualizar `src/main/resources/application.properties` com a configuração de produção (PostgreSQL)
    - Definir `spring.datasource.url=jdbc:postgresql://localhost:5432/todo`, username `postgres`, password `postgres`
    - Definir `spring.jpa.hibernate.ddl-auto=validate` e `spring.sql.init.mode=never`
    - _Requirements: 5.1_

  - [x] 1.3 Criar `src/test/resources/application-test.properties` com a configuração H2 em memória
    - Definir `spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`, driver H2, dialect H2
    - Definir `spring.jpa.hibernate.ddl-auto=create-drop` e `spring.sql.init.mode=never`
    - _Requirements: 5.2, 5.4_

- [x] 2. Implementar o modelo de domínio
  - [x] 2.1 Criar `StatusTarefa.java` no pacote `com.fatec.todo.model`
    - Enum com as constantes `PENDENTE`, `EM_ANDAMENTO` e `CONCLUIDA`
    - _Requirements: 2.3, 4.1_

  - [x] 2.2 Criar `Tarefa.java` no pacote `com.fatec.todo.model`
    - Entidade JPA com `@Entity`, `@Table(name = "tarefa")`, anotações Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
    - Atributos: `id` (Long, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`), `nome` (String), `descricao` (String), `status` (`@Enumerated(EnumType.STRING)` StatusTarefa), `observacoes` (String), `dataCriacao` (LocalDate), `dataAtualizacao` (LocalDate)
    - _Requirements: 2.3, 2.4, 4.1, 4.2, 4.3_

- [x] 3. Implementar a camada de exceção e persistência
  - [x] 3.1 Criar `RegraNegocioException.java` no pacote `com.fatec.todo.exception`
    - Estende `RuntimeException`; construtor recebe `String mensagem` e chama `super(mensagem)`
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 Criar `TarefaRepository.java` no pacote `com.fatec.todo.repository`
    - Interface que estende `JpaRepository<Tarefa, Long>` sem métodos adicionais
    - _Requirements: 4.4_

- [x] 4. Implementar a interface e a implementação de serviço
  - [x] 4.1 Criar `TarefaService.java` no pacote `com.fatec.todo.service`
    - Interface com os métodos: `Tarefa salvar(Tarefa)`, `Tarefa atualizar(Long, Tarefa)`, `void deletar(Long)`, `Tarefa buscarPorId(Long)`, `List<Tarefa> listarTodas()`
    - _Requirements: 7.1_

  - [x] 4.2 Criar `TarefaServiceImpl.java` no pacote `com.fatec.todo.service.impl`
    - Implementa `TarefaService`; injeta `TarefaRepository` via construtor (sem `@Autowired`)
    - `salvar`: chama `validarNome`, define `dataCriacao = LocalDate.now()`, persiste via repository
    - `atualizar`: busca entidade existente, chama `validarNome`, copia `id` e `dataCriacao` do existente, define `dataAtualizacao = LocalDate.now()`, persiste
    - `deletar`: chama `buscarPorId` para garantir existência, depois chama `deleteById`
    - `buscarPorId`: retorna `findById` ou lança `RegraNegocioException("Tarefa não encontrada: " + id)`
    - `validarNome` (privado): lança `RegraNegocioException("O campo 'nome' é obrigatório.")` se `nome` for `null` ou `isBlank()`
    - Métodos de escrita anotados com `@Transactional` de `org.springframework.transaction.annotation`
    - _Requirements: 2.1, 3.1, 3.2, 3.3, 7.2, 7.4_

- [x] 5. Checkpoint — verificar compilação do projeto
  - Garantir que todos os arquivos criados até aqui compilem sem erros. Perguntar ao usuário se surgirem dúvidas.

- [x] 6. Implementar o controller REST
  - [x] 6.1 Criar `TarefaController.java` no pacote `com.fatec.todo.controller`
    - Anotado com `@RestController` e `@RequestMapping("/api/tarefas")`; injeta `TarefaService` via construtor (sem `@Autowired`)
    - `POST /` → `tarefaService.salvar`, retorna `ResponseEntity.status(201).body(...)`
    - `GET /` → `tarefaService.listarTodas()`, retorna `ResponseEntity.ok(...)`
    - `GET /{id}` → `tarefaService.buscarPorId(id)`, retorna `ResponseEntity.ok(...)`
    - `PUT /{id}` → `tarefaService.atualizar(id, tarefa)`, retorna `ResponseEntity.ok(...)`
    - `DELETE /{id}` → `tarefaService.deletar(id)`, retorna `ResponseEntity.noContent().build()`
    - `@ExceptionHandler(RegraNegocioException.class)` → retorna `ResponseEntity.unprocessableEntity().body(ex.getMessage())`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 7.1, 7.3_

- [x] 7. Adicionar dependência jqwik ao pom.xml
  - [x] 7.1 Adicionar `net.jqwik:jqwik:1.9.3` com `scope=test` em `pom.xml`
    - Necessário para os testes de propriedade nas classes de teste
    - _Requirements: 6.1, 6.2_

- [x] 8. Implementar testes de integração de repositório
  - [x] 8.1 Criar `TarefaRepositoryTest.java` em `src/test/java/com/fatec/todo/`
    - Anotar com `@DataJpaTest` e `@ActiveProfiles("test")`; injetar `TarefaRepository` via `@Autowired`
    - Smoke test: verificar que contexto carrega e repository não é nulo
    - _Requirements: 5.4, 6.1_

  - [ ]* 8.2 Escrever teste de propriedade — round-trip de persistência (Property 1)
    - **Property 1: Round-trip de Persistência**
    - Para qualquer nome não-branco (`@ForAll @StringLength(min=1, max=100) @AlphaChars`), salvar uma `Tarefa` e recuperar pelo `id` deve retornar a tarefa com o mesmo `nome`, `status` e `dataCriacao`
    - Usar `@Property(tries = 100)` e incluir comentário: `// Feature: todo-list-backend, Property 1: Round-trip de Persistência`
    - **Validates: Requirements 1.3, 2.4, 6.1, 6.3**

  - [ ]* 8.3 Escrever teste de exemplo — busca por id inexistente retorna `Optional.empty()`
    - Chamar `tarefaRepository.findById(Long.MAX_VALUE)` e verificar que o resultado é vazio
    - _Requirements: 1.6_

- [x] 9. Checkpoint — rodar testes de repositório
  - Executar `mvn test -Dtest=TarefaRepositoryTest` e garantir que todos passam. Perguntar ao usuário se surgirem dúvidas.

- [x] 10. Implementar testes unitários de serviço
  - [x] 10.1 Criar `TarefaServiceTest.java` em `src/test/java/com/fatec/todo/`
    - Anotar com `@ExtendWith(MockitoExtension.class)`; criar mock de `TarefaRepository` e instanciar `TarefaServiceImpl` via construtor
    - _Requirements: 6.2_

  - [ ]* 10.2 Escrever teste de propriedade — nome inválido lança exceção sem chamar repository (Property 7)
    - **Property 7: Nome Inválido Lança Exceção sem Acionar o Repository**
    - Para qualquer string nula ou em branco (usando `@Provide` com `Arbitraries.strings().withChars(' ','\t','\n','\r')` e `injectNull(0.1)`), chamar `salvar` e `atualizar` deve lançar `RegraNegocioException` sem nenhuma chamada a `tarefaRepository.save()`
    - Usar `@Property(tries = 100)` e incluir comentário: `// Feature: todo-list-backend, Property 7: Nome Inválido Lança Exceção sem Acionar o Repository`
    - **Validates: Requirements 2.1, 6.2, 6.4**

  - [ ]* 10.3 Escrever teste de propriedade — `dataCriacao` preenchida automaticamente na criação (Property 8)
    - **Property 8: `dataCriacao` Preenchida Automaticamente na Criação**
    - Para qualquer nome não-branco, ao chamar `tarefaService.salvar()` (com `tarefaRepository.save()` mockado para retornar a tarefa recebida), o campo `dataCriacao` do objeto salvo deve ser igual a `LocalDate.now()`
    - Usar `@Property(tries = 50)` e incluir comentário: `// Feature: todo-list-backend, Property 8: dataCriacao Preenchida Automaticamente na Criação`
    - **Validates: Requirement 3.1**

  - [ ]* 10.4 Escrever teste de propriedade — `dataAtualizacao` preenchida automaticamente na atualização (Property 9)
    - **Property 9: `dataAtualizacao` Preenchida Automaticamente na Atualização**
    - Para qualquer nome não-branco, ao chamar `tarefaService.atualizar()` (com `findById` mockado para retornar tarefa existente e `save` mockado para retornar a recebida), o campo `dataAtualizacao` do objeto retornado deve ser igual a `LocalDate.now()`
    - Usar `@Property(tries = 50)` e incluir comentário: `// Feature: todo-list-backend, Property 9: dataAtualizacao Preenchida Automaticamente na Atualização`
    - **Validates: Requirement 3.2**

  - [ ]* 10.5 Escrever teste de propriedade — `dataCriacao` preservada na atualização (Property 10)
    - **Property 10: `dataCriacao` Preservada durante Atualizações**
    - Para qualquer `dataCriacao` passada (ex.: `LocalDate.now().minusDays(N)`) ao criar a tarefa mockada, chamar `atualizar` deve retornar tarefa com o mesmo valor de `dataCriacao`
    - Usar `@Property(tries = 50)` e incluir comentário: `// Feature: todo-list-backend, Property 10: dataCriacao Preservada durante Atualizações`
    - **Validates: Requirement 3.3**

  - [ ]* 10.6 Escrever teste de exemplo — salvar nome válido chama `repository.save()`
    - Criar tarefa com nome válido, chamar `tarefaService.salvar()` e verificar com `verify(tarefaRepository).save(any())`
    - _Requirements: 6.1_

- [x] 11. Checkpoint final — rodar toda a suíte de testes
  - Executar `mvn test` e garantir que todos os testes passam. Perguntar ao usuário se surgirem dúvidas.

## Notas

- Tarefas marcadas com `*` são opcionais (testes de propriedade e exemplos) e podem ser puladas para uma MVP mais rápida
- Cada tarefa referencia os requisitos específicos do `requirements.md` para rastreabilidade
- Os testes de propriedade usam jqwik 1.9.3 com JUnit 5 (compatível com Spring Boot Test)
- Injeção de dependência exclusivamente via construtor em todos os componentes — sem `@Autowired`
- `@Transactional` deve ser importado de `org.springframework.transaction.annotation`
- Os checkpoints garantem validação incremental antes de avançar para a próxima camada

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2"] },
    { "id": 5, "tasks": ["6.1", "7.1"] },
    { "id": 6, "tasks": ["8.1"] },
    { "id": 7, "tasks": ["8.2", "8.3", "10.1"] },
    { "id": 8, "tasks": ["10.2", "10.3", "10.4", "10.5", "10.6"] }
  ]
}
```

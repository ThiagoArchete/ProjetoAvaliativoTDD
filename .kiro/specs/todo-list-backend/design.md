# Design Técnico — Todo List Backend

## Visão Geral

O sistema é um backend RESTful de gerenciamento de tarefas (To-Do) construído com **Java 17** e **Spring Boot 4.x**. Ele expõe uma API HTTP sobre o recurso `Tarefa`, permitindo criar, listar, buscar por ID, atualizar e remover tarefas. Não há autenticação de usuário. A persistência usa **PostgreSQL** em produção e **H2 em memória** durante os testes automatizados.

O objetivo deste documento é especificar a estrutura interna do sistema: camadas, componentes, contratos de interface, modelo de dados e propriedades de corretude que os testes devem verificar.

---

## Arquitetura

### Estilo Arquitetural

Arquitetura em camadas (*Layered Architecture*) com fluxo unidirecional de dependências:

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│   TarefaController  │  ← Camada de Apresentação (REST)
└─────────┬───────────┘
          │ usa
          ▼
┌─────────────────────┐
│   TarefaService     │  ← Contrato de negócio (interface)
│  (interface)        │
└─────────┬───────────┘
          │ implementado por
          ▼
┌─────────────────────┐
│  TarefaServiceImpl  │  ← Camada de Negócio
└─────────┬───────────┘
          │ usa
          ▼
┌─────────────────────┐
│  TarefaRepository   │  ← Camada de Persistência (JPA)
└─────────┬───────────┘
          │ persiste
          ▼
┌─────────────────────┐
│      Tarefa         │  ← Entidade JPA / Modelo de Domínio
└─────────────────────┘
```

### Pacotes

| Pacote | Conteúdo |
|--------|----------|
| `com.fatec.todo` | Classe principal `TodoApplication` |
| `com.fatec.todo.model` | Entidade `Tarefa` e enum `StatusTarefa` |
| `com.fatec.todo.repository` | Interface `TarefaRepository` |
| `com.fatec.todo.service` | Interface `TarefaService` |
| `com.fatec.todo.service.impl` | Classe `TarefaServiceImpl` |
| `com.fatec.todo.controller` | Classe `TarefaController` |
| `com.fatec.todo.exception` | Classe `RegraNegocioException` |

### Decisões de Design

- **Injeção via construtor**: Nenhum componente usa `@Autowired` em campo. Todos declaram suas dependências como parâmetros do construtor, tornando-as explícitas e facilitando testes unitários com mocks.
- **Interface de serviço**: `TarefaService` desacopla o controller da implementação concreta, permitindo substituição sem alterar o ponto de entrada HTTP.
- **`@Transactional` apenas na camada de serviço**: Operações de escrita são demarcadas em `TarefaServiceImpl`, mantendo o controller livre de preocupações transacionais.
- **`RegraNegocioException` como `RuntimeException`**: Permite que o Spring propague a exceção automaticamente até o `@ExceptionHandler` no controller, sem `throws` obrigatórios.
- **Enum persistido como `STRING`**: Garante legibilidade na coluna de banco e tolerância a reordenação de constantes.

---

## Componentes e Interfaces

### 1. `StatusTarefa` (enum)

**Pacote:** `com.fatec.todo.model`

```java
public enum StatusTarefa {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA
}
```

---

### 2. `Tarefa` (entidade JPA)

**Pacote:** `com.fatec.todo.model`

```java
@Entity
@Table(name = "tarefa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusTarefa status;

    private String observacoes;
    private LocalDate dataCriacao;
    private LocalDate dataAtualizacao;
}
```

---

### 3. `RegraNegocioException` (exceção customizada)

**Pacote:** `com.fatec.todo.exception`

```java
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
```

---

### 4. `TarefaRepository` (repositório JPA)

**Pacote:** `com.fatec.todo.repository`

```java
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
}
```

Herda de `JpaRepository<Tarefa, Long>` os métodos:
- `save(Tarefa)` → persiste ou atualiza
- `findById(Long)` → retorna `Optional<Tarefa>`
- `findAll()` → retorna `List<Tarefa>`
- `deleteById(Long)` → remove pelo ID

---

### 5. `TarefaService` (interface de serviço)

**Pacote:** `com.fatec.todo.service`

```java
public interface TarefaService {

    Tarefa salvar(Tarefa tarefa);

    Tarefa atualizar(Long id, Tarefa tarefa);

    void deletar(Long id);

    Tarefa buscarPorId(Long id);

    List<Tarefa> listarTodas();
}
```

---

### 6. `TarefaServiceImpl` (implementação de negócio)

**Pacote:** `com.fatec.todo.service.impl`

```java
@Service
public class TarefaServiceImpl implements TarefaService {

    private final TarefaRepository tarefaRepository;

    public TarefaServiceImpl(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    @Override
    @Transactional
    public Tarefa salvar(Tarefa tarefa) {
        validarNome(tarefa.getNome());
        tarefa.setDataCriacao(LocalDate.now());
        return tarefaRepository.save(tarefa);
    }

    @Override
    @Transactional
    public Tarefa atualizar(Long id, Tarefa tarefa) {
        Tarefa existente = buscarPorId(id);
        validarNome(tarefa.getNome());
        tarefa.setId(id);
        tarefa.setDataCriacao(existente.getDataCriacao());   // preserva dataCriacao
        tarefa.setDataAtualizacao(LocalDate.now());
        return tarefaRepository.save(tarefa);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        buscarPorId(id);   // lança 404 se não existir
        tarefaRepository.deleteById(id);
    }

    @Override
    public Tarefa buscarPorId(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Tarefa não encontrada: " + id));
    }

    @Override
    public List<Tarefa> listarTodas() {
        return tarefaRepository.findAll();
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O campo 'nome' é obrigatório.");
        }
    }
}
```

**Algoritmos relevantes:**

- **`salvar`**: valida `nome` → define `dataCriacao = LocalDate.now()` → persiste via repository.
- **`atualizar`**: busca entidade existente (falha rápido se não existe) → valida `nome` → copia `id` e `dataCriacao` do objeto existente → define `dataAtualizacao = LocalDate.now()` → persiste.
- **`deletar`**: garante existência com `buscarPorId` antes de deletar, para lançar 404 quando necessário.
- **`validarNome`**: rejeita `null` e strings compostas apenas por espaços em branco (`isBlank()`).

---

### 7. `TarefaController` (controlador REST)

**Pacote:** `com.fatec.todo.controller`

```java
@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    public ResponseEntity<Tarefa> criar(@RequestBody Tarefa tarefa) {
        Tarefa criada = tarefaService.salvar(tarefa);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<Tarefa>> listar() {
        return ResponseEntity.ok(tarefaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tarefa> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(tarefaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tarefa> atualizar(@PathVariable Long id,
                                             @RequestBody Tarefa tarefa) {
        return ResponseEntity.ok(tarefaService.atualizar(id, tarefa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        tarefaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<String> handleRegraNegocio(RegraNegocioException ex) {
        return ResponseEntity.unprocessableEntity().body(ex.getMessage());
    }
}
```

---

## Modelos de Dados

### Entidade `Tarefa`

| Atributo | Tipo Java | Coluna SQL | Restrições |
|----------|-----------|------------|------------|
| `id` | `Long` | `id BIGSERIAL PRIMARY KEY` | Gerado automaticamente, não nulo |
| `nome` | `String` | `nome VARCHAR(255)` | Obrigatório (validado na camada de serviço) |
| `descricao` | `String` | `descricao TEXT` | Opcional |
| `status` | `StatusTarefa` | `status VARCHAR(20)` | Valores: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA` |
| `observacoes` | `String` | `observacoes TEXT` | Opcional |
| `dataCriacao` | `LocalDate` | `data_criacao DATE` | Preenchido na criação |
| `dataAtualizacao` | `LocalDate` | `data_atualizacao DATE` | Preenchido na atualização |

### DDL PostgreSQL (`schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS tarefa (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    descricao        TEXT,
    status           VARCHAR(20),
    observacoes      TEXT,
    data_criacao     DATE,
    data_atualizacao DATE
);
```

### Mapeamento de Convenção de Nomes

Spring Boot com JPA usa `spring.jpa.hibernate.ddl-auto` e respeita o `schema.sql` quando `spring.sql.init.mode=always`. Os campos `dataCriacao` e `dataAtualizacao` são mapeados para `data_criacao` e `data_atualizacao` por convenção de *snake_case* do Hibernate (`spring.jpa.hibernate.naming.physical-strategy`).

### Enum `StatusTarefa` — Persistência

```
Java: StatusTarefa.PENDENTE     → SQL: 'PENDENTE'
Java: StatusTarefa.EM_ANDAMENTO → SQL: 'EM_ANDAMENTO'
Java: StatusTarefa.CONCLUIDA    → SQL: 'CONCLUIDA'
```

### Configurações de Ambiente

**`application.properties` (produção):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/todo
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=never
```

**`application-test.properties` (testes):**
```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=never
```

---


## Propriedades de Corretude

*Uma propriedade é uma característica ou comportamento que deve ser verdadeiro em todas as execuções válidas do sistema — essencialmente, uma afirmação formal sobre o que o sistema deve fazer. As propriedades servem como ponte entre especificações legíveis por humanos e garantias de corretude verificáveis por máquina.*

---

### Propriedade 1: Round-trip de Persistência

*Para qualquer* `Tarefa` válida (nome não-nulo, não-branco, com qualquer `StatusTarefa`), após persisti-la via `TarefaRepository.save()`, buscar pelo `id` retornado com `findById()` deve produzir uma tarefa com os mesmos `nome`, `descricao`, `status`, `observacoes` e `dataCriacao`. O campo `status` deve ser recuperado como o mesmo valor do enum (round-trip via `@Enumerated(EnumType.STRING)`).

**Valida: Requisitos 1.3, 2.4, 6.1, 6.3**

---

### Propriedade 2: Criação via POST retorna HTTP 201 com Recurso Criado

*Para qualquer* `Tarefa` válida enviada como corpo JSON em `POST /api/tarefas`, a resposta deve ter status HTTP 201 e o corpo deve conter os campos da tarefa com um `id` gerado.

**Valida: Requisito 1.1**

---

### Propriedade 3: Listagem retorna Todas as Tarefas Persistidas

*Para qualquer* conjunto de `N` tarefas válidas persistidas no banco, `GET /api/tarefas` deve retornar uma lista com exatamente `N` elementos contendo todas as tarefas cadastradas.

**Valida: Requisito 1.2**

---

### Propriedade 4: Atualização Reflete os Novos Dados

*Para qualquer* tarefa existente e quaisquer novos dados válidos (nome não-branco), `PUT /api/tarefas/{id}` deve retornar HTTP 200 e o corpo da resposta deve refletir os novos valores enviados.

**Valida: Requisito 1.4**

---

### Propriedade 5: Deleção Remove a Tarefa (Round-trip Negativo)

*Para qualquer* tarefa existente, `DELETE /api/tarefas/{id}` deve retornar HTTP 204 e um subsequente `GET /api/tarefas/{id}` deve retornar HTTP 404, confirmando a remoção.

**Valida: Requisito 1.5**

---

### Propriedade 6: ID Inexistente Retorna HTTP 404

*Para qualquer* `id` que não corresponda a nenhuma tarefa cadastrada, operações de `GET`, `PUT` ou `DELETE` em `/api/tarefas/{id}` devem retornar HTTP 404.

**Valida: Requisito 1.6**

---

### Propriedade 7: Nome Inválido Lança Exceção sem Acionar o Repository

*Para qualquer* string nula ou composta exclusivamente de caracteres em branco usada como `nome`, `TarefaServiceImpl.salvar()` e `TarefaServiceImpl.atualizar()` devem lançar `RegraNegocioException` com mensagem descritiva **sem realizar nenhuma chamada** ao `TarefaRepository`.

**Valida: Requisitos 2.1, 6.2, 6.4**

---

### Propriedade 8: `dataCriacao` Preenchida Automaticamente na Criação

*Para qualquer* `Tarefa` válida criada via `TarefaServiceImpl.salvar()`, o campo `dataCriacao` da tarefa persistida deve ser igual a `LocalDate.now()` no momento da chamada.

**Valida: Requisito 3.1**

---

### Propriedade 9: `dataAtualizacao` Preenchida Automaticamente na Atualização

*Para qualquer* tarefa existente atualizada via `TarefaServiceImpl.atualizar()`, o campo `dataAtualizacao` da tarefa retornada deve ser igual a `LocalDate.now()` no momento da chamada.

**Valida: Requisito 3.2**

---

### Propriedade 10: `dataCriacao` Preservada durante Atualizações

*Para qualquer* tarefa criada e posteriormente atualizada via `TarefaServiceImpl.atualizar()`, o campo `dataCriacao` deve permanecer com o valor definido no momento da criação, independentemente do número de atualizações subsequentes.

**Valida: Requisito 3.3**

---

## Tratamento de Erros

### Estratégia Geral

O sistema adota uma abordagem de **fail-fast na camada de serviço**: as regras de negócio são verificadas o mais cedo possível, antes de qualquer interação com o banco de dados. A propagação de erros segue o fluxo:

```
TarefaServiceImpl (lança exceção)
       │
       ▼
TarefaController (@ExceptionHandler captura)
       │
       ▼
Resposta HTTP com status e mensagem adequados
```

### Mapeamento de Exceções para HTTP

| Situação | Exceção | Status HTTP | Corpo da Resposta |
|----------|---------|-------------|-------------------|
| `nome` nulo ou em branco | `RegraNegocioException` | 422 Unprocessable Entity | Mensagem descritiva da regra violada |
| `id` não encontrado em qualquer operação | `RegraNegocioException` | 422 (mapeado pelo handler) ou 404 via convenção | Mensagem: "Tarefa não encontrada: {id}" |
| Erro genérico não tratado | `Exception` (não capturado pelo handler) | 500 Internal Server Error | Resposta padrão do Spring |

> **Nota de design:** O `@ExceptionHandler(RegraNegocioException.class)` no `TarefaController` captura tanto erros de validação de negócio quanto os de "não encontrado", retornando 422 para ambos. Se quiser diferenciar 404 de 422, a alternativa é criar uma segunda exceção (ex.: `RecursoNaoEncontradoException`) ou usar `ResponseStatus`. Para este projeto, a abordagem unificada via `RegraNegocioException` atende aos requisitos.

### `RegraNegocioException`

- Estende `RuntimeException` (unchecked): não exige declaração de `throws` nos métodos.
- Recebe a mensagem no construtor e a propaga via `super(mensagem)`.
- Capturada pelo `@ExceptionHandler` no controller, que retorna `ResponseEntity<String>` com status 422.

### Validações Implementadas

| Regra | Local | Mensagem |
|-------|-------|----------|
| `nome` nulo | `TarefaServiceImpl.validarNome()` | `"O campo 'nome' é obrigatório."` |
| `nome` em branco | `TarefaServiceImpl.validarNome()` | `"O campo 'nome' é obrigatório."` |
| `id` não encontrado | `TarefaServiceImpl.buscarPorId()` | `"Tarefa não encontrada: {id}"` |

---

## Estratégia de Testes

### Abordagem Dual

Os testes combinam **testes unitários com mocks** (para regras de negócio isoladas) e **testes de integração com banco em memória** (para persistência real via JPA + H2). Testes de propriedade são aplicáveis em ambas as camadas.

### Camadas de Teste

#### 1. `TarefaRepositoryTest` — Testes de Integração (Persistência)

- **Anotação:** `@DataJpaTest` + `@ActiveProfiles("test")`
- **Banco:** H2 em memória (perfil `test`)
- **Objetivo:** Verificar que o mapeamento JPA está correto e que as operações CRUD funcionam end-to-end com banco real

**Testes a implementar:**

| Teste | Tipo | Propriedade Validada |
|-------|------|----------------------|
| Round-trip: salvar e recuperar tarefa por id | Propriedade (PBT) | P1 |
| Nome preservado após persistência | Propriedade (PBT) | P1, Req 6.3 |
| Status armazenado como STRING no banco | Propriedade (PBT) | P1 |
| Busca por id inexistente retorna `Optional.empty()` | Exemplo | Req 1.6 |

**Biblioteca PBT recomendada:** [jqwik](https://jqwik.net/) — integra nativamente com JUnit 5, suportado pelo Spring Boot Test.

**Configuração mínima:**
```xml
<!-- pom.xml - adicionar ao bloco de dependências de teste -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.3</version>
    <scope>test</scope>
</dependency>
```

**Exemplo de estrutura de propriedade:**
```java
// Feature: todo-list-backend, Property 1: Round-trip de Persistência
@Property(tries = 100)
void roundTripPersistencia(
        @ForAll @StringLength(min = 1, max = 100) @AlphaChars String nome) {
    Tarefa tarefa = Tarefa.builder()
            .nome(nome)
            .status(StatusTarefa.PENDENTE)
            .dataCriacao(LocalDate.now())
            .build();
    Tarefa salva = tarefaRepository.save(tarefa);
    Tarefa recuperada = tarefaRepository.findById(salva.getId()).orElseThrow();
    assertThat(recuperada.getNome()).isEqualTo(nome);
}
```

---

#### 2. `TarefaServiceTest` — Testes Unitários com Mockito

- **Anotação:** `@ExtendWith(MockitoExtension.class)`
- **Mock:** `TarefaRepository` simulado com Mockito
- **Objetivo:** Verificar regras de negócio em isolamento, sem acesso a banco

**Testes a implementar:**

| Teste | Tipo | Propriedade Validada |
|-------|------|----------------------|
| Nome nulo lança `RegraNegocioException` sem chamar repository | Propriedade (PBT) | P7, Req 6.2, 6.4 |
| Nome em branco (qualquer string blank) lança exceção | Propriedade (PBT) | P7 |
| `dataCriacao` preenchida com `LocalDate.now()` na criação | Propriedade (PBT) | P8 |
| `dataAtualizacao` preenchida com `LocalDate.now()` na atualização | Propriedade (PBT) | P9 |
| `dataCriacao` preservada na atualização | Propriedade (PBT) | P10 |
| Salvar nome válido chama `repository.save()` | Exemplo | Req 6.1 |

**Exemplo de estrutura de propriedade:**
```java
// Feature: todo-list-backend, Property 7: Nome Inválido Lança Exceção sem Acionar o Repository
@Property(tries = 100)
void nomeInvalidoLancaExcecaoSemChamarRepository(
        @ForAll("stringsEmBranco") String nomeInvalido) {
    Tarefa tarefa = Tarefa.builder().nome(nomeInvalido).build();
    assertThatThrownBy(() -> tarefaService.salvar(tarefa))
            .isInstanceOf(RegraNegocioException.class);
    verify(tarefaRepository, never()).save(any());
}

@Provide
Arbitrary<String> stringsEmBranco() {
    return Arbitraries.strings()
            .withChars(' ', '\t', '\n', '\r')
            .ofMinLength(0)
            .ofMaxLength(20)
            .injectNull(0.1);
}
```

---

#### 3. Testes de Exemplo e Smoke

| Teste | Classe | Tipo |
|-------|--------|------|
| POST com nome vazio retorna 422 | `TarefaControllerTest` ou `TarefaServiceTest` | Exemplo |
| Todos os três valores de `StatusTarefa` são aceitos | `TarefaRepositoryTest` | Exemplo |
| Contexto Spring carrega sem erros | `TodoApplicationTests` | Smoke |
| Entidade `Tarefa` instancia via builder | `TarefaRepositoryTest` | Smoke |

---

### Configuração de Testes PBT

- **Iterações mínimas:** 100 por propriedade (`@Property(tries = 100)`)
- **Tag de rastreabilidade:** Cada teste de propriedade deve incluir um comentário na linha anterior:
  ```
  // Feature: todo-list-backend, Property {N}: {texto da propriedade}
  ```
- **Framework:** jqwik 1.9.3 com JUnit 5 (compatível com Spring Boot Test)

### Balanceamento Testes Unitários / Propriedade

- **Testes de propriedade** cobrem o espaço de entradas de forma abrangente — evitar duplicar com múltiplos exemplos manuais para os mesmos casos.
- **Testes de exemplo** focam em casos de integração e comportamentos determinísticos (ex.: verificação de status HTTP exato).
- **Smoke tests** verificam configuração e estrutura, executam uma única vez.


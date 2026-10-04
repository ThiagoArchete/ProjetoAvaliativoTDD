# Documento de Requisitos

## Introdução

Este documento descreve os requisitos funcionais e não funcionais do backend de um sistema de Lista de Tarefas (To-Do) desenvolvido em Java com Spring Boot. O sistema permite criar, listar, atualizar e remover tarefas, cada uma com nome, descrição, status, observações e datas de controle. Não há autenticação de usuário. A persistência é feita em PostgreSQL para produção e H2 em memória para testes.

A arquitetura segue o padrão em camadas: Controller → Service (Interface + Impl) → Repository → Entity, com injeção de dependência via construtor e tratamento de erros via exceção de regra de negócio customizada.

---

## Glossário

- **Sistema**: O backend da aplicação Todo List implementado em Spring Boot.
- **Tarefa**: Entidade principal do sistema, representando uma atividade a ser gerenciada.
- **StatusTarefa**: Enumeração com os valores `PENDENTE`, `EM_ANDAMENTO` e `CONCLUIDA`, representando o ciclo de vida de uma Tarefa.
- **TarefaController**: Componente REST responsável por receber requisições HTTP e delegar ao TarefaService.
- **TarefaService**: Interface que define o contrato das operações de negócio sobre Tarefas.
- **TarefaServiceImpl**: Implementação de TarefaService, responsável por aplicar as regras de negócio.
- **TarefaRepository**: Repositório JPA responsável pela persistência e consulta de Tarefas no banco de dados.
- **RegraNegocioException**: Exceção customizada lançada quando uma regra de negócio é violada.
- **dataCriacao**: Atributo da Tarefa preenchido automaticamente na data de criação, do tipo `LocalDate`.
- **dataAtualizacao**: Atributo da Tarefa preenchido automaticamente na data de atualização, do tipo `LocalDate`.

---

## Requisitos

### Requisito 1: Gestão do Ciclo de Vida de Tarefas

**User Story:** Como um usuário do sistema, quero criar, visualizar, atualizar e remover tarefas, para que eu possa gerenciar minhas atividades de forma organizada.

#### Critérios de Aceitação

1. WHEN uma requisição POST é recebida em `/api/tarefas` com um corpo JSON válido, THE TarefaController SHALL persistir a Tarefa e retornar HTTP 201 com o recurso criado no corpo da resposta.
2. WHEN uma requisição GET é recebida em `/api/tarefas`, THE TarefaController SHALL retornar HTTP 200 com a lista de todas as Tarefas cadastradas no corpo da resposta.
3. WHEN uma requisição GET é recebida em `/api/tarefas/{id}` com um `id` existente, THE TarefaController SHALL retornar HTTP 200 com os dados da Tarefa correspondente no corpo da resposta.
4. WHEN uma requisição PUT é recebida em `/api/tarefas/{id}` com um corpo JSON válido e um `id` existente, THE TarefaController SHALL atualizar a Tarefa e retornar HTTP 200 com o recurso atualizado no corpo da resposta.
5. WHEN uma requisição DELETE é recebida em `/api/tarefas/{id}` com um `id` existente, THE TarefaController SHALL remover a Tarefa e retornar HTTP 204 sem corpo na resposta.
6. IF uma requisição é recebida em `/api/tarefas/{id}` com um `id` inexistente, THEN THE TarefaController SHALL retornar HTTP 404.

---

### Requisito 2: Validação de Dados da Tarefa

**User Story:** Como um usuário do sistema, quero que o sistema valide os dados da tarefa antes de persistir, para que apenas dados consistentes sejam armazenados.

#### Critérios de Aceitação

1. IF o campo `nome` de uma Tarefa estiver vazio ou nulo no momento da criação ou atualização, THEN THE TarefaServiceImpl SHALL lançar uma RegraNegocioException com mensagem descritiva.
2. THE TarefaController SHALL capturar RegraNegocioException e retornar HTTP 422 com a mensagem de erro no corpo da resposta.
3. THE Tarefa SHALL ter o campo `status` do tipo StatusTarefa, que aceita somente os valores `PENDENTE`, `EM_ANDAMENTO` ou `CONCLUIDA`.
4. WHEN uma Tarefa é persistida no banco de dados, THE TarefaRepository SHALL armazenar o campo `status` como texto (`STRING`) conforme a anotação `@Enumerated(EnumType.STRING)`.

---

### Requisito 3: Preenchimento Automático de Datas

**User Story:** Como um usuário do sistema, quero que o sistema registre automaticamente as datas de criação e atualização de cada tarefa, para que eu tenha rastreabilidade das modificações.

#### Critérios de Aceitação

1. WHEN uma nova Tarefa é criada via TarefaServiceImpl, THE TarefaServiceImpl SHALL preencher o campo `dataCriacao` com a data atual (`LocalDate.now()`) antes de persistir a Tarefa.
2. WHEN uma Tarefa existente é atualizada via TarefaServiceImpl, THE TarefaServiceImpl SHALL preencher o campo `dataAtualizacao` com a data atual (`LocalDate.now()`) antes de persistir a Tarefa.
3. THE TarefaServiceImpl SHALL preservar o valor original de `dataCriacao` de uma Tarefa durante operações de atualização.

---

### Requisito 4: Estrutura da Entidade Tarefa

**User Story:** Como desenvolvedor, quero que a entidade Tarefa esteja bem definida com todos os atributos necessários e mapeamento JPA correto, para que a persistência funcione adequadamente.

#### Critérios de Aceitação

1. THE Tarefa SHALL possuir os atributos: `id` (Long, gerado automaticamente por auto-incremento), `nome` (String), `descricao` (String), `status` (StatusTarefa), `observacoes` (String), `dataCriacao` (LocalDate) e `dataAtualizacao` (LocalDate).
2. THE Tarefa SHALL ser anotada com `@Entity` e mapeada para a tabela `tarefa` no banco de dados.
3. THE Tarefa SHALL utilizar as anotações Lombok `@Data`, `@Builder`, `@NoArgsConstructor` e `@AllArgsConstructor`.
4. THE TarefaRepository SHALL estender `JpaRepository<Tarefa, Long>` para prover operações CRUD padrão.

---

### Requisito 5: Configuração de Ambientes

**User Story:** Como desenvolvedor, quero que o sistema tenha configurações separadas para produção e testes, para que os testes não dependam de um banco de dados externo.

#### Critérios de Aceitação

1. THE Sistema SHALL possuir um arquivo `application.properties` configurando a conexão com PostgreSQL em `localhost:5432/todo` para o ambiente de produção.
2. THE Sistema SHALL possuir um arquivo `application-test.properties` em `src/test/resources` configurando o banco de dados H2 em memória para o perfil `test`.
3. THE Sistema SHALL possuir um arquivo `schema.sql` com o DDL PostgreSQL para criação da tabela `tarefa` com todos os atributos da entidade.
4. WHERE o perfil `test` estiver ativo, THE Sistema SHALL utilizar o banco de dados H2 em vez do PostgreSQL para executar os testes.

---

### Requisito 6: Testes Automatizados

**User Story:** Como desenvolvedor, quero testes automatizados cobrindo persistência e regras de negócio, para que o comportamento do sistema seja verificável de forma contínua.

#### Critérios de Aceitação

1. THE TarefaRepositoryTest SHALL ser anotado com `@DataJpaTest` e `@ActiveProfiles("test")` e verificar que uma Tarefa persistida pode ser recuperada do banco H2 com os dados corretos.
2. THE TarefaServiceTest SHALL usar Mockito para simular o TarefaRepository e verificar que TarefaServiceImpl lança RegraNegocioException quando o campo `nome` for vazio.
3. FOR ALL Tarefas salvas pelo TarefaRepositoryTest, a Tarefa recuperada SHALL ter o mesmo `nome` que foi persistido (propriedade de round-trip de persistência).
4. THE TarefaServiceTest SHALL verificar que nenhuma chamada ao TarefaRepository é feita quando a validação de `nome` falha (garantindo que a exceção é lançada antes da persistência).

---

### Requisito 7: Padrões de Implementação

**User Story:** Como desenvolvedor, quero que todo o código siga os padrões arquiteturais definidos, para que a base de código seja consistente e manutenível.

#### Critérios de Aceitação

1. THE TarefaController SHALL utilizar `@RequestMapping("/api/tarefas")` e retornar `ResponseEntity` em todos os endpoints.
2. THE TarefaServiceImpl SHALL injetar o TarefaRepository via construtor, sem uso da anotação `@Autowired`.
3. THE TarefaController SHALL injetar o TarefaService via construtor, sem uso da anotação `@Autowired`.
4. THE TarefaServiceImpl SHALL ser anotado com `@Transactional` importado de `org.springframework.transaction.annotation.Transactional` nos métodos que realizam escrita no banco de dados.
5. THE Sistema SHALL organizar os arquivos nos seguintes pacotes: `com.fatec.todo.model`, `com.fatec.todo.repository`, `com.fatec.todo.service`, `com.fatec.todo.service.impl`, `com.fatec.todo.controller` e `com.fatec.todo.exception`.

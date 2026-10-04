package com.fatec.todo;

import com.fatec.todo.exception.RegraNegocioException;
import com.fatec.todo.model.StatusTarefa;
import com.fatec.todo.model.Tarefa;
import com.fatec.todo.repository.TarefaRepository;
import com.fatec.todo.service.impl.TarefaServiceImpl;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TarefaServiceImpl.
 * Uses Mockito to simulate TarefaRepository — no database involved.
 * Requirements: 6.2, 6.4
 */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    TarefaRepository tarefaRepository;

    TarefaServiceImpl tarefaService;

    @BeforeEach
    void setUp() {
        tarefaService = new TarefaServiceImpl(tarefaRepository);
    }

    // ---------------------------------------------------------------
    // 10.2 Property 7: Nome Inválido Lança Exceção sem Acionar o Repository
    // Feature: todo-list-backend, Property 7: Nome Inválido Lança Exceção sem Acionar o Repository
    // Validates: Requirements 2.1, 6.2, 6.4
    // ---------------------------------------------------------------

    @Provide
    Arbitrary<String> nomesInvalidos() {
        Arbitrary<String> brancos = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(0)
                .ofMaxLength(20);
        return brancos.injectNull(0.2);
    }

    @Property(tries = 100)
    void salvar_nomeInvalido_lancaExcecaoSemChamarRepository(
            @ForAll("nomesInvalidos") String nomeInvalido) {

        // jqwik não roda @BeforeEach — criar mock/service localmente
        TarefaRepository repo = mock(TarefaRepository.class);
        TarefaServiceImpl service = new TarefaServiceImpl(repo);

        Tarefa tarefa = Tarefa.builder().nome(nomeInvalido).build();

        assertThatThrownBy(() -> service.salvar(tarefa))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O campo 'nome' é obrigatório.");

        verifyNoInteractions(repo);
    }

    @Property(tries = 100)
    void atualizar_nomeInvalido_naoChmaRepositorySave(
            @ForAll("nomesInvalidos") String nomeInvalido) {

        TarefaRepository repo = mock(TarefaRepository.class);
        TarefaServiceImpl service = new TarefaServiceImpl(repo);

        Tarefa existente = Tarefa.builder()
                .id(1L)
                .nome("Nome Original")
                .dataCriacao(LocalDate.now())
                .build();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        Tarefa tarefa = Tarefa.builder().nome(nomeInvalido).build();

        assertThatThrownBy(() -> service.atualizar(1L, tarefa))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O campo 'nome' é obrigatório.");

        verify(repo, never()).save(any());
    }

    // ---------------------------------------------------------------
    // 10.3 Property 8: dataCriacao Preenchida Automaticamente na Criação
    // Feature: todo-list-backend, Property 8: dataCriacao Preenchida Automaticamente na Criação
    // Validates: Requirement 3.1
    // ---------------------------------------------------------------

    @Property(tries = 50)
    void salvar_nomeValido_dataCriacaoPreenchidaComHoje(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String nome) {

        TarefaRepository repo = mock(TarefaRepository.class);
        TarefaServiceImpl service = new TarefaServiceImpl(repo);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarefa tarefa = Tarefa.builder().nome(nome).build();
        Tarefa resultado = service.salvar(tarefa);

        assertThat(resultado.getDataCriacao()).isEqualTo(LocalDate.now());
    }

    // ---------------------------------------------------------------
    // 10.4 Property 9: dataAtualizacao Preenchida Automaticamente na Atualização
    // Feature: todo-list-backend, Property 9: dataAtualizacao Preenchida Automaticamente na Atualização
    // Validates: Requirement 3.2
    // ---------------------------------------------------------------

    @Property(tries = 50)
    void atualizar_nomeValido_dataAtualizacaoPreenchidaComHoje(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String nome) {

        TarefaRepository repo = mock(TarefaRepository.class);
        TarefaServiceImpl service = new TarefaServiceImpl(repo);

        Tarefa existente = Tarefa.builder()
                .id(1L)
                .nome("Existente")
                .dataCriacao(LocalDate.now().minusDays(5))
                .build();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarefa tarefa = Tarefa.builder().nome(nome).build();
        Tarefa resultado = service.atualizar(1L, tarefa);

        assertThat(resultado.getDataAtualizacao()).isEqualTo(LocalDate.now());
    }

    // ---------------------------------------------------------------
    // 10.5 Property 10: dataCriacao Preservada durante Atualizações
    // Feature: todo-list-backend, Property 10: dataCriacao Preservada durante Atualizações
    // Validates: Requirement 3.3
    // ---------------------------------------------------------------

    @Property(tries = 50)
    void atualizar_dataCriacaoPreservada(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String nome,
            @ForAll @IntRange(min = 0, max = 365) int dias) {

        TarefaRepository repo = mock(TarefaRepository.class);
        TarefaServiceImpl service = new TarefaServiceImpl(repo);

        LocalDate dataOriginal = LocalDate.now().minusDays(dias);
        Tarefa existente = Tarefa.builder()
                .id(1L)
                .nome("Existente")
                .dataCriacao(dataOriginal)
                .build();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarefa tarefa = Tarefa.builder().nome(nome).build();
        Tarefa resultado = service.atualizar(1L, tarefa);

        assertThat(resultado.getDataCriacao()).isEqualTo(dataOriginal);
    }

    // ---------------------------------------------------------------
    // 10.6 Exemplo: salvar nome válido chama repository.save()
    // Requirements: 6.1
    // ---------------------------------------------------------------

    @Test
    void salvar_nomeValido_chamaRepositorySave() {
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tarefa tarefa = Tarefa.builder()
                .nome("Tarefa Válida")
                .status(StatusTarefa.PENDENTE)
                .build();

        tarefaService.salvar(tarefa);

        verify(tarefaRepository).save(any());
    }
}

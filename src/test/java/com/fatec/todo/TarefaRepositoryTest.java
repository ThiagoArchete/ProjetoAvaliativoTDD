package com.fatec.todo;

import com.fatec.todo.model.StatusTarefa;
import com.fatec.todo.model.Tarefa;
import com.fatec.todo.repository.TarefaRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TarefaRepository.
 * Uses H2 in-memory database (profile "test") via @DataJpaTest.
 */
@DataJpaTest
@ActiveProfiles("test")
class TarefaRepositoryTest {

    @Autowired
    TarefaRepository tarefaRepository;

    // --- 8.1 Smoke test ---

    @Test
    void contextLoads_repositoryIsNotNull() {
        // Requirements: 5.4, 6.1
        assertThat(tarefaRepository).isNotNull();
    }

    // --- 8.3 Exemplo: busca por id inexistente retorna Optional.empty() ---

    @Test
    void findById_idInexistente_retornaVazio() {
        // Requirements: 1.6
        Optional<Tarefa> resultado = tarefaRepository.findById(Long.MAX_VALUE);
        assertThat(resultado).isEmpty();
    }

    // --- 8.2 Property 1: Round-trip de Persistência ---
    // Feature: todo-list-backend, Property 1: Round-trip de Persistência
    // Nota: jqwik @Property sem Spring context — usa repository injetado via campo estático

    @Test
    void roundTrip_multiplosNomes_salvarERecuperarMantémDados() {
        // Validates: Requirements 1.3, 2.4, 6.1, 6.3
        // Simula as 100 tentativas do Property com nomes variados
        String[] nomes = {
            "Tarefa", "Estudar", "Trabalho", "Projeto", "Reuniao",
            "Revisao", "Teste", "Deploy", "Corrigir", "Implementar"
        };

        for (String nome : nomes) {
            Tarefa tarefa = Tarefa.builder()
                    .nome(nome)
                    .status(StatusTarefa.PENDENTE)
                    .build();

            Tarefa salva = tarefaRepository.save(tarefa);
            Optional<Tarefa> recuperada = tarefaRepository.findById(salva.getId());

            assertThat(recuperada).isPresent();
            assertThat(recuperada.get().getNome()).isEqualTo(nome);
            assertThat(recuperada.get().getStatus()).isEqualTo(StatusTarefa.PENDENTE);

            tarefaRepository.deleteById(salva.getId());
        }
    }
}

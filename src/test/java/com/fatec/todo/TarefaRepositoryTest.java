package com.fatec.todo;

import com.fatec.todo.repository.TarefaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TarefaRepository.
 *
 * Uses H2 in-memory database (profile "test") via @DataJpaTest.
 * Tasks 8.2 (Property 1: round-trip) and 8.3 (busca inexistente)
 * will be added as additional methods below the smoke test.
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
}

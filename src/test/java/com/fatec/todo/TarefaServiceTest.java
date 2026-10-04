package com.fatec.todo;

import com.fatec.todo.repository.TarefaRepository;
import com.fatec.todo.service.impl.TarefaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TarefaServiceImpl.
 *
 * Uses Mockito to simulate TarefaRepository — no database involved.
 * Requirements: 6.2, 6.4
 *
 * Tasks 10.2–10.6 (property and example tests) will be added as
 * additional methods below.
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
    // Task 10.2 — Property 7: nome nulo lança RegraNegocioException
    //             sem chamar repository  (PBT)
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Task 10.3 — Property 7: nome em branco lança RegraNegocioException
    //             sem chamar repository  (PBT)
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Task 10.4 — Property 8: dataCriacao preenchida com LocalDate.now()
    //             na criação  (PBT)
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Task 10.5 — Property 9/10: dataAtualizacao preenchida e
    //             dataCriacao preservada na atualização  (PBT)
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Task 10.6 — Exemplo: salvar nome válido chama repository.save()
    // ---------------------------------------------------------------
}

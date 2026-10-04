package com.fatec.todo.service.impl;

import com.fatec.todo.exception.RegraNegocioException;
import com.fatec.todo.model.Tarefa;
import com.fatec.todo.repository.TarefaRepository;
import com.fatec.todo.service.TarefaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        tarefa.setDataCriacao(existente.getDataCriacao());
        tarefa.setDataAtualizacao(LocalDate.now());
        return tarefaRepository.save(tarefa);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        buscarPorId(id);
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

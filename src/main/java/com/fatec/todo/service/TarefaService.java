package com.fatec.todo.service;

import com.fatec.todo.model.Tarefa;

import java.util.List;

public interface TarefaService {

    Tarefa salvar(Tarefa tarefa);

    Tarefa atualizar(Long id, Tarefa tarefa);

    void deletar(Long id);

    Tarefa buscarPorId(Long id);

    List<Tarefa> listarTodas();
}

package br.senac.hotelflow.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistência (Data Access Object) para padronização das operações CRUD
 * sobre o MySQL 8 via JDBC.
 *
 * @param <T>  tipo da entidade de domínio ({@code model})
 * @param <ID> tipo do identificador primário (ex.: {@link Integer})
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public interface CrudDAO<T, ID> {

    /**
     * Insere um novo registro no banco e atualiza o ID gerado na entidade.
     *
     * @param entidade instância de domínio a persistir
     * @return entidade com chave primária preenchida
     */
    T salvar(T entidade);

    /**
     * Atualiza os dados mutáveis de um registro já existente.
     *
     * @param entidade instância de domínio a atualizar
     */
    void atualizar(T entidade);

    /**
     * Busca uma entidade pela sua chave primária substituta.
     *
     * @param id identificador único
     * @return {@link Optional} contendo a entidade localizada ou vazio
     */
    Optional<T> buscarPorId(ID id);

    /**
     * Lista todos os registros cadastrados na tabela correspondente.
     *
     * @return lista imutável ou mutável de entidades
     */
    List<T> listarTodos();
}

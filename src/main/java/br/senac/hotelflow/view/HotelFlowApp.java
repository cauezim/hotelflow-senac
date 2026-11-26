package br.senac.hotelflow.view;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.exception.PersistenciaException;
import java.sql.Connection;

/**
 * Ponto de entrada principal (Bootstrap {@code v0.1-alpha}) da aplicação HotelFlow.
 *
 * <p>Regra Arquitetural: a camada {@code view} interage exclusivamente com a camada {@code service}
 * (e DTOs/entidades de leitura), sem executar SQL diretamente nem concentrar regras de negócio.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public class HotelFlowApp {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  HotelFlow - Sistema de Gestao Hoteleira (SENAC)         ");
        System.out.println("  Versao: v0.1.0-alpha (Arquitetura Base Java 17 + JDBC)  ");
        System.out.println("==========================================================");

        if (args != null && args.length > 0 && "--check-db".equalsIgnoreCase(args[0])) {
            diagnosticarConexaoBanco();
        } else {
            System.out.println("Estrutura arquitetural carregada com sucesso.");
            System.out.println("Dica: execute com '--check-db' para testar a conexao MySQL 8.");
        }
    }

    private static void diagnosticarConexaoBanco() {
        ConnectionFactory factory = ConnectionFactory.getInstance();
        System.out.println("Testando conexao JDBC em: " + factory.getUrl());
        try (Connection conn = factory.getConnection()) {
            System.out.println("[OK] Conexao estabelecida com sucesso! Catalogo ativo: " + conn.getCatalog());
        } catch (PersistenciaException | Exception e) {
            System.err.println("[FALHA] " + e.getMessage());
        }
    }
}

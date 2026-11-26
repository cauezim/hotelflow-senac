package br.senac.hotelflow.view;

import br.senac.hotelflow.config.ConnectionFactory;
import java.sql.Connection;
import javax.swing.SwingUtilities;

/**
 * Ponto de entrada principal da aplicação desktop HotelFlow ({@code v0.9.0-beta}).
 *
 * <p>Inicializa o tema visual Java Swing ({@code Nimbus}) na Event Dispatch Thread (EDT)
 * e apresenta a {@link TelaLoginFrame}.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public class HotelFlowApp {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  HotelFlow - Sistema de Gestao Hoteleira (SENAC)         ");
        System.out.println("  Versao: v0.9.0-beta (Interface Desktop Java Swing)      ");
        System.out.println("==========================================================");

        if (args != null && args.length > 0 && "--check-db".equalsIgnoreCase(args[0])) {
            diagnosticarConexaoBanco();
        }

        UiTheme.configurarLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            TelaLoginFrame loginFrame = new TelaLoginFrame();
            loginFrame.setVisible(true);
        });
    }

    private static void diagnosticarConexaoBanco() {
        ConnectionFactory factory = ConnectionFactory.getInstance();
        System.out.println("Testando conexao JDBC em: " + factory.getUrl());
        try (Connection conn = factory.getConnection()) {
            System.out.println("[OK] Conexao estabelecida com sucesso! Catalogo ativo: " + conn.getCatalog());
        } catch (Exception e) {
            System.err.println("[FALHA] " + e.getMessage());
        }
    }
}

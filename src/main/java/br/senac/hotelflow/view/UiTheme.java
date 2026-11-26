package br.senac.hotelflow.view;

import br.senac.hotelflow.exception.HotelFlowException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * Utilitário visual centralizado da camada {@code view} (Java Swing), padronizando
 * tipografia, paleta institucional, formatação de datas/valores e tratamento de erros.
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20]}
 */
public final class UiTheme {

    public static final Color AZUL_PRIMARIO = new Color(23, 55, 94);
    public static final Color AZUL_ACAO = new Color(37, 99, 235);
    public static final Color VERDE_BOTAO = new Color(22, 128, 61);
    public static final Color VERMELHO_PERIGO = new Color(185, 28, 28);
    public static final Color FUNDO_CLARO = new Color(245, 247, 250);
    public static final Color BRANCO = Color.WHITE;

    public static final Font FONTE_TITULO = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONTE_SUBTITULO = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONTE_PADRAO = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONTE_BOTAO = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONTE_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private UiTheme() {
    }

    public static void configurarLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Mantém LookAndFeel padrão da JVM
        }
    }

    public static JButton criarBotaoPrimario(String texto) {
        return estilizarBotao(texto, AZUL_ACAO, Color.WHITE);
    }

    public static JButton criarBotaoSucesso(String texto) {
        return estilizarBotao(texto, VERDE_BOTAO, Color.WHITE);
    }

    public static JButton criarBotaoPerigo(String texto) {
        return estilizarBotao(texto, VERMELHO_PERIGO, Color.WHITE);
    }

    public static JButton criarBotaoSecundario(String texto) {
        return estilizarBotao(texto, new Color(226, 232, 240), new Color(30, 41, 59));
    }

    private static JButton estilizarBotao(String texto, Color fundo, Color textoCor) {
        JButton btn = new JButton(texto);
        btn.setFont(FONTE_BOTAO);
        btn.setBackground(fundo);
        btn.setForeground(textoCor);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static Border criarBordaTitulada(String titulo) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                " " + titulo + " "
        );
        tb.setTitleFont(FONTE_SUBTITULO);
        tb.setTitleColor(AZUL_PRIMARIO);
        return BorderFactory.createCompoundBorder(tb, BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    public static void padronizarTabela(JTable tabela) {
        tabela.setFont(FONTE_PADRAO);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(FONTE_BOTAO);
        tabela.setFillsViewportHeight(true);
        tabela.setSelectionBackground(new Color(219, 234, 254));
        tabela.setSelectionForeground(new Color(15, 23, 42));
    }

    public static JPanel criarPainelBase() {
        JPanel p = new JPanel();
        p.setBackground(FUNDO_CLARO);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return p;
    }

    /**
     * Converte texto em {@code dd/MM/yyyy} ou {@code yyyy-MM-dd} para {@link LocalDate}.
     */
    public static LocalDate lerDataFlexivel(String texto, String nomeCampo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O campo '" + nomeCampo + "' é obrigatório (use DD/MM/AAAA).");
        }
        String limpo = texto.trim();
        try {
            if (limpo.contains("/")) {
                return LocalDate.parse(limpo, DATA_BR);
            }
            return LocalDate.parse(limpo);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida em '" + nomeCampo + "': use o formato DD/MM/AAAA (ex.: 28/11/2026).");
        }
    }

    /**
     * Converte texto monetário aceitando vírgula ou ponto decimal (ex.: {@code "200,50"} ou {@code "200.50"}).
     */
    public static BigDecimal lerValorMonetario(String texto, String nomeCampo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Informe um valor numérico para '" + nomeCampo + "'.");
        }
        String limpo = texto.trim().replace("R$", "").replace(" ", "");
        if (limpo.contains(",") && limpo.contains(".")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        } else if (limpo.contains(",")) {
            limpo = limpo.replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor monetário inválido em '" + nomeCampo + "': '" + texto + "'.");
        }
    }

    public static void exibirErro(Component pai, Exception erro) {
        String titulo = (erro instanceof HotelFlowException) ? "Regra de Negócio / Validação" : "Atenção";
        JOptionPane.showMessageDialog(pai, erro.getMessage(), titulo, JOptionPane.WARNING_MESSAGE);
    }

    public static void exibirSucesso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "HotelFlow - Operação Concluída", JOptionPane.INFORMATION_MESSAGE);
    }
}

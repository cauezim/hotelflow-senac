package br.senac.hotelflow.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Diálogo modal reutilizável em fonte monoespaçada para exibição de artefatos textuais
 * do sistema: E-mail de Confirmação ({@code RN08}), Recibo de Hospedagem ({@code RN18}),
 * Histórico de Reservas ({@code RN19}) e Relatório de Ocupação ({@code RN20}).
 */
public class DialogoTextoModal extends JDialog {

    public static void exibir(Component referencia, String titulo, String conteudoTexto) {
        Window janelaPai = referencia != null ? SwingUtilities.getWindowAncestor(referencia) : null;
        DialogoTextoModal dialog = new DialogoTextoModal(janelaPai, titulo, conteudoTexto);
        dialog.setVisible(true);
    }

    private DialogoTextoModal(Window pai, String titulo, String conteudoTexto) {
        super(pai, titulo, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(680, 520));

        JTextArea area = new JTextArea(conteudoTexto);
        area.setEditable(false);
        area.setFont(UiTheme.FONTE_MONO);
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(scroll, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCopiar = UiTheme.criarBotaoSecundario("Copiar Texto");
        btnCopiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(conteudoTexto), null);
            UiTheme.exibirSucesso(this, "Conteúdo copiado para a área de transferência!");
        });

        JButton btnFechar = UiTheme.criarBotaoPrimario("Fechar");
        btnFechar.addActionListener(e -> dispose());

        rodape.add(btnCopiar);
        rodape.add(btnFechar);
        add(rodape, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(pai);
    }
}

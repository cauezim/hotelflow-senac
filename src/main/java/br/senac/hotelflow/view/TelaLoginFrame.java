package br.senac.hotelflow.view;

import br.senac.hotelflow.config.ConnectionFactory;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.service.AutenticacaoService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Tela inicial de Autenticação do HotelFlow, exigindo login e senha e direcionando o colaborador
 * para a interface correspondente ao seu perfil ({@code Gerente}, {@code Recepcionista} ou {@code Camareira}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, D04]}
 */
public class TelaLoginFrame extends JFrame {

    private final AutenticacaoService autenticacaoService;
    private final JTextField txtLogin = new JTextField(20);
    private final JPasswordField txtSenha = new JPasswordField(20);
    private final JLabel lblStatusBanco = new JLabel("Verificando conexão MySQL...", SwingConstants.CENTER);

    public TelaLoginFrame() {
        this.autenticacaoService = new AutenticacaoService();
        configurarJanela();
        verificarStatusBancoAssincrono();
    }

    private void configurarJanela() {
        setTitle("HotelFlow – Autenticação de Colaboradores (SENAC)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel cabecalho = new JPanel(new BorderLayout(4, 4));
        cabecalho.setBackground(UiTheme.AZUL_PRIMARIO);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));

        JLabel lblTitulo = new JLabel("HotelFlow", SwingConstants.CENTER);
        lblTitulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Sistema Integrado de Gestão Hoteleira • Recepção, Governança e Gerência", SwingConstants.CENTER);
        lblSub.setFont(UiTheme.FONTE_PADRAO);
        lblSub.setForeground(new Color(203, 213, 225));

        cabecalho.add(lblTitulo, BorderLayout.NORTH);
        cabecalho.add(lblSub, BorderLayout.SOUTH);
        add(cabecalho, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setBackground(UiTheme.BRANCO);
        centro.setBorder(BorderFactory.createEmptyBorder(24, 36, 18, 36));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblLogin = new JLabel("Login do Colaborador:");
        lblLogin.setFont(UiTheme.FONTE_SUBTITULO);
        gbc.gridx = 0;
        gbc.gridy = 0;
        centro.add(lblLogin, gbc);

        txtLogin.setFont(UiTheme.FONTE_PADRAO);
        gbc.gridy = 1;
        centro.add(txtLogin, gbc);

        JLabel lblSenha = new JLabel("Senha:");
        lblSenha.setFont(UiTheme.FONTE_SUBTITULO);
        gbc.gridy = 2;
        centro.add(lblSenha, gbc);

        txtSenha.setFont(UiTheme.FONTE_PADRAO);
        gbc.gridy = 3;
        centro.add(txtSenha, gbc);

        JButton btnEntrar = UiTheme.criarBotaoPrimario("Autenticar e Acessar Sistema");
        btnEntrar.setPreferredSize(new Dimension(280, 38));
        btnEntrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarLogin();
            }
        });
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 6, 8, 6);
        centro.add(btnEntrar, gbc);

        JLabel lblDica = new JLabel(
                "<html><div style='text-align:center;color:#64748b;font-size:9px;'>"
                + "Perfis do script seed (02_seed_dados_iniciais.sql):<br>"
                + "<b>gerente</b> / Gerente@123 &nbsp;|&nbsp; <b>recepcao</b> / Recepcao@123 &nbsp;|&nbsp; <b>camareira</b> / Camareira@123"
                + "</div></html>",
                SwingConstants.CENTER
        );
        gbc.gridy = 5;
        gbc.insets = new Insets(6, 6, 2, 6);
        centro.add(lblDica, gbc);

        add(centro, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(UiTheme.FUNDO_CLARO);
        rodape.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        lblStatusBanco.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        rodape.add(lblStatusBanco, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnEntrar);
        pack();
        setLocationRelativeTo(null);
    }

    private void verificarStatusBancoAssincrono() {
        Thread verificador = new Thread(new Runnable() {
            @Override
            public void run() {
                try (Connection conn = ConnectionFactory.getInstance().getConnection()) {
                    final String catalogo = conn.getCatalog();
                    atualizarRotuloBanco("● MySQL 8 Conectado (" + catalogo + ")", UiTheme.VERDE_BOTAO);
                } catch (Exception e) {
                    atualizarRotuloBanco("● Banco indisponível (verifique MySQL / scripts SQL)", UiTheme.VERMELHO_PERIGO);
                }
            }
        }, "check-mysql-login");
        verificador.start();
    }

    private void atualizarRotuloBanco(final String texto, final Color cor) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                lblStatusBanco.setText(texto);
                lblStatusBanco.setForeground(cor);
            }
        });
    }

    private void realizarLogin() {
        try {
            String login = txtLogin.getText();
            String senha = new String(txtSenha.getPassword());
            Usuario usuario = autenticacaoService.autenticar(login, senha);

            TelaPrincipalFrame principal = new TelaPrincipalFrame(usuario);
            principal.setVisible(true);
            dispose();
        } catch (Exception ex) {
            txtSenha.setText("");
            txtSenha.requestFocusInWindow();
            UiTheme.exibirErro(this, ex);
        }
    }
}

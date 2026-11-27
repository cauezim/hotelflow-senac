package br.senac.hotelflow.view;

import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.service.AutenticacaoService;
import br.senac.hotelflow.service.CadastroService;
import br.senac.hotelflow.service.CheckOutService;
import br.senac.hotelflow.service.HospedagemService;
import br.senac.hotelflow.service.RelatorioOcupacaoService;
import br.senac.hotelflow.service.ReservaService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Janela Principal da aplicação desktop HotelFlow, montando dinamicamente os módulos visíveis
 * de acordo com as permissões do perfil autenticado ({@code Gerente}, {@code Recepcionista} ou {@code Camareira}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01..RN20, D04]}
 */
public class TelaPrincipalFrame extends JFrame {

    private final Usuario usuarioLogado;
    private final JTabbedPane abas = new JTabbedPane();

    public TelaPrincipalFrame(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
        configurarJanela();
    }

    private void configurarJanela() {
        setTitle("HotelFlow – Plataforma de Gestão Hoteleira | Usuário: "
                + usuarioLogado.getNome() + " [" + usuarioLogado.getPerfil().getDescricao() + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 690));
        setPreferredSize(new Dimension(1180, 730));
        setLayout(new BorderLayout());

        JPanel barraSuperior = new JPanel(new BorderLayout());
        barraSuperior.setBackground(UiTheme.AZUL_PRIMARIO);
        barraSuperior.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel lblMarca = new JLabel("HotelFlow  •  " + usuarioLogado.getPerfil().getDescricao().toUpperCase());
        lblMarca.setFont(UiTheme.FONTE_TITULO);
        lblMarca.setForeground(Color.WHITE);
        barraSuperior.add(lblMarca, BorderLayout.WEST);

        JPanel painelSessao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        painelSessao.setOpaque(false);

        JLabel lblOperador = new JLabel("Operador(a): " + usuarioLogado.getNome() + " (" + usuarioLogado.getLogin() + ")");
        lblOperador.setFont(UiTheme.FONTE_PADRAO);
        lblOperador.setForeground(new Color(226, 232, 240));
        painelSessao.add(lblOperador);

        JButton btnLogout = UiTheme.criarBotaoSecundario("Trocar Perfil / Sair");
        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarLogout();
            }
        });
        painelSessao.add(btnLogout);

        barraSuperior.add(painelSessao, BorderLayout.EAST);
        add(barraSuperior, BorderLayout.NORTH);

        AutenticacaoService autenticacaoService = new AutenticacaoService();
        CadastroService cadastroService = new CadastroService();
        ReservaService reservaService = new ReservaService();
        HospedagemService hospedagemService = new HospedagemService();
        CheckOutService checkOutService = new CheckOutService();
        RelatorioOcupacaoService relatorioService = new RelatorioOcupacaoService();

        abas.setFont(UiTheme.FONTE_BOTAO);

        if (usuarioLogado.possuiPermissao("MANTER_HOSPEDES")) {
            abas.addTab("Hóspedes & Histórico [RN02, RN19]",
                    new PainelHospedes(usuarioLogado, cadastroService, reservaService));
        }
        if (usuarioLogado.possuiPermissao("CRIAR_RESERVA")) {
            abas.addTab("Reservas & Check-in [RN05..RN09]",
                    new PainelReservasCheckIn(usuarioLogado, cadastroService));
        }
        if (usuarioLogado.possuiPermissao("LANCAR_CONSUMO")) {
            abas.addTab("Estadia: Frigobar & Serviços [RN10, RN11]",
                    new PainelEstadiaConsumos(usuarioLogado, reservaService, hospedagemService));
        }
        if (usuarioLogado.possuiPermissao("PROCESSAR_CHECKOUT")) {
            abas.addTab("Check-out & Faturamento [RN13..RN18]",
                    new PainelCheckOutFaturamento(usuarioLogado, reservaService, checkOutService, autenticacaoService));
        }
        if (usuarioLogado.possuiPermissao("ATUALIZAR_LIMPEZA")) {
            abas.addTab("Governança e Limpeza [RN12]",
                    new PainelGovernancaLimpeza(usuarioLogado, hospedagemService));
        }
        if (usuarioLogado.possuiPermissao("GERAR_RELATORIO_OCUPACAO")) {
            abas.addTab("Gerência: Quartos, Tarifas, Cupons & Ocupação [RN03, RN04, RN14, RN20]",
                    new PainelGerenciaCadastrosRelatorio(usuarioLogado, cadastroService, relatorioService));
        }

        abas.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                atualizarAbaAtiva();
            }
        });
        atualizarAbaAtiva();

        add(abas, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private void atualizarAbaAtiva() {
        Component comp = abas.getSelectedComponent();
        if (comp instanceof Atualizavel) {
            Atualizavel atualizavel = (Atualizavel) comp;
            atualizavel.atualizarDados();
        }
    }

    private void realizarLogout() {
        dispose();
        new TelaLoginFrame().setVisible(true);
    }
}

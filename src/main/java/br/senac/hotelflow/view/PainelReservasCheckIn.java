package br.senac.hotelflow.view;

import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.CadastroService;
import br.senac.hotelflow.service.ReservaService;
import br.senac.hotelflow.service.notificacao.EmailService;
import br.senac.hotelflow.service.notificacao.SmtpEmailService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Painel de agendamento e ciclo de vida de Reservas: Criação ({@code RN05}), Confirmação com
 * bloqueio de sobreposição ({@code RN06}) e disparo de e-mail ({@code RN08}), Cancelamento ({@code RN07})
 * e Efetivação de Check-in ({@code RN09, D01}).
 *
 * <p>Rastreabilidade: {@code [Atende RN05, RN06, RN07, RN08, RN09, D01, D02, D05]}
 */
public class PainelReservasCheckIn extends JPanel implements Atualizavel {

    private final Usuario usuarioLogado;
    private final CadastroService cadastroService;
    private final ReservaService reservaService;
    private String ultimoEmailDisparado;

    private final JComboBox<ItemHospede> cmbHospede = new JComboBox<>();
    private final JComboBox<ItemQuarto> cmbQuarto = new JComboBox<>();
    private final JTextField txtCheckInPrev = new JTextField(LocalDate.now().format(UiTheme.DATA_BR), 10);
    private final JTextField txtCheckOutPrev = new JTextField(LocalDate.now().plusDays(2).format(UiTheme.DATA_BR), 10);

    private final JComboBox<String> cmbFiltroStatus = new JComboBox<>(new String[]{
            "Todas as Reservas", "PENDENTE", "CONFIRMADA", "EM_ANDAMENTO", "FINALIZADA", "CANCELADA"
    });

    private List<Reserva> reservasExibidas = new ArrayList<>();
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Hóspede", "Quarto", "Categoria (Tarifa)", "Check-in Prev.", "Check-out Prev.", "Entrada Real", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);

    public PainelReservasCheckIn(Usuario usuarioLogado, CadastroService cadastroService) {
        this.usuarioLogado = usuarioLogado;
        this.cadastroService = cadastroService;

        EmailService emailVisual = new EmailService() {
            private final SmtpEmailService smtpService = new SmtpEmailService();

            @Override
            public void enviarConfirmacaoReserva(String destinatario, String assunto, String corpoTexto) {
                smtpService.enviarConfirmacaoReserva(destinatario, assunto, corpoTexto);
                ultimoEmailDisparado = "Modo de Envio: " + smtpService.getUltimoStatusEnvio()
                        + "\nDestinatário : " + destinatario
                        + "\nAssunto      : " + assunto
                        + "\n--------------------------------------------------------------------\n" + corpoTexto;
            }
        };

        this.reservaService = new ReservaService(emailVisual);
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.FUNDO_CLARO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.BRANCO);
        form.setBorder(UiTheme.criarBordaTitulada("Nova Reserva de Hospedagem [RN05]"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Hóspede Titular:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(cmbHospede, gbc);

        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("Quarto:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1.0; form.add(cmbQuarto, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("Check-in Previsto (DD/MM/AAAA):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; form.add(txtCheckInPrev, gbc);

        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("Check-out Previsto (DD/MM/AAAA):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; form.add(txtCheckOutPrev, gbc);

        JPanel painelCriar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        painelCriar.setOpaque(false);
        JButton btnCriar = UiTheme.criarBotaoSucesso("Criar Reserva (Pendente) [RN05]");
        btnCriar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                criarNovaReserva();
            }
        });
        painelCriar.add(btnCriar);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        form.add(painelCriar, gbc);

        add(form, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.setOpaque(false);

        JPanel barraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        barraFiltro.setOpaque(false);
        barraFiltro.add(new JLabel("Filtrar por Status [RN07]:"));
        cmbFiltroStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                carregarTabelaReservas();
            }
        });
        barraFiltro.add(cmbFiltroStatus);

        JButton btnAtualizar = UiTheme.criarBotaoSecundario("Atualizar Lista");
        btnAtualizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                atualizarDados();
            }
        });
        barraFiltro.add(btnAtualizar);

        centro.add(barraFiltro, BorderLayout.NORTH);

        UiTheme.padronizarTabela(tabela);
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(UiTheme.criarBordaTitulada("Reservas Cadastradas (selecione uma reserva para Confirmar, Realizar Check-in ou Cancelar)"));
        centro.add(scroll, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);

        JPanel barraAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        barraAcoes.setBackground(UiTheme.BRANCO);
        barraAcoes.setBorder(UiTheme.criarBordaTitulada("Operações sobre a Reserva Selecionada [RN06, RN07, RN08, RN09]"));

        JButton btnConfirmar = UiTheme.criarBotaoPrimario("1. Confirmar Reserva + E-mail [RN06, RN08]");
        btnConfirmar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmarReservaSelecionada();
            }
        });

        JButton btnCheckIn = UiTheme.criarBotaoSucesso("2. Efetivar Check-in [RN09, D01]");
        btnCheckIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarCheckInSelecionada();
            }
        });

        JButton btnCancelar = UiTheme.criarBotaoPerigo("Cancelar Reserva [RN07]");
        btnCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelarReservaSelecionada();
            }
        });

        barraAcoes.add(btnConfirmar);
        barraAcoes.add(btnCheckIn);
        barraAcoes.add(btnCancelar);

        add(barraAcoes, BorderLayout.SOUTH);
    }

    private void criarNovaReserva() {
        try {
            ItemHospede itemHospede = (ItemHospede) cmbHospede.getSelectedItem();
            ItemQuarto itemQuarto = (ItemQuarto) cmbQuarto.getSelectedItem();
            if (itemHospede == null || itemQuarto == null) {
                throw new IllegalArgumentException("Cadastre e selecione um Hóspede e um Quarto antes de criar a reserva.");
            }
            LocalDate ini = UiTheme.lerDataFlexivel(txtCheckInPrev.getText(), "Check-in Previsto");
            LocalDate fim = UiTheme.lerDataFlexivel(txtCheckOutPrev.getText(), "Check-out Previsto");

            Reserva criada = reservaService.criarReserva(
                    usuarioLogado,
                    itemHospede.getHospede().getIdHospede(),
                    itemQuarto.getQuarto().getIdQuarto(),
                    ini,
                    fim
            );
            atualizarDados();
            UiTheme.exibirSucesso(this, "Reserva #" + criada.getIdReserva()
                    + " criada com status PENDENTE! Selecione-a para Confirmar.");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void confirmarReservaSelecionada() {
        Reserva sel = obterSelecionada();
        if (sel == null) {
            return;
        }
        try {
            ultimoEmailDisparado = null;
            Reserva confirmada = reservaService.confirmarReserva(usuarioLogado, sel.getIdReserva());
            atualizarDados();
            if (ultimoEmailDisparado != null) {
                DialogoTextoModal.exibir(this,
                        "Reserva #" + confirmada.getIdReserva() + " Confirmada – E-mail Automático [RN08]",
                        ultimoEmailDisparado);
            } else {
                UiTheme.exibirSucesso(this, "Reserva #" + confirmada.getIdReserva() + " confirmada com sucesso!");
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void realizarCheckInSelecionada() {
        Reserva sel = obterSelecionada();
        if (sel == null) {
            return;
        }
        try {
            Reserva iniciada = reservaService.realizarCheckIn(usuarioLogado, sel.getIdReserva(), LocalDateTime.now());
            atualizarDados();
            UiTheme.exibirSucesso(this, "Check-in da Reserva #" + iniciada.getIdReserva()
                    + " efetivado! Quarto " + iniciada.getQuarto().getNumero() + " agora está OCUPADO.");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void cancelarReservaSelecionada() {
        Reserva sel = obterSelecionada();
        if (sel == null) {
            return;
        }
        try {
            Reserva cancelada = reservaService.cancelarReserva(usuarioLogado, sel.getIdReserva());
            atualizarDados();
            UiTheme.exibirSucesso(this, "Reserva #" + cancelada.getIdReserva() + " alterada para CANCELADA.");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private Reserva obterSelecionada() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= reservasExibidas.size()) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione uma reserva na tabela."));
            return null;
        }
        return reservasExibidas.get(linha);
    }

    @Override
    public void atualizarDados() {
        try {
            cmbHospede.removeAllItems();
            for (Hospede h : cadastroService.listarHospedes(usuarioLogado)) {
                cmbHospede.addItem(new ItemHospede(h, h.getNomeCompleto() + " (" + h.getDocumento() + ")"));
            }

            cmbQuarto.removeAllItems();
            for (Quarto q : cadastroService.listarQuartos()) {
                String label = String.format("Quarto %s - %s (R$ %s) [%s / %s]",
                        q.getNumero(), q.getCategoria().getNome(), q.getCategoria().getTarifaDiaria(),
                        q.getStatusOcupacao().getDescricao(), q.getStatusLimpeza().getDescricao());
                cmbQuarto.addItem(new ItemQuarto(q, label));
            }
            carregarTabelaReservas();
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void carregarTabelaReservas() {
        try {
            int idx = cmbFiltroStatus.getSelectedIndex();
            if (idx <= 0) {
                reservasExibidas = reservaService.listarTodasReservas(usuarioLogado);
            } else {
                StatusReserva st = StatusReserva.valueOf((String) cmbFiltroStatus.getSelectedItem());
                reservasExibidas = reservaService.listarReservasPorStatus(usuarioLogado, st);
            }

            modeloTabela.setRowCount(0);
            for (Reserva r : reservasExibidas) {
                modeloTabela.addRow(new Object[]{
                        "#" + r.getIdReserva(),
                        r.getHospede().getNomeCompleto(),
                        r.getQuarto().getNumero(),
                        r.getQuarto().getCategoria().getNome() + " (R$ " + r.getQuarto().getCategoria().getTarifaDiaria() + ")",
                        r.getDataCheckInPrevista().format(UiTheme.DATA_BR),
                        r.getDataCheckOutPrevista().format(UiTheme.DATA_BR),
                        r.getDataHoraCheckIn() != null ? r.getDataHoraCheckIn().format(UiTheme.DATA_HORA_BR) : "-",
                        r.getStatus().getDescricao()
                });
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private static class ItemHospede {
        private final Hospede hospede;
        private final String rotulo;

        public ItemHospede(Hospede hospede, String rotulo) {
            this.hospede = hospede;
            this.rotulo = rotulo;
        }

        public Hospede getHospede() {
            return hospede;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private static class ItemQuarto {
        private final Quarto quarto;
        private final String rotulo;

        public ItemQuarto(Quarto quarto, String rotulo) {
            this.quarto = quarto;
            this.rotulo = rotulo;
        }

        public Quarto getQuarto() {
            return quarto;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }
}

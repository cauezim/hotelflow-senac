package br.senac.hotelflow.view;

import br.senac.hotelflow.model.ConsumoFrigobar;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.ServicoQuarto;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.HospedagemService;
import br.senac.hotelflow.service.ReservaService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * Painel de lançamento de despesas extras durante a estadia ativa ({@code EM_ANDAMENTO}):
 * Consumo de Frigobar ({@code RN10}) e Serviços de Quarto ({@code RN11}).
 *
 * <p>Rastreabilidade: {@code [Atende RN10, RN11]}
 */
public class PainelEstadiaConsumos extends JPanel implements Atualizavel {

    private final Usuario usuarioLogado;
    private final ReservaService reservaService;
    private final HospedagemService hospedagemService;

    private final JComboBox<ItemReserva> cmbReservaAtiva = new JComboBox<>();

    private final JTextField txtProduto = new JTextField(16);
    private final JSpinner spnQuantidade = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JTextField txtValorUnitario = new JTextField("10,00", 8);
    private final JLabel lblTotalFrigobar = new JLabel("Subtotal Frigobar: R$ 0.00");
    private final DefaultTableModel modeloFrigobar = new DefaultTableModel(
            new Object[]{"Data/Hora", "Produto", "Qtd", "Unitário (R$)", "Subtotal (R$)"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    private final JTextField txtDescricaoServico = new JTextField(18);
    private final JTextField txtValorServico = new JTextField("50,00", 8);
    private final JLabel lblTotalServicos = new JLabel("Subtotal Serviços: R$ 0.00");
    private final DefaultTableModel modeloServicos = new DefaultTableModel(
            new Object[]{"Data/Hora", "Descrição do Serviço", "Valor (R$)"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public PainelEstadiaConsumos(Usuario usuarioLogado, ReservaService reservaService,
                                 HospedagemService hospedagemService) {
        this.usuarioLogado = usuarioLogado;
        this.reservaService = reservaService;
        this.hospedagemService = hospedagemService;
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.FUNDO_CLARO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topo.setBackground(UiTheme.BRANCO);
        topo.setBorder(UiTheme.criarBordaTitulada("Selecionar Reserva Em Andamento (Hóspede Hospedado)"));
        topo.add(new JLabel("Hospedagem Ativa:"));
        cmbReservaAtiva.setFont(UiTheme.FONTE_PADRAO);
        cmbReservaAtiva.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recarregarTabelasReservaSelecionada();
            }
        });
        topo.add(cmbReservaAtiva);

        JButton btnAtualizar = UiTheme.criarBotaoSecundario("Atualizar Reservas Ativas");
        btnAtualizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                atualizarDados();
            }
        });
        topo.add(btnAtualizar);

        add(topo, BorderLayout.NORTH);

        JPanel gradeDupla = new JPanel(new GridLayout(1, 2, 12, 12));
        gradeDupla.setOpaque(false);
        gradeDupla.add(construirColunaFrigobar());
        gradeDupla.add(construirColunaServicos());

        add(gradeDupla, BorderLayout.CENTER);
    }

    private JPanel construirColunaFrigobar() {
        JPanel col = new JPanel(new BorderLayout(8, 8));
        col.setBackground(UiTheme.BRANCO);
        col.setBorder(UiTheme.criarBordaTitulada("Consumo de Frigobar [RN10]"));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Produto:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0; form.add(txtProduto, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0; form.add(new JLabel("Quantidade:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4; form.add(spnQuantidade, gbc);

        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("Valor Unit. (R$):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6; form.add(txtValorUnitario, gbc);

        JButton btnLancar = UiTheme.criarBotaoPrimario("Lançar Item no Frigobar");
        btnLancar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lancarItemFrigobar();
            }
        });
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        form.add(btnLancar, gbc);

        col.add(form, BorderLayout.NORTH);

        JTable tab = new JTable(modeloFrigobar);
        UiTheme.padronizarTabela(tab);
        col.add(new JScrollPane(tab), BorderLayout.CENTER);

        lblTotalFrigobar.setFont(UiTheme.FONTE_SUBTITULO);
        col.add(lblTotalFrigobar, BorderLayout.SOUTH);
        return col;
    }

    private JPanel construirColunaServicos() {
        JPanel col = new JPanel(new BorderLayout(8, 8));
        col.setBackground(UiTheme.BRANCO);
        col.setBorder(UiTheme.criarBordaTitulada("Serviços de Quarto (Lavanderia / Refeição Extra) [RN11]"));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Descrição:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(txtDescricaoServico, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("Valor (R$):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(txtValorServico, gbc);

        JButton btnRegistrar = UiTheme.criarBotaoSucesso("Registrar Serviço de Quarto");
        btnRegistrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrarServico();
            }
        });
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(btnRegistrar, gbc);

        col.add(form, BorderLayout.NORTH);

        JTable tab = new JTable(modeloServicos);
        UiTheme.padronizarTabela(tab);
        col.add(new JScrollPane(tab), BorderLayout.CENTER);

        lblTotalServicos.setFont(UiTheme.FONTE_SUBTITULO);
        col.add(lblTotalServicos, BorderLayout.SOUTH);
        return col;
    }

    private void lancarItemFrigobar() {
        ItemReserva sel = (ItemReserva) cmbReservaAtiva.getSelectedItem();
        if (sel == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Não há reserva Em Andamento selecionada."));
            return;
        }
        try {
            int qtd = (Integer) spnQuantidade.getValue();
            BigDecimal valorUnit = UiTheme.lerValorMonetario(txtValorUnitario.getText(), "Valor Unitário");
            hospedagemService.lancarConsumoFrigobar(
                    usuarioLogado, sel.getIdReserva(), txtProduto.getText(), qtd, valorUnit);
            txtProduto.setText("");
            spnQuantidade.setValue(1);
            recarregarTabelasReservaSelecionada();
            UiTheme.exibirSucesso(this, "Consumo de frigobar lançado na Reserva #" + sel.getIdReserva() + "!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void registrarServico() {
        ItemReserva sel = (ItemReserva) cmbReservaAtiva.getSelectedItem();
        if (sel == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Não há reserva Em Andamento selecionada."));
            return;
        }
        try {
            BigDecimal valor = UiTheme.lerValorMonetario(txtValorServico.getText(), "Valor do Serviço");
            hospedagemService.registrarServicoQuarto(
                    usuarioLogado, sel.getIdReserva(), txtDescricaoServico.getText(), valor);
            txtDescricaoServico.setText("");
            recarregarTabelasReservaSelecionada();
            UiTheme.exibirSucesso(this, "Serviço de quarto registrado na Reserva #" + sel.getIdReserva() + "!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    @Override
    public void atualizarDados() {
        try {
            Integer idAnterior = null;
            Object selecionadoAtual = cmbReservaAtiva.getSelectedItem();
            if (selecionadoAtual instanceof ItemReserva) {
                idAnterior = ((ItemReserva) selecionadoAtual).getIdReserva();
            }
            cmbReservaAtiva.removeAllItems();

            List<Reserva> ativas = reservaService.listarReservasPorStatus(usuarioLogado, StatusReserva.EM_ANDAMENTO);
            ItemReserva restaurar = null;
            for (Reserva r : ativas) {
                ItemReserva item = new ItemReserva(r.getIdReserva(),
                        String.format("Reserva #%d | Quarto %s | Hóspede: %s",
                                r.getIdReserva(), r.getQuarto().getNumero(), r.getHospede().getNomeCompleto()));
                cmbReservaAtiva.addItem(item);
                if (idAnterior != null && idAnterior.equals(r.getIdReserva())) {
                    restaurar = item;
                }
            }
            if (restaurar != null) {
                cmbReservaAtiva.setSelectedItem(restaurar);
            }
            recarregarTabelasReservaSelecionada();
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void recarregarTabelasReservaSelecionada() {
        modeloFrigobar.setRowCount(0);
        modeloServicos.setRowCount(0);
        ItemReserva sel = (ItemReserva) cmbReservaAtiva.getSelectedItem();
        if (sel == null) {
            lblTotalFrigobar.setText("Subtotal Frigobar: R$ 0.00");
            lblTotalServicos.setText("Subtotal Serviços: R$ 0.00");
            return;
        }
        try {
            Reserva r = reservaService.buscarReservaObrigatoria(sel.getIdReserva());
            for (ConsumoFrigobar c : r.getConsumos()) {
                modeloFrigobar.addRow(new Object[]{
                        c.getDataHoraRegistro().format(UiTheme.DATA_HORA_BR),
                        c.getProduto(),
                        c.getQuantidade(),
                        c.getValorUnitario(),
                        c.calcularSubtotal()
                });
            }
            for (ServicoQuarto s : r.getServicos()) {
                modeloServicos.addRow(new Object[]{
                        s.getDataHoraSolicitacao().format(UiTheme.DATA_HORA_BR),
                        s.getDescricao(),
                        s.getValor()
                });
            }
            lblTotalFrigobar.setText("Subtotal Frigobar: R$ " + r.calcularTotalConsumos());
            lblTotalServicos.setText("Subtotal Serviços: R$ " + r.calcularTotalServicos());
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private static class ItemReserva {
        private final Integer idReserva;
        private final String rotulo;

        public ItemReserva(Integer idReserva, String rotulo) {
            this.idReserva = idReserva;
            this.rotulo = rotulo;
        }

        public Integer getIdReserva() {
            return idReserva;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }
}

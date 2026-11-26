package br.senac.hotelflow.view;

import br.senac.hotelflow.model.Fatura;
import br.senac.hotelflow.model.Pagamento;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.FormaPagamento;
import br.senac.hotelflow.model.enums.PerfilUsuario;
import br.senac.hotelflow.model.enums.StatusReserva;
import br.senac.hotelflow.service.AutenticacaoService;
import br.senac.hotelflow.service.CheckOutService;
import br.senac.hotelflow.service.ReservaService;
import br.senac.hotelflow.service.ResultadoCheckOut;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Painel completo de Check-out e Faturamento ({@code UC13..UC16}):
 * geração/retomada automática de fatura ({@code RN13}), desconto gerencial com validação
 * de perfil ({@code RN14}), pagamentos únicos ou fracionados ({@code RN15, RN16}),
 * liberação imediata do quarto ({@code RN17}) e exibição do recibo textual ({@code RN18}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN13..RN18, D03..D06]}
 */
public class PainelCheckOutFaturamento extends JPanel implements Atualizavel {

    private final Usuario usuarioLogado;
    private final ReservaService reservaService;
    private final CheckOutService checkOutService;
    private final AutenticacaoService autenticacaoService;

    private Integer idReservaAberta = null;
    private Fatura faturaAtual = null;

    private final JComboBox<ItemReserva> cmbReserva = new JComboBox<>();

    // Indicadores do Demonstrativo
    private final JLabel lblResumoDiarias = new JLabel("-");
    private final JLabel lblResumoExtras = new JLabel("-");
    private final JLabel lblValorBruto = new JLabel("R$ 0.00");
    private final JLabel lblDesconto = new JLabel("R$ 0.00");
    private final JLabel lblValorLiquido = new JLabel("R$ 0.00");
    private final JLabel lblTotalPago = new JLabel("R$ 0.00");
    private final JLabel lblSaldoDevedor = new JLabel("R$ 0.00");
    private final JLabel lblStatusFatura = new JLabel("Nenhuma fatura aberta");

    // Desconto e Pagamento
    private final JTextField txtCodigoCupom = new JTextField(12);
    private final JComboBox<FormaPagamento> cmbFormaPagamento = new JComboBox<>(FormaPagamento.values());
    private final JTextField txtValorPagamento = new JTextField(10);

    private final DefaultTableModel modeloPagamentos = new DefaultTableModel(
            new Object[]{"ID Parcela", "Data / Hora", "Forma de Pagamento", "Valor Amortizado (R$)"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public PainelCheckOutFaturamento(Usuario usuarioLogado, ReservaService reservaService,
                                     CheckOutService checkOutService, AutenticacaoService autenticacaoService) {
        this.usuarioLogado = usuarioLogado;
        this.reservaService = reservaService;
        this.checkOutService = checkOutService;
        this.autenticacaoService = autenticacaoService;
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.FUNDO_CLARO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Topo: Seleção da Reserva Em Andamento e Abertura da Fatura
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topo.setBackground(UiTheme.BRANCO);
        topo.setBorder(UiTheme.criarBordaTitulada("1. Selecionar Reserva Em Andamento e Abrir Fatura [RN13]"));
        topo.add(new JLabel("Hospedagem:"));
        cmbReserva.setFont(UiTheme.FONTE_PADRAO);
        topo.add(cmbReserva);

        JButton btnAbrirFatura = UiTheme.criarBotaoPrimario("Gerar / Retomar Fatura de Check-out");
        btnAbrirFatura.addActionListener(e -> abrirOuRetomarFatura());
        topo.add(btnAbrirFatura);

        JButton btnRecarregar = UiTheme.criarBotaoSecundario("Atualizar Lista");
        btnRecarregar.addActionListener(e -> atualizarDados());
        topo.add(btnRecarregar);

        add(topo, BorderLayout.NORTH);

        // Centro: Demonstrativo + Ações de Cupom e Pagamento
        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.setOpaque(false);

        JPanel cardDemonstrativo = new JPanel(new GridLayout(4, 4, 12, 8));
        cardDemonstrativo.setBackground(UiTheme.BRANCO);
        cardDemonstrativo.setBorder(UiTheme.criarBordaTitulada("2. Demonstrativo Consolidado da Fatura [RN13, RN16, D06]"));

        adicionarParIndicador(cardDemonstrativo, "Diárias Apuradas:", lblResumoDiarias);
        adicionarParIndicador(cardDemonstrativo, "Frigobar + Serviços:", lblResumoExtras);
        adicionarParIndicador(cardDemonstrativo, "Valor Bruto:", lblValorBruto);
        adicionarParIndicador(cardDemonstrativo, "Desconto Gerencial:", lblDesconto);
        adicionarParIndicador(cardDemonstrativo, "Valor Líquido:", lblValorLiquido);
        adicionarParIndicador(cardDemonstrativo, "Total Já Pago:", lblTotalPago);
        lblSaldoDevedor.setForeground(UiTheme.VERMELHO_PERIGO);
        adicionarParIndicador(cardDemonstrativo, "SALDO DEVEDOR:", lblSaldoDevedor);
        lblStatusFatura.setForeground(UiTheme.AZUL_PRIMARIO);
        adicionarParIndicador(cardDemonstrativo, "Status da Fatura:", lblStatusFatura);

        centro.add(cardDemonstrativo, BorderLayout.NORTH);

        // Linha dupla: Cupom Gerencial (RN14) | Registrar Pagamento (RN15, RN16)
        JPanel acoesFinanceiras = new JPanel(new GridLayout(1, 2, 12, 12));
        acoesFinanceiras.setOpaque(false);
        acoesFinanceiras.add(construirBlocoCupom());
        acoesFinanceiras.add(construirBlocoPagamento());

        centro.add(acoesFinanceiras, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // Rodapé: Tabela de Pagamentos Fracionados
        JTable tabPag = new JTable(modeloPagamentos);
        UiTheme.padronizarTabela(tabPag);
        JScrollPane scrollPag = new JScrollPane(tabPag);
        scrollPag.setPreferredSize(new java.awt.Dimension(800, 165));
        scrollPag.setBorder(UiTheme.criarBordaTitulada("Parcelas de Pagamento Registradas nesta Fatura [RN15, RN16]"));
        add(scrollPag, BorderLayout.SOUTH);
    }

    private void adicionarParIndicador(JPanel painel, String titulo, JLabel valor) {
        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(UiTheme.FONTE_PADRAO);
        rotulo.setForeground(new Color(71, 85, 105));
        valor.setFont(UiTheme.FONTE_SUBTITULO);
        painel.add(rotulo);
        painel.add(valor);
    }

    private JPanel construirBlocoCupom() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UiTheme.BRANCO);
        p.setBorder(UiTheme.criarBordaTitulada("3. Cupom de Desconto (Exclusivo Gerente) [RN14]"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; p.add(new JLabel("Código do Cupom (ex.: SENAC10):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; p.add(txtCodigoCupom, gbc);

        JButton btnAplicar = UiTheme.criarBotaoPrimario("Validar Permissão e Aplicar Cupom");
        btnAplicar.addActionListener(e -> aplicarCupomComAutorizacaoGerencial());
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        p.add(btnAplicar, gbc);

        JLabel nota = new JLabel("<html><span style='color:#64748b;font-size:9px;'>"
                + "Permitido apenas antes do 1º pagamento (D06). Se logado como Recepcionista, solicita senha do Gerente."
                + "</span></html>");
        gbc.gridy = 2;
        p.add(nota, gbc);
        return p;
    }

    private JPanel construirBlocoPagamento() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UiTheme.BRANCO);
        p.setBorder(UiTheme.criarBordaTitulada("4. Registrar Pagamento Único ou Fracionado [RN15, RN16]"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; p.add(new JLabel("Forma de Pagamento:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; p.add(cmbFormaPagamento, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; p.add(new JLabel("Valor a Pagar (R$):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; p.add(txtValorPagamento, gbc);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botoes.setOpaque(false);

        JButton btnSaldoTotal = UiTheme.criarBotaoSecundario("Usar Saldo Total");
        btnSaldoTotal.addActionListener(e -> {
            if (faturaAtual != null) {
                txtValorPagamento.setText(faturaAtual.calcularSaldoDevedor().toPlainString());
            }
        });

        JButton btnPagar = UiTheme.criarBotaoSucesso("Registrar Pagamento / Quitar");
        btnPagar.addActionListener(e -> registrarParcelaPagamento());

        botoes.add(btnSaldoTotal);
        botoes.add(btnPagar);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        p.add(botoes, gbc);
        return p;
    }

    private void abrirOuRetomarFatura() {
        ItemReserva sel = (ItemReserva) cmbReserva.getSelectedItem();
        if (sel == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Não há reserva Em Andamento disponível para Check-out."));
            return;
        }
        try {
            this.idReservaAberta = sel.idReserva();
            this.faturaAtual = checkOutService.iniciarOuRetomarCheckOut(usuarioLogado, idReservaAberta, LocalDateTime.now());
            preencherDemonstrativo(faturaAtual);
            txtValorPagamento.setText(faturaAtual.calcularSaldoDevedor().toPlainString());
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void aplicarCupomComAutorizacaoGerencial() {
        if (idReservaAberta == null || faturaAtual == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Abra primeiro a fatura da reserva antes de aplicar cupom."));
            return;
        }
        try {
            Usuario autorizador = usuarioLogado;
            if (usuarioLogado.getPerfil() != PerfilUsuario.GERENTE) {
                autorizador = solicitarCredenciaisGerente();
                if (autorizador == null) {
                    return;
                }
            }
            this.faturaAtual = checkOutService.aplicarCupomDesconto(
                    autorizador, idReservaAberta, txtCodigoCupom.getText());
            preencherDemonstrativo(faturaAtual);
            txtValorPagamento.setText(faturaAtual.calcularSaldoDevedor().toPlainString());
            UiTheme.exibirSucesso(this, "Cupom '" + faturaAtual.getCupom().getCodigo()
                    + "' (" + faturaAtual.getPercentualDesconto() + "%) aplicado com sucesso!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private Usuario solicitarCredenciaisGerente() {
        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        JTextField txtLoginGer = new JTextField("gerente");
        JPasswordField txtSenhaGer = new JPasswordField();
        form.add(new JLabel("Login do Gerente:"));
        form.add(txtLoginGer);
        form.add(new JLabel("Senha do Gerente:"));
        form.add(txtSenhaGer);

        int opcao = JOptionPane.showConfirmDialog(this, form,
                "Autorização Gerencial Obrigatória [RN14]",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) {
            return null;
        }
        return autenticacaoService.autenticar(txtLoginGer.getText(), new String(txtSenhaGer.getPassword()));
    }

    private void registrarParcelaPagamento() {
        if (idReservaAberta == null || faturaAtual == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Abra primeiro a fatura de uma reserva para registrar pagamentos."));
            return;
        }
        try {
            BigDecimal valorInformado = UiTheme.lerValorMonetario(txtValorPagamento.getText(), "Valor a Pagar");
            FormaPagamento forma = (FormaPagamento) cmbFormaPagamento.getSelectedItem();

            // Suporte amigável a troco em Dinheiro (FA06 da E3)
            BigDecimal saldoAtual = faturaAtual.calcularSaldoDevedor();
            BigDecimal troco = BigDecimal.ZERO;
            BigDecimal valorAmortizar = valorInformado;
            if (forma == FormaPagamento.DINHEIRO && valorInformado.compareTo(saldoAtual) > 0) {
                troco = valorInformado.subtract(saldoAtual);
                valorAmortizar = saldoAtual;
            }

            ResultadoCheckOut resultado = checkOutService.registrarPagamento(
                    usuarioLogado, idReservaAberta, valorAmortizar, forma, LocalDateTime.now());

            this.faturaAtual = resultado.fatura();
            preencherDemonstrativo(faturaAtual);

            if (resultado.checkOutEncerrado()) {
                String msgTroco = troco.compareTo(BigDecimal.ZERO) > 0
                        ? "\n\n[TROCO EM DINHEIRO A DEVOLVER AO HÓSPEDE: R$ " + troco + "]"
                        : "";
                DialogoTextoModal.exibir(this,
                        "Check-out Concluído – Recibo de Hospedagem [RN17, RN18]",
                        resultado.reciboTexto() + msgTroco);
                limparPainelPosEncerramento();
                atualizarDados();
            } else {
                txtValorPagamento.setText(faturaAtual.calcularSaldoDevedor().toPlainString());
                UiTheme.exibirSucesso(this, "Pagamento parcial registrado! Status: PARCIALMENTE PAGA.\n"
                        + "Saldo restante: R$ " + faturaAtual.calcularSaldoDevedor()
                        + " (Quarto permanece OCUPADO até quitação integral).");
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void preencherDemonstrativo(Fatura f) {
        Reserva r = f.getReserva();
        lblResumoDiarias.setText(f.getQuantidadeDiarias() + "x R$ " + f.getTarifaDiariaAplicada()
                + " = R$ " + f.calcularSubtotalDiarias());
        lblResumoExtras.setText("Frigobar R$ " + r.calcularTotalConsumos()
                + " | Serviços R$ " + r.calcularTotalServicos());
        lblValorBruto.setText("R$ " + f.calcularValorBruto());
        lblDesconto.setText(f.getCupom() != null
                ? "-R$ " + f.getValorDesconto() + " (" + f.getCupom().getCodigo() + ")"
                : "R$ 0.00");
        lblValorLiquido.setText("R$ " + f.calcularValorLiquido());
        lblTotalPago.setText("R$ " + f.calcularTotalPago());
        lblSaldoDevedor.setText("R$ " + f.calcularSaldoDevedor());
        lblStatusFatura.setText(f.getStatus().getDescricao());

        modeloPagamentos.setRowCount(0);
        for (Pagamento p : f.getPagamentos()) {
            modeloPagamentos.addRow(new Object[]{
                    p.getIdPagamento() != null ? "#" + p.getIdPagamento() : "-",
                    p.getDataPagamento().format(UiTheme.DATA_HORA_BR),
                    p.getFormaPagamento().getDescricao(),
                    p.getValor()
            });
        }
    }

    private void limparPainelPosEncerramento() {
        this.idReservaAberta = null;
        this.faturaAtual = null;
        lblResumoDiarias.setText("-");
        lblResumoExtras.setText("-");
        lblValorBruto.setText("R$ 0.00");
        lblDesconto.setText("R$ 0.00");
        lblValorLiquido.setText("R$ 0.00");
        lblTotalPago.setText("R$ 0.00");
        lblSaldoDevedor.setText("R$ 0.00");
        lblStatusFatura.setText("Check-out concluído (Quarto liberado como Disponível / Sujo)");
        txtCodigoCupom.setText("");
        txtValorPagamento.setText("");
        modeloPagamentos.setRowCount(0);
    }

    @Override
    public void atualizarDados() {
        try {
            cmbReserva.removeAllItems();
            List<Reserva> ativas = reservaService.listarReservasPorStatus(usuarioLogado, StatusReserva.EM_ANDAMENTO);
            for (Reserva r : ativas) {
                String tagFatura = r.getFatura() != null ? " [Fatura: " + r.getFatura().getStatus().getDescricao() + "]" : "";
                cmbReserva.addItem(new ItemReserva(r.getIdReserva(),
                        String.format("Reserva #%d | Quarto %s | %s%s",
                                r.getIdReserva(), r.getQuarto().getNumero(),
                                r.getHospede().getNomeCompleto(), tagFatura)));
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private record ItemReserva(Integer idReserva, String rotulo) {
        @Override
        public String toString() {
            return rotulo;
        }
    }
}

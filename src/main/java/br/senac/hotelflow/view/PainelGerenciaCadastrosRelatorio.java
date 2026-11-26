package br.senac.hotelflow.view;

import br.senac.hotelflow.model.CategoriaQuarto;
import br.senac.hotelflow.model.CupomDesconto;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.service.CadastroService;
import br.senac.hotelflow.service.RelatorioOcupacaoMensal;
import br.senac.hotelflow.service.RelatorioOcupacaoMensal.OcupacaoPorQuarto;
import br.senac.hotelflow.service.RelatorioOcupacaoService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * Painel Administrativo e Analítico exclusivo do perfil {@code Gerente}:
 * gestão de Categorias e Tarifas ({@code RN04}), Quartos ({@code RN03}),
 * Cupons de Desconto ({@code RN14, D03}) e Relatório Mensal de Taxa de Ocupação ({@code RN20}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN03, RN04, RN14, RN20, D03, D04]}
 */
public class PainelGerenciaCadastrosRelatorio extends JPanel implements Atualizavel {

    private final Usuario usuarioGerente;
    private final CadastroService cadastroService;
    private final RelatorioOcupacaoService relatorioService;

    // Sub-aba 1: Tarifas e Quartos
    private List<CategoriaQuarto> categorias = new ArrayList<>();
    private final DefaultTableModel modeloCategorias = new DefaultTableModel(
            new Object[]{"ID", "Categoria", "Tarifa Diária Vigente (R$)"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable tabelaCategorias = new JTable(modeloCategorias);
    private final JTextField txtNovaTarifa = new JTextField(10);

    private final JTextField txtNumQuarto = new JTextField(8);
    private final JSpinner spnAndar = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
    private final JSpinner spnCapacidade = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
    private final JComboBox<ItemCategoria> cmbCategoriaQuarto = new JComboBox<>();
    private final DefaultTableModel modeloQuartos = new DefaultTableModel(
            new Object[]{"ID", "Número", "Andar", "Categoria", "Tarifa (R$)", "Capacidade", "Ocupação", "Limpeza"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // Sub-aba 2: Cupons de Desconto
    private List<CupomDesconto> cupons = new ArrayList<>();
    private final JTextField txtCodigoNovoCupom = new JTextField(12);
    private final JTextField txtPercentualCupom = new JTextField("10,00", 8);
    private final DefaultTableModel modeloCupons = new DefaultTableModel(
            new Object[]{"ID", "Código Promocional", "Desconto (%)", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable tabelaCupons = new JTable(modeloCupons);

    // Sub-aba 3: Relatório Mensal RN20
    private final JSpinner spnMes = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
    private final JSpinner spnAno = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2035, 1));
    private final JLabel lblResumoOcupacao = new JLabel("Selecione o mês/ano e clique em Gerar Relatório.");
    private RelatorioOcupacaoMensal ultimoRelatorio = null;
    private final DefaultTableModel modeloOcupacao = new DefaultTableModel(
            new Object[]{"Quarto Nº", "Categoria", "Diárias Ocupadas no Mês", "Taxa de Ocupação Individual (%)"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public PainelGerenciaCadastrosRelatorio(Usuario usuarioGerente, CadastroService cadastroService,
                                            RelatorioOcupacaoService relatorioService) {
        this.usuarioGerente = usuarioGerente;
        this.cadastroService = cadastroService;
        this.relatorioService = relatorioService;
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.FUNDO_CLARO);

        JTabbedPane subAbas = new JTabbedPane();
        subAbas.setFont(UiTheme.FONTE_BOTAO);
        subAbas.addTab("Quartos e Tarifas Diárias [RN03, RN04]", construirSubAbaQuartosETarifas());
        subAbas.addTab("Cupons de Desconto [RN14]", construirSubAbaCupons());
        subAbas.addTab("Relatório de Taxa de Ocupação Mensal [RN20]", construirSubAbaRelatorio());

        add(subAbas, BorderLayout.CENTER);
    }

    private JPanel construirSubAbaQuartosETarifas() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 12));
        p.setBackground(UiTheme.FUNDO_CLARO);
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Esquerda: Categorias e Tarifas [RN04]
        JPanel esq = new JPanel(new BorderLayout(8, 8));
        esq.setBackground(UiTheme.BRANCO);
        esq.setBorder(UiTheme.criarBordaTitulada("Categorias e Tarifas Diárias [RN04]"));

        UiTheme.padronizarTabela(tabelaCategorias);
        tabelaCategorias.getSelectionModel().addListSelectionListener(e -> {
            int row = tabelaCategorias.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < categorias.size()) {
                txtNovaTarifa.setText(categorias.get(row).getTarifaDiaria().toPlainString());
            }
        });
        esq.add(new JScrollPane(tabelaCategorias), BorderLayout.CENTER);

        JPanel rodapeTarifa = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        rodapeTarifa.setOpaque(false);
        rodapeTarifa.add(new JLabel("Nova Tarifa (R$):"));
        rodapeTarifa.add(txtNovaTarifa);
        JButton btnAtualizarTarifa = UiTheme.criarBotaoPrimario("Atualizar Tarifa da Categoria");
        btnAtualizarTarifa.addActionListener(e -> salvarNovaTarifaCategoria());
        rodapeTarifa.add(btnAtualizarTarifa);
        esq.add(rodapeTarifa, BorderLayout.SOUTH);

        // Direita: Cadastro de Quartos [RN03]
        JPanel dir = new JPanel(new BorderLayout(8, 8));
        dir.setBackground(UiTheme.BRANCO);
        dir.setBorder(UiTheme.criarBordaTitulada("Cadastro e Mapa de Quartos [RN03]"));

        JPanel formQto = new JPanel(new GridBagLayout());
        formQto.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; formQto.add(new JLabel("Número:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; formQto.add(txtNumQuarto, gbc);
        gbc.gridx = 2; gbc.weightx = 0; formQto.add(new JLabel("Andar:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; formQto.add(spnAndar, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; formQto.add(new JLabel("Capacidade:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; formQto.add(spnCapacidade, gbc);
        gbc.gridx = 2; gbc.weightx = 0; formQto.add(new JLabel("Categoria:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; formQto.add(cmbCategoriaQuarto, gbc);

        JButton btnNovoQuarto = UiTheme.criarBotaoSucesso("Cadastrar Novo Quarto");
        btnNovoQuarto.addActionListener(e -> cadastrarNovoQuarto());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        formQto.add(btnNovoQuarto, gbc);

        dir.add(formQto, BorderLayout.NORTH);

        JTable tabQto = new JTable(modeloQuartos);
        UiTheme.padronizarTabela(tabQto);
        dir.add(new JScrollPane(tabQto), BorderLayout.CENTER);

        p.add(esq);
        p.add(dir);
        return p;
    }

    private JPanel construirSubAbaCupons() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UiTheme.FUNDO_CLARO);
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topo.setBackground(UiTheme.BRANCO);
        topo.setBorder(UiTheme.criarBordaTitulada("Novo Cupom de Desconto Percentual [RN14, D03]"));
        topo.add(new JLabel("Código (ex.: INVERNO15):"));
        topo.add(txtCodigoNovoCupom);
        topo.add(new JLabel("Desconto (%):"));
        topo.add(txtPercentualCupom);

        JButton btnCriarCupom = UiTheme.criarBotaoSucesso("Cadastrar Cupom");
        btnCriarCupom.addActionListener(e -> cadastrarNovoCupom());
        topo.add(btnCriarCupom);

        p.add(topo, BorderLayout.NORTH);

        UiTheme.padronizarTabela(tabelaCupons);
        JScrollPane scroll = new JScrollPane(tabelaCupons);
        scroll.setBorder(UiTheme.criarBordaTitulada("Cupons Cadastrados (selecione para ativar ou desativar)"));
        p.add(scroll, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rodape.setBackground(UiTheme.BRANCO);
        JButton btnAlternar = UiTheme.criarBotaoPrimario("Ativar / Desativar Cupom Selecionado");
        btnAlternar.addActionListener(e -> alternarStatusCupomSelecionado());
        rodape.add(btnAlternar);
        p.add(rodape, BorderLayout.SOUTH);

        return p;
    }

    private JPanel construirSubAbaRelatorio() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UiTheme.FUNDO_CLARO);
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filtro.setBackground(UiTheme.BRANCO);
        filtro.setBorder(UiTheme.criarBordaTitulada("Parâmetros do Relatório Gerencial Mensal [RN20]"));
        filtro.add(new JLabel("Mês (1–12):"));
        filtro.add(spnMes);
        filtro.add(new JLabel("Ano:"));
        spnAno.setEditor(new JSpinner.NumberEditor(spnAno, "#"));
        filtro.add(spnAno);

        JButton btnGerar = UiTheme.criarBotaoPrimario("Calcular Taxa de Ocupação do Mês");
        btnGerar.addActionListener(e -> gerarRelatorioMensal());
        filtro.add(btnGerar);

        JButton btnTexto = UiTheme.criarBotaoSecundario("Exibir Relatório Textual p/ Impressão");
        btnTexto.addActionListener(e -> {
            if (ultimoRelatorio == null) {
                gerarRelatorioMensal();
            }
            if (ultimoRelatorio != null) {
                DialogoTextoModal.exibir(this, "Relatório Gerencial de Ocupação [RN20]",
                        relatorioService.formatarRelatorioTexto(ultimoRelatorio));
            }
        });
        filtro.add(btnTexto);

        p.add(filtro, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBackground(UiTheme.BRANCO);
        centro.setBorder(UiTheme.criarBordaTitulada("Indicadores Consolidados e Ocupação por Quarto"));

        lblResumoOcupacao.setFont(UiTheme.FONTE_SUBTITULO);
        lblResumoOcupacao.setForeground(UiTheme.AZUL_PRIMARIO);
        centro.add(lblResumoOcupacao, BorderLayout.NORTH);

        JTable tabRel = new JTable(modeloOcupacao);
        UiTheme.padronizarTabela(tabRel);
        centro.add(new JScrollPane(tabRel), BorderLayout.CENTER);

        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private void salvarNovaTarifaCategoria() {
        int row = tabelaCategorias.getSelectedRow();
        if (row < 0 || row >= categorias.size()) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione uma categoria na tabela à esquerda."));
            return;
        }
        try {
            CategoriaQuarto cat = categorias.get(row);
            BigDecimal novaTarifa = UiTheme.lerValorMonetario(txtNovaTarifa.getText(), "Nova Tarifa");
            cadastroService.atualizarTarifaDiaria(usuarioGerente, cat.getIdCategoria(), novaTarifa);
            atualizarDados();
            UiTheme.exibirSucesso(this, "Tarifa diária da categoria '" + cat.getNome()
                    + "' atualizada para R$ " + novaTarifa + " (faturas antigas permanecem congeladas - D06)!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void cadastrarNovoQuarto() {
        ItemCategoria selCat = (ItemCategoria) cmbCategoriaQuarto.getSelectedItem();
        if (selCat == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione a categoria do quarto."));
            return;
        }
        try {
            Quarto novo = new Quarto(
                    txtNumQuarto.getText(),
                    (Integer) spnAndar.getValue(),
                    (Integer) spnCapacidade.getValue(),
                    selCat.categoria()
            );
            cadastroService.cadastrarQuarto(usuarioGerente, novo);
            txtNumQuarto.setText("");
            atualizarDados();
            UiTheme.exibirSucesso(this, "Quarto '" + novo.getNumero() + "' cadastrado com sucesso!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void cadastrarNovoCupom() {
        try {
            BigDecimal perc = UiTheme.lerValorMonetario(txtPercentualCupom.getText(), "Percentual do Cupom");
            CupomDesconto cupom = new CupomDesconto(txtCodigoNovoCupom.getText(), perc);
            cadastroService.cadastrarCupom(usuarioGerente, cupom);
            txtCodigoNovoCupom.setText("");
            atualizarDados();
            UiTheme.exibirSucesso(this, "Cupom '" + cupom.getCodigo() + "' cadastrado com sucesso!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void alternarStatusCupomSelecionado() {
        int row = tabelaCupons.getSelectedRow();
        if (row < 0 || row >= cupons.size()) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione um cupom na tabela."));
            return;
        }
        try {
            CupomDesconto sel = cupons.get(row);
            cadastroService.alterarAtivacaoCupom(usuarioGerente, sel.getIdCupom(), !sel.isAtivo());
            atualizarDados();
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void gerarRelatorioMensal() {
        try {
            int mes = (Integer) spnMes.getValue();
            int ano = (Integer) spnAno.getValue();
            YearMonth ym = YearMonth.of(ano, mes);
            this.ultimoRelatorio = relatorioService.gerarRelatorio(usuarioGerente, ym, LocalDate.now());

            lblResumoOcupacao.setText(String.format(
                    "Competência %02d/%d  |  Quartos: %d  |  Capacidade: %d quartos-dia  |  Ocupados: %d quartos-dia  |  TAXA GLOBAL: %s%%",
                    mes, ano,
                    ultimoRelatorio.totalQuartosCadastrados(),
                    ultimoRelatorio.capacidadeTotalQuartosDia(),
                    ultimoRelatorio.totalQuartosDiaOcupados(),
                    ultimoRelatorio.taxaOcupacaoPercentual()
            ));

            modeloOcupacao.setRowCount(0);
            for (OcupacaoPorQuarto item : ultimoRelatorio.detalhamentoQuartos()) {
                modeloOcupacao.addRow(new Object[]{
                        item.numeroQuarto(),
                        item.categoria(),
                        item.diariasOcupadas() + " diária(s)",
                        item.percentualQuarto() + "%"
                });
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    @Override
    public void atualizarDados() {
        try {
            categorias = cadastroService.listarCategorias();
            modeloCategorias.setRowCount(0);
            cmbCategoriaQuarto.removeAllItems();
            for (CategoriaQuarto c : categorias) {
                modeloCategorias.addRow(new Object[]{c.getIdCategoria(), c.getNome(), c.getTarifaDiaria()});
                cmbCategoriaQuarto.addItem(new ItemCategoria(c));
            }

            modeloQuartos.setRowCount(0);
            for (Quarto q : cadastroService.listarQuartos()) {
                modeloQuartos.addRow(new Object[]{
                        q.getIdQuarto(),
                        q.getNumero(),
                        q.getAndar() + "º",
                        q.getCategoria().getNome(),
                        q.getCategoria().getTarifaDiaria(),
                        q.getCapacidadeMaxima() + " pessoa(s)",
                        q.getStatusOcupacao().getDescricao(),
                        q.getStatusLimpeza().getDescricao()
                });
            }

            cupons = cadastroService.listarCupons(usuarioGerente);
            modeloCupons.setRowCount(0);
            for (CupomDesconto cp : cupons) {
                modeloCupons.addRow(new Object[]{
                        cp.getIdCupom(),
                        cp.getCodigo(),
                        cp.getPercentual() + "%",
                        cp.isAtivo() ? "ATIVO" : "INATIVO"
                });
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private record ItemCategoria(CategoriaQuarto categoria) {
        @Override
        public String toString() {
            return categoria.getNome() + " (R$ " + categoria.getTarifaDiaria() + ")";
        }
    }
}

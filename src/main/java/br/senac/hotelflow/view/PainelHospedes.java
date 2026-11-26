package br.senac.hotelflow.view;

import br.senac.hotelflow.model.Hospede;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.service.CadastroService;
import br.senac.hotelflow.service.ReservaService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Painel de manutenção de cadastro de Hóspedes ({@code RN02}) e consulta ao Histórico de
 * Reservas Finalizadas ({@code RN19}).
 *
 * <p>Rastreabilidade: {@code [Atende RN02, RN19]}
 */
public class PainelHospedes extends JPanel implements Atualizavel {

    private final Usuario usuarioLogado;
    private final CadastroService cadastroService;
    private final ReservaService reservaService;

    private Integer idSelecionado = null;
    private final JTextField txtNome = new JTextField(24);
    private final JTextField txtDocumento = new JTextField(16);
    private final JTextField txtTelefone = new JTextField(14);
    private final JTextField txtEmail = new JTextField(22);
    private final JTextField txtEndereco = new JTextField(32);

    private List<Hospede> hospedesCarregados = new ArrayList<>();
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Nome Completo", "CPF / Passaporte", "Telefone", "E-mail", "Endereço"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);

    public PainelHospedes(Usuario usuarioLogado, CadastroService cadastroService, ReservaService reservaService) {
        this.usuarioLogado = usuarioLogado;
        this.cadastroService = cadastroService;
        this.reservaService = reservaService;
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.FUNDO_CLARO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Formulário Superior
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.BRANCO);
        form.setBorder(UiTheme.criarBordaTitulada("Cadastro e Atualização de Hóspede [RN02]"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Nome Completo:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; form.add(txtNome, gbc);

        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("CPF / Passaporte:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.6; form.add(txtDocumento, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; form.add(new JLabel("Telefone:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; form.add(txtTelefone, gbc);

        gbc.gridx = 2; gbc.weightx = 0; form.add(new JLabel("E-mail:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.8; form.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; form.add(new JLabel("Endereço:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0; form.add(txtEndereco, gbc);

        JPanel botoesForm = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botoesForm.setOpaque(false);

        JButton btnLimpar = UiTheme.criarBotaoSecundario("Novo / Limpar Campos");
        btnLimpar.addActionListener(e -> limparFormulario());

        JButton btnSalvar = UiTheme.criarBotaoSucesso("Salvar Hóspede");
        btnSalvar.addActionListener(e -> salvarOuAtualizarHospede());

        JButton btnHistorico = UiTheme.criarBotaoPrimario("Consultar Histórico Finalizado [RN19]");
        btnHistorico.addActionListener(e -> exibirHistoricoHospedeSelecionado());

        botoesForm.add(btnLimpar);
        botoesForm.add(btnSalvar);
        botoesForm.add(btnHistorico);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        form.add(botoesForm, gbc);

        add(form, BorderLayout.NORTH);

        // Tabela Central
        UiTheme.padronizarTabela(tabela);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) {
                preencherFormularioDaLinha(tabela.getSelectedRow());
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(UiTheme.criarBordaTitulada("Hóspedes Cadastrados (clique em uma linha para editar ou consultar histórico)"));
        add(scroll, BorderLayout.CENTER);
    }

    private void salvarOuAtualizarHospede() {
        try {
            if (idSelecionado == null) {
                Hospede novo = new Hospede(
                        txtNome.getText(),
                        txtDocumento.getText(),
                        txtTelefone.getText(),
                        txtEmail.getText(),
                        txtEndereco.getText()
                );
                cadastroService.cadastrarHospede(usuarioLogado, novo);
                UiTheme.exibirSucesso(this, "Hóspede '" + novo.getNomeCompleto() + "' cadastrado com sucesso!");
            } else {
                Hospede editado = new Hospede(
                        idSelecionado,
                        txtNome.getText(),
                        txtDocumento.getText(),
                        txtTelefone.getText(),
                        txtEmail.getText(),
                        txtEndereco.getText()
                );
                cadastroService.atualizarHospede(usuarioLogado, editado);
                UiTheme.exibirSucesso(this, "Dados do hóspede atualizados com sucesso!");
            }
            limparFormulario();
            atualizarDados();
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void exibirHistoricoHospedeSelecionado() {
        if (idSelecionado == null) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione um hóspede na tabela abaixo para consultar o histórico."));
            return;
        }
        try {
            List<Reserva> historico = reservaService.consultarHistoricoHospede(usuarioLogado, idSelecionado);
            StringBuilder sb = new StringBuilder();
            sb.append("====================================================================\n");
            sb.append("        HISTÓRICO DE RESERVAS FINALIZADAS DO HÓSPEDE [RN19]         \n");
            sb.append("====================================================================\n");
            sb.append("Hóspede  : ").append(txtNome.getText()).append("\n");
            sb.append("Documento: ").append(txtDocumento.getText()).append("\n");
            sb.append("Total de Hospedagens Concluídas: ").append(historico.size()).append("\n");
            sb.append("--------------------------------------------------------------------\n");

            if (historico.isEmpty()) {
                sb.append("Nenhuma reserva finalizada encontrada para este hóspede.\n");
            } else {
                for (Reserva r : historico) {
                    sb.append(String.format("• Reserva #%d | Quarto %s (%s)%n",
                            r.getIdReserva(), r.getQuarto().getNumero(), r.getQuarto().getCategoria().getNome()));
                    sb.append(String.format("  Check-in Efetivo : %s%n",
                            r.getDataHoraCheckIn() != null ? r.getDataHoraCheckIn().format(UiTheme.DATA_HORA_BR) : "-"));
                    sb.append(String.format("  Check-out Efetivo: %s%n",
                            r.getDataHoraCheckOut() != null ? r.getDataHoraCheckOut().format(UiTheme.DATA_HORA_BR) : "-"));
                    if (r.getFatura() != null) {
                        sb.append(String.format("  Fatura Quitada   : Bruto R$ %s | Desconto R$ %s | Líquido R$ %s%n",
                                r.getFatura().calcularValorBruto(),
                                r.getFatura().getValorDesconto(),
                                r.getFatura().calcularValorLiquido()));
                    }
                    sb.append("--------------------------------------------------------------------\n");
                }
            }
            DialogoTextoModal.exibir(this, "Histórico do Hóspede - " + txtNome.getText(), sb.toString());
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    private void preencherFormularioDaLinha(int linha) {
        if (linha >= 0 && linha < hospedesCarregados.size()) {
            Hospede h = hospedesCarregados.get(linha);
            this.idSelecionado = h.getIdHospede();
            txtNome.setText(h.getNomeCompleto());
            txtDocumento.setText(h.getDocumento());
            txtTelefone.setText(h.getTelefone());
            txtEmail.setText(h.getEmail());
            txtEndereco.setText(h.getEndereco());
        }
    }

    private void limparFormulario() {
        this.idSelecionado = null;
        txtNome.setText("");
        txtDocumento.setText("");
        txtTelefone.setText("");
        txtEmail.setText("");
        txtEndereco.setText("");
        tabela.clearSelection();
    }

    @Override
    public void atualizarDados() {
        try {
            hospedesCarregados = cadastroService.listarHospedes(usuarioLogado);
            modeloTabela.setRowCount(0);
            for (Hospede h : hospedesCarregados) {
                modeloTabela.addRow(new Object[]{
                        h.getIdHospede(),
                        h.getNomeCompleto(),
                        h.getDocumento(),
                        h.getTelefone(),
                        h.getEmail(),
                        h.getEndereco()
                });
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }
}

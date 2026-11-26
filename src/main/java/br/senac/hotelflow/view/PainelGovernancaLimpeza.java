package br.senac.hotelflow.view;

import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.service.HospedagemService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Painel operacional de Governança exclusivo do perfil {@code Camareira} para inspeção e
 * atualização do status de limpeza dos quartos ({@code Sujo}, {@code Em Limpeza}, {@code Limpo}).
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN12, D01]}
 */
public class PainelGovernancaLimpeza extends JPanel implements Atualizavel {

    private final Usuario usuarioCamareira;
    private final HospedagemService hospedagemService;

    private final JComboBox<String> cmbFiltro = new JComboBox<>(new String[]{
            "Todos os Quartos", "Apenas SUJOS (Prioridade Pós-Check-out)", "Apenas EM LIMPEZA", "Apenas LIMPOS"
    });

    private List<Quarto> quartosExibidos = new ArrayList<>();
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Quarto Nº", "Andar", "Categoria", "Ocupação Física", "Status de Limpeza", "Apto p/ Check-in?"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);

    public PainelGovernancaLimpeza(Usuario usuarioCamareira, HospedagemService hospedagemService) {
        this.usuarioCamareira = usuarioCamareira;
        this.hospedagemService = hospedagemService;
        construirInterface();
    }

    private void construirInterface() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiTheme.FUNDO_CLARO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Barra Superior de Filtro
        JPanel barraTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        barraTopo.setBackground(UiTheme.BRANCO);
        barraTopo.setBorder(UiTheme.criarBordaTitulada("Filtro de Governança Hoteleira [RN12]"));
        barraTopo.add(new JLabel("Exibir:"));
        cmbFiltro.setFont(UiTheme.FONTE_PADRAO);
        cmbFiltro.addActionListener(e -> atualizarDados());
        barraTopo.add(cmbFiltro);

        JButton btnRecarregar = UiTheme.criarBotaoSecundario("Recarregar Lista");
        btnRecarregar.addActionListener(e -> atualizarDados());
        barraTopo.add(btnRecarregar);

        add(barraTopo, BorderLayout.NORTH);

        // Tabela Central
        UiTheme.padronizarTabela(tabela);
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(UiTheme.criarBordaTitulada("Mapa de Higienização dos Quartos (selecione um quarto abaixo)"));
        add(scroll, BorderLayout.CENTER);

        // Botões de Transição de Estado de Limpeza
        JPanel barraAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        barraAcoes.setBackground(UiTheme.BRANCO);
        barraAcoes.setBorder(UiTheme.criarBordaTitulada("Atualizar Status de Limpeza do Quarto Selecionado [RN12]"));

        JButton btnSujo = UiTheme.criarBotaoPerigo("Marcar como SUJO");
        btnSujo.addActionListener(e -> alterarLimpezaSelecionado(StatusLimpeza.SUJO));

        JButton btnEmLimpeza = UiTheme.criarBotaoPrimario("Iniciar Higienização (EM LIMPEZA)");
        btnEmLimpeza.addActionListener(e -> alterarLimpezaSelecionado(StatusLimpeza.EM_LIMPEZA));

        JButton btnLimpo = UiTheme.criarBotaoSucesso("Concluir Higienização (LIMPO)");
        btnLimpo.addActionListener(e -> alterarLimpezaSelecionado(StatusLimpeza.LIMPO));

        barraAcoes.add(btnSujo);
        barraAcoes.add(btnEmLimpeza);
        barraAcoes.add(btnLimpo);

        add(barraAcoes, BorderLayout.SOUTH);
    }

    private void alterarLimpezaSelecionado(StatusLimpeza novoStatus) {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= quartosExibidos.size()) {
            UiTheme.exibirErro(this, new IllegalArgumentException("Selecione um quarto na tabela para atualizar o status de limpeza."));
            return;
        }
        try {
            Quarto selecionado = quartosExibidos.get(linha);
            Quarto atualizado = hospedagemService.atualizarLimpezaQuarto(
                    usuarioCamareira, selecionado.getIdQuarto(), novoStatus);
            atualizarDados();
            UiTheme.exibirSucesso(this, "Quarto " + atualizado.getNumero()
                    + " atualizado para '" + atualizado.getStatusLimpeza().getDescricao() + "'!");
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }

    @Override
    public void atualizarDados() {
        try {
            List<Quarto> todos = hospedagemService.listarQuartosParaGovernanca(usuarioCamareira);
            int filtro = cmbFiltro.getSelectedIndex();
            quartosExibidos = new ArrayList<>();
            modeloTabela.setRowCount(0);

            for (Quarto q : todos) {
                boolean incluir = switch (filtro) {
                    case 1 -> q.getStatusLimpeza() == StatusLimpeza.SUJO;
                    case 2 -> q.getStatusLimpeza() == StatusLimpeza.EM_LIMPEZA;
                    case 3 -> q.getStatusLimpeza() == StatusLimpeza.LIMPO;
                    default -> true;
                };
                if (incluir) {
                    quartosExibidos.add(q);
                    modeloTabela.addRow(new Object[]{
                            q.getIdQuarto(),
                            q.getNumero(),
                            q.getAndar() + "º andar",
                            q.getCategoria().getNome(),
                            q.getStatusOcupacao().getDescricao(),
                            q.getStatusLimpeza().getDescricao(),
                            q.estaAptoParaCheckIn() ? "SIM (Disponível e Limpo)" : "NÃO"
                    });
                }
            }
        } catch (Exception ex) {
            UiTheme.exibirErro(this, ex);
        }
    }
}

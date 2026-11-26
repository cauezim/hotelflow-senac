package br.senac.hotelflow.service;

import br.senac.hotelflow.dao.QuartoDAO;
import br.senac.hotelflow.dao.ReservaDAO;
import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.Quarto;
import br.senac.hotelflow.model.Reserva;
import br.senac.hotelflow.model.Usuario;
import br.senac.hotelflow.service.RelatorioOcupacaoMensal.OcupacaoPorQuarto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço gerencial exclusivo do perfil {@code GERENTE} para cálculo da Taxa de Ocupação Mensal,
 * tratando corretamente estadias que atravessam meses, estadias {@code EM_ANDAMENTO} (sem projetar
 * dias futuros) e estadias day-use de mesmo dia.
 *
 * <p>Rastreabilidade: {@code [Atende RN01, RN20, D04, D05]}
 */
public class RelatorioOcupacaoService {

    private static final BigDecimal CEM = new BigDecimal("100.00");
    private static final DateTimeFormatter MES_ANO_BR = DateTimeFormatter.ofPattern("MM/yyyy");

    private final QuartoDAO quartoDAO;
    private final ReservaDAO reservaDAO;

    public RelatorioOcupacaoService() {
        this(new QuartoDAO(), new ReservaDAO());
    }

    public RelatorioOcupacaoService(QuartoDAO quartoDAO, ReservaDAO reservaDAO) {
        this.quartoDAO = quartoDAO;
        this.reservaDAO = reservaDAO;
    }

    /**
     * Gera a apuração completa de ocupação hoteleira em um mês específico.
     *
     * <p>Rastreabilidade: {@code [Atende RN20, D04, D05]}
     *
     * @param operadorGerente    usuário autenticado (exige permissão {@code GERAR_RELATORIO_OCUPACAO})
     * @param competencia        mês/ano desejado (ex.: {@code YearMonth.of(2026, 11)})
     * @param dataReferenciaHoje data atual de corte para estadias ainda {@code EM_ANDAMENTO}
     */
    public RelatorioOcupacaoMensal gerarRelatorio(Usuario operadorGerente,
                                                  YearMonth competencia,
                                                  LocalDate dataReferenciaHoje) {
        AutenticacaoService.exigirPermissao(operadorGerente, "GERAR_RELATORIO_OCUPACAO");
        if (competencia == null) {
            throw new RegraNegocioException("Informe o mês e o ano de referência para o relatório de ocupação.");
        }

        LocalDate hoje = dataReferenciaHoje != null ? dataReferenciaHoje : LocalDate.now();
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMesExclusivo = competencia.plusMonths(1).atDay(1);

        List<Quarto> quartos = quartoDAO.listarTodos();
        List<Reserva> estadias = reservaDAO.listarEstadiasEfetivasNoPeriodo(inicioMes, fimMesExclusivo);

        return calcularOcupacaoEmMemoria(quartos, estadias, competencia, hoje);
    }

    /**
     * Motor puro de cálculo matemático de ocupação mensal (desacoplado de SQL para verificação
     * determinística em testes automatizados JUnit 5 com cenários de virada de mês e estadias abertas).
     */
    public RelatorioOcupacaoMensal calcularOcupacaoEmMemoria(List<Quarto> quartos,
                                                             List<Reserva> estadias,
                                                             YearMonth competencia,
                                                             LocalDate hoje) {
        int diasNoMes = competencia.lengthOfMonth();
        int totalQuartos = quartos.size();
        long capacidadeTotal = (long) totalQuartos * diasNoMes;

        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMesExclusivo = competencia.plusMonths(1).atDay(1);
        LocalDate limiteEmAndamento = hoje.isBefore(fimMesExclusivo) ? hoje.plusDays(0) : fimMesExclusivo;

        Map<Integer, Long> diariasPorQuarto = new HashMap<>();
        for (Quarto q : quartos) {
            diariasPorQuarto.put(q.getIdQuarto(), 0L);
        }

        for (Reserva r : estadias) {
            if (r.getDataHoraCheckIn() == null || r.getQuarto() == null) {
                continue;
            }
            LocalDate entradaReal = r.getDataHoraCheckIn().toLocalDate();
            LocalDate saidaReal;
            if (r.getDataHoraCheckOut() != null) {
                saidaReal = r.getDataHoraCheckOut().toLocalDate();
                if (saidaReal.equals(entradaReal)) {
                    // Day-use encerrado no mesmo dia contabiliza 1 diária efetiva naquele dia
                    saidaReal = entradaReal.plusDays(1);
                }
            } else {
                // Reserva EM_ANDAMENTO: corta no limite de hoje (ou fim do mês se mês passado)
                if (hoje.isBefore(entradaReal)) {
                    continue;
                }
                saidaReal = limiteEmAndamento.equals(entradaReal)
                        ? entradaReal.plusDays(1)
                        : limiteEmAndamento;
                if (saidaReal.isAfter(fimMesExclusivo)) {
                    saidaReal = fimMesExclusivo;
                }
            }

            LocalDate iniRecorte = entradaReal.isAfter(inicioMes) ? entradaReal : inicioMes;
            LocalDate fimRecorte = saidaReal.isBefore(fimMesExclusivo) ? saidaReal : fimMesExclusivo;

            long diasOcupadosNoMes = ChronoUnit.DAYS.between(iniRecorte, fimRecorte);
            if (diasOcupadosNoMes > 0) {
                Integer idQuarto = r.getQuarto().getIdQuarto();
                long acumulado = diariasPorQuarto.getOrDefault(idQuarto, 0L) + diasOcupadosNoMes;
                diariasPorQuarto.put(idQuarto, Math.min(diasNoMes, acumulado));
            }
        }

        long somaOcupados = 0L;
        List<OcupacaoPorQuarto> detalhamento = new ArrayList<>();
        for (Quarto q : quartos) {
            long ocupadosQuarto = diariasPorQuarto.getOrDefault(q.getIdQuarto(), 0L);
            somaOcupados += ocupadosQuarto;
            BigDecimal percQuarto = diasNoMes > 0
                    ? BigDecimal.valueOf(ocupadosQuarto).multiply(CEM)
                            .divide(BigDecimal.valueOf(diasNoMes), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            detalhamento.add(new OcupacaoPorQuarto(
                    q.getNumero(),
                    q.getCategoria().getNome(),
                    ocupadosQuarto,
                    percQuarto
            ));
        }

        BigDecimal taxaGlobal = capacidadeTotal > 0
                ? BigDecimal.valueOf(somaOcupados).multiply(CEM)
                        .divide(BigDecimal.valueOf(capacidadeTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new RelatorioOcupacaoMensal(
                competencia,
                totalQuartos,
                diasNoMes,
                capacidadeTotal,
                somaOcupados,
                taxaGlobal,
                detalhamento
        );
    }

    /**
     * Formata o relatório gerencial em layout textual tabular pronto para exibição.
     */
    public String formatarRelatorioTexto(RelatorioOcupacaoMensal rel) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("       HOTELFLOW - RELATÓRIO GERENCIAL DE OCUPAÇÃO MENSAL           \n");
        sb.append("====================================================================\n");
        sb.append(String.format("Competência Analisada        : %s (%d dias)%n",
                rel.competencia().format(MES_ANO_BR), rel.diasNoMesConsiderados()));
        sb.append(String.format("Total de Quartos Cadastrados : %d%n", rel.totalQuartosCadastrados()));
        sb.append(String.format("Capacidade Instalada no Mês  : %d quartos-dia%n", rel.capacidadeTotalQuartosDia()));
        sb.append(String.format("Quartos-Dia Ocupados         : %d quartos-dia%n", rel.totalQuartosDiaOcupados()));
        sb.append(String.format("TAXA DE OCUPAÇÃO DO PERÍODO  : %s%%%n", rel.taxaOcupacaoPercentual()));
        sb.append("--------------------------------------------------------------------\n");
        sb.append("Quarto  | Categoria    | Diárias Ocupadas | Taxa Individual\n");
        sb.append("--------------------------------------------------------------------\n");
        for (OcupacaoPorQuarto item : rel.detalhamentoQuartos()) {
            sb.append(String.format("%-7s | %-12s | %16d | %13s%%%n",
                    item.numeroQuarto(),
                    item.categoria(),
                    item.diariasOcupadas(),
                    item.percentualQuarto()));
        }
        sb.append("====================================================================");
        return sb.toString();
    }
}

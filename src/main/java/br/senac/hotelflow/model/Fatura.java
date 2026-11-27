package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.StatusFatura;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Documento financeiro consolidado no Check-out, responsável por congelar os valores históricos
 * da estadia ({@code D06}), aplicar desconto percentual gerencial ({@code RN14}) e controlar
 * amortizações únicas ou fracionadas ({@code RN15, RN16}).
 *
 * <p>Rastreabilidade: {@code [Atende RN13..RN18, D06]}
 */
public class Fatura {

    private Integer idFatura;
    private final Reserva reserva;
    private CupomDesconto cupom;
    private final LocalDateTime dataEmissao;
    private long quantidadeDiarias;
    private final BigDecimal tarifaDiariaAplicada;
    private BigDecimal valorBruto;
    private BigDecimal percentualDesconto;
    private BigDecimal valorDesconto;
    private StatusFatura status;
    private final List<Pagamento> pagamentos = new ArrayList<>();

    /**
     * Construtor de negócio para emissão inicial da fatura no Check-out, congelando diárias,
     * tarifa vigente e valor bruto.
     *
     * <p>Rastreabilidade: {@code [Atende RN04, RN10, RN11, RN13, D05, D06]}
     */
    public Fatura(Reserva reserva, LocalDateTime dataEmissao) {
        if (reserva == null) {
            throw new RegraNegocioException("A reserva vinculada à fatura é obrigatória.");
        }
        if (reserva.getStatus() != StatusReserva.EM_ANDAMENTO) {
            throw new RegraNegocioException("A fatura só pode ser gerada para uma reserva Em Andamento.");
        }
        this.reserva = reserva;
        this.dataEmissao = dataEmissao != null ? dataEmissao : LocalDateTime.now();
        this.quantidadeDiarias = reserva.calcularDiarias(this.dataEmissao.toLocalDate());
        this.tarifaDiariaAplicada = reserva.getQuarto().getCategoria().getTarifaDiaria()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal valorDiarias = tarifaDiariaAplicada
                .multiply(BigDecimal.valueOf(quantidadeDiarias))
                .setScale(2, RoundingMode.HALF_UP);
        this.valorBruto = valorDiarias
                .add(reserva.calcularTotalConsumos())
                .add(reserva.calcularTotalServicos())
                .setScale(2, RoundingMode.HALF_UP);

        this.percentualDesconto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.valorDesconto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.status = StatusFatura.AGUARDANDO_PAGAMENTO;

        this.reserva.vincularFaturaInterna(this);
    }

    /**
     * Construtor completo para hidratação fiel dos valores históricos congelados no banco ({@code FaturaDAO}).
     */
    public Fatura(Integer idFatura, Reserva reserva, CupomDesconto cupom, LocalDateTime dataEmissao,
                  long quantidadeDiarias, BigDecimal tarifaDiariaAplicada, BigDecimal valorBruto,
                  BigDecimal percentualDesconto, BigDecimal valorDesconto, StatusFatura status) {
        if (reserva == null) {
            throw new RegraNegocioException("A reserva da fatura é obrigatória.");
        }
        this.idFatura = idFatura;
        this.reserva = reserva;
        this.cupom = cupom;
        this.dataEmissao = dataEmissao != null ? dataEmissao : LocalDateTime.now();
        this.quantidadeDiarias = quantidadeDiarias;
        this.tarifaDiariaAplicada = tarifaDiariaAplicada.setScale(2, RoundingMode.HALF_UP);
        this.valorBruto = valorBruto.setScale(2, RoundingMode.HALF_UP);
        this.percentualDesconto = percentualDesconto.setScale(2, RoundingMode.HALF_UP);
        this.valorDesconto = valorDesconto.setScale(2, RoundingMode.HALF_UP);
        this.status = status;

        this.reserva.vincularFaturaInterna(this);
    }

    /**
     * Retorna o valor bruto consolidado da fatura.
     *
     * <p>Rastreabilidade: {@code [Atende RN13, D06]}
     */
    public BigDecimal calcularValorBruto() {
        return valorBruto;
    }

    /**
     * Caso o hóspede permaneça no quarto em dias adicionais após abertura/pagamento parcial da fatura,
     * atualiza a contagem de diárias e o valor bruto preservando a tarifa congelada e os pagamentos já feitos.
     *
     * <p>Rastreabilidade: {@code [Atende RN13, RN16, D05, D06]}
     */
    public boolean atualizarSeProrrogada( java.time.LocalDate dataSaidaAtual) {
        if (dataSaidaAtual == null || estaQuitada()) {
            return false;
        }
        long novasDiarias = reserva.calcularDiarias(dataSaidaAtual);
        if (novasDiarias <= this.quantidadeDiarias) {
            return false;
        }
        this.quantidadeDiarias = novasDiarias;
        BigDecimal novoSubtotalDiarias = tarifaDiariaAplicada
                .multiply(BigDecimal.valueOf(novasDiarias))
                .setScale(2, RoundingMode.HALF_UP);
        this.valorBruto = novoSubtotalDiarias
                .add(reserva.calcularTotalConsumos())
                .add(reserva.calcularTotalServicos())
                .setScale(2, RoundingMode.HALF_UP);

        if (this.percentualDesconto.compareTo(BigDecimal.ZERO) > 0) {
            this.valorDesconto = this.valorBruto
                    .multiply(this.percentualDesconto)
                    .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
        }
        return true;
    }

    /**
     * Aplica um cupom de desconto válido exclusivamente enquanto nenhum pagamento parcial foi iniciado ({@code D06}).
     *
     * <p>Rastreabilidade: {@code [Atende RN14, D03, D06]}
     */
    public void aplicarDesconto(CupomDesconto cupom) {
        if (cupom == null) {
            throw new RegraNegocioException("Informe um cupom de desconto válido.");
        }
        if (this.status != StatusFatura.AGUARDANDO_PAGAMENTO
                || calcularTotalPago().compareTo(BigDecimal.ZERO) > 0) {
            throw new RegraNegocioException(
                    "Não é permitido aplicar ou alterar cupom de desconto após o início dos pagamentos.");
        }
        BigDecimal descontoCalculado = cupom.calcularDesconto(this.valorBruto);
        if (descontoCalculado.compareTo(this.valorBruto) >= 0) {
            throw new RegraNegocioException("O desconto não pode ser igual ou superior ao valor bruto da fatura.");
        }
        this.cupom = cupom;
        this.percentualDesconto = cupom.getPercentual();
        this.valorDesconto = descontoCalculado;
    }

    /**
     * Calcula o valor líquido devido ({@code valorBruto - valorDesconto}).
     */
    public BigDecimal calcularValorLiquido() {
        return valorBruto.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Registra uma parcela de pagamento, atualizando automaticamente o status para
     * {@code PARCIALMENTE_PAGA} ou {@code PAGA}.
     *
     * <p>Rastreabilidade: {@code [Atende RN15, RN16]}
     */
    public void registrarPagamento(Pagamento pagamento) {
        if (pagamento == null) {
            throw new RegraNegocioException("O registro de pagamento não pode ser nulo.");
        }
        if (estaQuitada()) {
            throw new RegraNegocioException("Esta fatura já se encontra integralmente quitada.");
        }
        BigDecimal saldoAtual = calcularSaldoDevedor();
        if (pagamento.getValor().compareTo(saldoAtual) > 0) {
            throw new RegraNegocioException(
                    "O valor do pagamento (R$ " + pagamento.getValor()
                    + ") excede o saldo devedor restante (R$ " + saldoAtual + ").");
        }
        this.pagamentos.add(pagamento);

        if (calcularSaldoDevedor().compareTo(BigDecimal.ZERO) == 0) {
            this.status = StatusFatura.PAGA;
        } else {
            this.status = StatusFatura.PARCIALMENTE_PAGA;
        }
    }

    /**
     * Hidrata um pagamento já persistido no banco de dados durante a reconstrução pelo DAO.
     */
    public void hidratarPagamento(Pagamento pagamento) {
        if (pagamento != null) {
            this.pagamentos.add(pagamento);
        }
    }

    /**
     * Soma todas as parcelas de pagamento registradas na fatura.
     *
     * <p>Rastreabilidade: {@code [Atende RN15, RN16]}
     */
    public BigDecimal calcularTotalPago() {
        BigDecimal total = BigDecimal.ZERO;
        for (Pagamento p : this.pagamentos) {
            total = total.add(p.getValor());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o saldo restante para quitação ({@code calcularValorLiquido() - calcularTotalPago()}).
     *
     * <p>Rastreabilidade: {@code [Atende RN16]}
     */
    public BigDecimal calcularSaldoDevedor() {
        return calcularValorLiquido()
                .subtract(calcularTotalPago())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Indica se a fatura atingiu quitação integral ({@code saldoDevedor == 0,00} e {@code status == PAGA}).
     *
     * <p>Rastreabilidade: {@code [Atende RN16, RN17, RN18]}
     */
    public boolean estaQuitada() {
        return this.status == StatusFatura.PAGA
                && calcularSaldoDevedor().compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal calcularSubtotalDiarias() {
        return tarifaDiariaAplicada
                .multiply(BigDecimal.valueOf(quantidadeDiarias))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdFatura() {
        return idFatura;
    }

    public void setIdFatura(Integer idFatura) {
        this.idFatura = idFatura;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public CupomDesconto getCupom() {
        return cupom;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public long getQuantidadeDiarias() {
        return quantidadeDiarias;
    }

    public BigDecimal getTarifaDiariaAplicada() {
        return tarifaDiariaAplicada;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public StatusFatura getStatus() {
        return status;
    }

    public List<Pagamento> getPagamentos() {
        return Collections.unmodifiableList(pagamentos);
    }

    @Override
    public String toString() {
        return "Fatura{id=" + idFatura + ", bruto=R$ " + valorBruto
                + ", liquido=R$ " + calcularValorLiquido()
                + ", saldo=R$ " + calcularSaldoDevedor()
                + ", status=" + status + "}";
    }
}

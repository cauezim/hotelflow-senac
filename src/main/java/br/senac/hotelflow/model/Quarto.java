package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.StatusLimpeza;
import br.senac.hotelflow.model.enums.StatusOcupacao;
import java.util.Objects;

/**
 * Unidade física habitacional do hotel modelada com dois eixos ortogonais independentes de estado:
 * {@link StatusOcupacao} e {@link StatusLimpeza}.
 *
 * <p>Rastreabilidade: {@code [Atende RN03, RN09, RN12, RN17, D01]}
 */
public class Quarto {

    private Integer idQuarto;
    private String numero;
    private int andar;
    private int capacidadeMaxima;
    private CategoriaQuarto categoria;
    private StatusOcupacao statusOcupacao;
    private StatusLimpeza statusLimpeza;

    public Quarto(String numero, int andar, int capacidadeMaxima, CategoriaQuarto categoria) {
        this(null, numero, andar, capacidadeMaxima, categoria, StatusOcupacao.DISPONIVEL, StatusLimpeza.LIMPO);
    }

    public Quarto(Integer idQuarto, String numero, int andar, int capacidadeMaxima,
                  CategoriaQuarto categoria, StatusOcupacao statusOcupacao, StatusLimpeza statusLimpeza) {
        this.idQuarto = idQuarto;
        setNumero(numero);
        setAndar(andar);
        setCapacidadeMaxima(capacidadeMaxima);
        setCategoria(categoria);
        if (statusOcupacao == null) {
            throw new RegraNegocioException("O status de ocupação do quarto é obrigatório.");
        }
        if (statusLimpeza == null) {
            throw new RegraNegocioException("O status de limpeza do quarto é obrigatório.");
        }
        this.statusOcupacao = statusOcupacao;
        this.statusLimpeza = statusLimpeza;
    }

    /**
     * Verifica se o quarto atende simultaneamente aos dois eixos exigidos para check-in ({@code D01}):
     * {@code DISPONIVEL} e {@code LIMPO}.
     *
     * <p>Rastreabilidade: {@code [Atende RN09, RN12, D01]}
     */
    public boolean estaAptoParaCheckIn() {
        return this.statusOcupacao == StatusOcupacao.DISPONIVEL
                && this.statusLimpeza == StatusLimpeza.LIMPO;
    }

    /**
     * Ocupa o quarto na efetivação do Check-in, bloqueando caso esteja ocupado ou pendente de limpeza.
     *
     * <p>Rastreabilidade: {@code [Atende RN09, D01]}
     */
    public void ocupar() {
        if (!estaAptoParaCheckIn()) {
            throw new RegraNegocioException(
                    "Quarto " + numero + " não está apto para Check-in (Ocupação: "
                    + statusOcupacao.getDescricao() + ", Limpeza: " + statusLimpeza.getDescricao() + ").");
        }
        this.statusOcupacao = StatusOcupacao.OCUPADO;
    }

    /**
     * Libera a ocupação do quarto imediatamente após quitação total no Check-out.
     *
     * <p>Rastreabilidade: {@code [Atende RN17]}
     */
    public void liberar() {
        this.statusOcupacao = StatusOcupacao.DISPONIVEL;
    }

    /**
     * Atualiza o estado de governança/higienização do quarto.
     *
     * <p>Rastreabilidade: {@code [Atende RN12, RN17]}
     */
    public void atualizarLimpeza(StatusLimpeza novoStatus) {
        if (novoStatus == null) {
            throw new RegraNegocioException("Informe um status de limpeza válido.");
        }
        this.statusLimpeza = novoStatus;
    }

    public Integer getIdQuarto() {
        return idQuarto;
    }

    public void setIdQuarto(Integer idQuarto) {
        this.idQuarto = idQuarto;
    }

    public String getNumero() {
        return numero;
    }

    public final void setNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new RegraNegocioException("O número do quarto é obrigatório.");
        }
        this.numero = numero.trim();
    }

    public int getAndar() {
        return andar;
    }

    public final void setAndar(int andar) {
        if (andar < 0) {
            throw new RegraNegocioException("O andar do quarto não pode ser negativo.");
        }
        this.andar = andar;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public final void setCapacidadeMaxima(int capacidadeMaxima) {
        if (capacidadeMaxima <= 0) {
            throw new RegraNegocioException("A capacidade máxima do quarto deve ser maior que zero.");
        }
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public CategoriaQuarto getCategoria() {
        return categoria;
    }

    public final void setCategoria(CategoriaQuarto categoria) {
        if (categoria == null) {
            throw new RegraNegocioException("A categoria do quarto é obrigatória.");
        }
        this.categoria = categoria;
    }

    public StatusOcupacao getStatusOcupacao() {
        return statusOcupacao;
    }

    public StatusLimpeza getStatusLimpeza() {
        return statusLimpeza;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quarto that)) return false;
        return Objects.equals(numero, that.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Quarto{numero='" + numero + "', categoria=" + categoria.getNome()
                + ", ocupacao=" + statusOcupacao + ", limpeza=" + statusLimpeza + "}";
    }
}

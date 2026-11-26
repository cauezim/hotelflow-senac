package br.senac.hotelflow.model;

import br.senac.hotelflow.exception.RegraNegocioException;
import br.senac.hotelflow.model.enums.StatusReserva;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidade de domínio que representa o cliente titular de reservas no hotel (identificado
 * por CPF ou Passaporte) e mantém acesso ao histórico de estadias finalizadas.
 *
 * <p>Rastreabilidade: {@code [Atende RN02, RN19]}
 */
public class Hospede {

    private Integer idHospede;
    private String nomeCompleto;
    private String documento;
    private String telefone;
    private String email;
    private String endereco;
    private final List<Reserva> reservas = new ArrayList<>();

    public Hospede(String nomeCompleto, String documento, String telefone, String email, String endereco) {
        this(null, nomeCompleto, documento, telefone, email, endereco);
    }

    public Hospede(Integer idHospede, String nomeCompleto, String documento,
                   String telefone, String email, String endereco) {
        this.idHospede = idHospede;
        setNomeCompleto(nomeCompleto);
        setDocumento(documento);
        setTelefone(telefone);
        setEmail(email);
        setEndereco(endereco);
    }

    /**
     * Retorna exclusivamente as reservas vinculadas ao hóspede cujo status seja {@code FINALIZADA}.
     *
     * <p>Rastreabilidade: {@code [Atende RN19]}
     *
     * @return lista imutável de reservas finalizadas
     */
    public List<Reserva> consultarHistorico() {
        return reservas.stream()
                .filter(r -> r.getStatus() == StatusReserva.FINALIZADA)
                .toList();
    }

    /**
     * Associa uma reserva à coleção em memória do hóspede evitando duplicidade.
     */
    public void vincularReserva(Reserva reserva) {
        if (reserva != null && !reservas.contains(reserva)) {
            reservas.add(reserva);
        }
    }

    public List<Reserva> getReservas() {
        return Collections.unmodifiableList(reservas);
    }

    public Integer getIdHospede() {
        return idHospede;
    }

    public void setIdHospede(Integer idHospede) {
        this.idHospede = idHospede;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public final void setNomeCompleto(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new RegraNegocioException("O nome completo do hóspede é obrigatório.");
        }
        this.nomeCompleto = nomeCompleto.trim();
    }

    public String getDocumento() {
        return documento;
    }

    /**
     * Padroniza CPF ou Passaporte removendo caracteres de formatação (pontos, traços, barras e espaços)
     * e convertendo letras para maiúsculas, evitando duplicidade de cadastro por máscara.
     *
     * <p>Rastreabilidade: {@code [Atende RN02]}
     */
    public static String normalizarDocumento(String documento) {
        if (documento == null) {
            throw new RegraNegocioException("O CPF ou Passaporte do hóspede é obrigatório.");
        }
        String normalizado = documento.replaceAll("[.\\-/\\s]", "").toUpperCase();
        if (normalizado.isBlank()) {
            throw new RegraNegocioException("O CPF ou Passaporte informado é inválido.");
        }
        return normalizado;
    }

    public final void setDocumento(String documento) {
        this.documento = normalizarDocumento(documento);
    }

    public String getTelefone() {
        return telefone;
    }

    public final void setTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new RegraNegocioException("O telefone do hóspede é obrigatório.");
        }
        this.telefone = telefone.trim();
    }

    public String getEmail() {
        return email;
    }

    public final void setEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new RegraNegocioException("Informe um endereço de e-mail válido para o hóspede.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getEndereco() {
        return endereco;
    }

    public final void setEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) {
            throw new RegraNegocioException("O endereço do hóspede é obrigatório.");
        }
        this.endereco = endereco.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hospede that)) return false;
        return Objects.equals(documento, that.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documento);
    }

    @Override
    public String toString() {
        return "Hospede{id=" + idHospede + ", nome='" + nomeCompleto + "', doc='" + documento + "'}";
    }
}

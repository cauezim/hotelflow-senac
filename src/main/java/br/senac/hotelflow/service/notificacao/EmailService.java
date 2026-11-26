package br.senac.hotelflow.service.notificacao;

/**
 * Porta de saída desacoplada (Princípio da Inversão de Dependência - SOLID) para envio
 * automático de e-mail de confirmação de reserva ao hóspede.
 *
 * <p>Permite alternar entre simulação local homologável em console/log e provedor SMTP real,
 * além de viabilizar dublês de teste (Mock/Spy) nos testes automatizados JUnit 5 (Etapa E11).
 *
 * <p>Rastreabilidade: {@code [Atende RN08]}
 */
public interface EmailService {

    /**
     * Envia mensagem eletrônica de confirmação quando a reserva transita para {@code CONFIRMADA}.
     *
     * @param destinatario endereço de e-mail do hóspede titular
     * @param assunto      título institucional da confirmação
     * @param corpoTexto   conteúdo detalhado com número da reserva, quarto e datas previstas
     */
    void enviarConfirmacaoReserva(String destinatario, String assunto, String corpoTexto);
}

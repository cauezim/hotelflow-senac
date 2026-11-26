package br.senac.hotelflow.service.notificacao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementação padrão de {@link EmailService} para execução acadêmica local sem dependência
 * de credenciais SMTP externas, registrando o disparo formatado em saída auditável.
 *
 * <p>Rastreabilidade: {@code [Atende RN08]}
 */
public class ConsoleEmailService implements EmailService {

    private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void enviarConfirmacaoReserva(String destinatario, String assunto, String corpoTexto) {
        System.out.println("==================================================================");
        System.out.println("[SERVICO DE E-MAIL AUTOMATICO - RN08] Disparo em: " + LocalDateTime.now().format(FORMATADOR));
        System.out.println("Para   : " + destinatario);
        System.out.println("Assunto: " + assunto);
        System.out.println("------------------------------------------------------------------");
        System.out.println(corpoTexto);
        System.out.println("==================================================================");
    }
}

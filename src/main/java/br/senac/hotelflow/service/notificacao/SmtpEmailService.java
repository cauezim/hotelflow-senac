package br.senac.hotelflow.service.notificacao;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Implementação de {@link EmailService} com disparo real via protocolo SMTP (Jakarta Mail)
 * e fallback automático para {@link ConsoleEmailService} caso o ambiente acadêmico esteja
 * offline ou sem credenciais SMTP configuradas em {@code db.properties}.
 *
 * <p>Justificativa Didática: aplica Polimorfismo por Interface, desacoplando o serviço de
 * reservas do meio físico de entrega da mensagem.
 *
 * <p>Rastreabilidade: {@code [Atende RN08]}
 */
public class SmtpEmailService implements EmailService {

    private static final String ARQUIVO_LOCAL = "/db.properties";
    private static final String ARQUIVO_EXEMPLO = "/db.properties.example";
    private static final String TIMEOUT_MS = "5000";

    private final String host;
    private final String porta;
    private final String usuario;
    private final String senha;
    private final String remetente;
    private final boolean exigirAutenticacao;
    private final boolean usarStartTls;
    private final ConsoleEmailService consoleFallback;

    private String ultimoStatusEnvio;

    public SmtpEmailService() {
        Properties props = carregarPropriedades();
        this.host = lerValor("HOTELFLOW_SMTP_HOST", props, "mail.smtp.host", "");
        this.porta = lerValor("HOTELFLOW_SMTP_PORT", props, "mail.smtp.port", "587");
        this.usuario = lerValor("HOTELFLOW_SMTP_USER", props, "mail.smtp.user", "");
        this.senha = lerValor("HOTELFLOW_SMTP_PASSWORD", props, "mail.smtp.password", "");

        String padraoRemetente = !this.usuario.isEmpty() ? this.usuario : "nao-responda@hotelflow.com.br";
        this.remetente = lerValor("HOTELFLOW_SMTP_FROM", props, "mail.smtp.from", padraoRemetente);

        String authTexto = lerValor("HOTELFLOW_SMTP_AUTH", props, "mail.smtp.auth", "true");
        this.exigirAutenticacao = Boolean.parseBoolean(authTexto);

        String tlsTexto = lerValor("HOTELFLOW_SMTP_STARTTLS", props, "mail.smtp.starttls", "true");
        this.usarStartTls = Boolean.parseBoolean(tlsTexto);

        this.consoleFallback = new ConsoleEmailService();
        this.ultimoStatusEnvio = "AGUARDANDO_ENVIO";
    }

    public SmtpEmailService(String host, String porta, String usuario, String senha,
                            String remetente, boolean exigirAutenticacao, boolean usarStartTls) {
        this.host = host != null ? host.trim() : "";
        this.porta = (porta != null && !porta.trim().isEmpty()) ? porta.trim() : "587";
        this.usuario = usuario != null ? usuario.trim() : "";
        this.senha = senha != null ? senha.trim() : "";
        this.remetente = (remetente != null && !remetente.trim().isEmpty())
                ? remetente.trim()
                : "nao-responda@hotelflow.com.br";
        this.exigirAutenticacao = exigirAutenticacao;
        this.usarStartTls = usarStartTls;
        this.consoleFallback = new ConsoleEmailService();
        this.ultimoStatusEnvio = "AGUARDANDO_ENVIO";
    }

    @Override
    public void enviarConfirmacaoReserva(String destinatario, String assunto, String corpoTexto) {
        if (!possuiConfiguracaoSmtpValida()) {
            this.ultimoStatusEnvio = "SIMULACAO LOCAL (configure mail.smtp.* em db.properties para envio SMTP real)";
            this.consoleFallback.enviarConfirmacaoReserva(destinatario, assunto, corpoTexto);
            return;
        }

        try {
            enviarViaSmtp(destinatario, assunto, corpoTexto);
            this.ultimoStatusEnvio = "ENVIADO VIA SMTP REAL (" + this.host + ":" + this.porta + ")";
            System.out.println("[SERVICO DE E-MAIL - RN08] E-mail real enviado via SMTP para: " + destinatario);
        } catch (MessagingException e) {
            this.ultimoStatusEnvio = "FALLBACK CONSOLE (falha de conexão SMTP: " + e.getMessage() + ")";
            System.out.println("[AVISO SMTP - RN08] Servidor SMTP indisponível (" + e.getMessage()
                    + "). Acionando fallback local em console.");
            this.consoleFallback.enviarConfirmacaoReserva(destinatario, assunto, corpoTexto);
        }
    }

    public boolean possuiConfiguracaoSmtpValida() {
        if (this.host.isEmpty()) {
            return false;
        }
        if (this.exigirAutenticacao) {
            return !this.usuario.isEmpty() && !this.senha.isEmpty();
        }
        return true;
    }

    public String getUltimoStatusEnvio() {
        return ultimoStatusEnvio;
    }

    private void enviarViaSmtp(String destinatario, String assunto, String corpoTexto) throws MessagingException {
        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.host", this.host);
        smtpProps.put("mail.smtp.port", this.porta);
        smtpProps.put("mail.smtp.auth", String.valueOf(this.exigirAutenticacao));
        smtpProps.put("mail.smtp.starttls.enable", String.valueOf(this.usarStartTls));
        smtpProps.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        smtpProps.put("mail.smtp.timeout", TIMEOUT_MS);
        smtpProps.put("mail.smtp.writetimeout", TIMEOUT_MS);
        if ("465".equals(this.porta)) {
            smtpProps.put("mail.smtp.ssl.enable", "true");
        }

        Session sessao;
        if (this.exigirAutenticacao) {
            final String usuarioAuth = this.usuario;
            final String senhaAuth = this.senha;
            Authenticator autenticador = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(usuarioAuth, senhaAuth);
                }
            };
            sessao = Session.getInstance(smtpProps, autenticador);
        } else {
            sessao = Session.getInstance(smtpProps);
        }

        MimeMessage mensagem = new MimeMessage(sessao);
        mensagem.setFrom(new InternetAddress(this.remetente));
        mensagem.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensagem.setSubject(assunto, "UTF-8");
        mensagem.setText(corpoTexto, "UTF-8");

        Transport.send(mensagem);
    }

    private Properties carregarPropriedades() {
        Properties props = new Properties();
        try (InputStream localStream = SmtpEmailService.class.getResourceAsStream(ARQUIVO_LOCAL)) {
            if (localStream != null) {
                props.load(localStream);
                return props;
            }
        } catch (IOException ignored) {
            // Segue para arquivo de exemplo
        }

        try (InputStream exemploStream = SmtpEmailService.class.getResourceAsStream(ARQUIVO_EXEMPLO)) {
            if (exemploStream != null) {
                props.load(exemploStream);
            }
        } catch (IOException ignored) {
            // Mantém propriedades vazias para fallback seguro
        }
        return props;
    }

    private String lerValor(String chaveAmbiente, Properties props, String chavePropriedade, String valorPadrao) {
        String valorEnv = System.getenv(chaveAmbiente);
        if (valorEnv != null && !valorEnv.trim().isEmpty()) {
            return valorEnv.trim();
        }
        String valorProp = props.getProperty(chavePropriedade);
        if (valorProp != null && !valorProp.trim().isEmpty()) {
            return valorProp.trim();
        }
        return valorPadrao;
    }
}

package com.microservice.notificaciones.service;

import com.microservice.notificaciones.dto.TurnoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String emailRemitente;

    public void enviarCorreoVerificacion(String destinatario, String nombre, String token) {
        try {
            Context context = crearContextoVerificacion(nombre, token);
            String htmlBody = templateEngine.process("email-verificacion", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailRemitente);
            helper.setTo(destinatario);
            helper.setSubject("¡Bienvenido a AgendApp! Verificá tu cuenta");
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo preparar el correo.", e);
        }
    }

    public void enviarCorreoInvitacion(String destino, String nombreNegocio, String nombreRecurso, String token) {
        try {
            Context context = crearContextoInvitacion(nombreNegocio, nombreRecurso, token);
            String htmlCuerpo = templateEngine.process("email-invitacion.html", context);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(emailRemitente);
            helper.setTo(destino);
            helper.setSubject("Invitación para unirte a " + nombreNegocio + " en AgendApp");
            helper.setText(htmlCuerpo, true);

            mailSender.send(mensaje);

        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo preparar el correo.", e);
        }
    }

    public void enviarCorreoTurno(TurnoEvent evento) {
        try {
            Context context = crearContextoTurno(evento);
            boolean esConfirmacion = "CONFIRMACION".equals(evento.getTipoEvento());
            String nombreTemplate = esConfirmacion ? "email-confirmacion-turno" : "email-cancelacion-turno";

            String htmlBody = templateEngine.process(nombreTemplate, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailRemitente);
            helper.setTo(evento.getEmailDestino());

            String asunto = esConfirmacion
                    ? "¡Tu turno en " + evento.getNombreNegocio() + " está confirmado!"
                    : "Turno cancelado en " + evento.getNombreNegocio();

            helper.setSubject(asunto);
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo preparar el correo.", e);
        }
    }

    private Context crearContextoVerificacion(String nombre, String token) {
        Context context = new Context();
        context.setVariable("nombre", nombre);

        context.setVariable("enlace", frontendUrl + "/verificar?token=" + token);
        return context;
    }

    private Context crearContextoInvitacion(String nombreNegocio, String nombreRecurso, String token) {
        Context context = new Context();
        context.setVariable("nombreNegocio", nombreNegocio);
        context.setVariable("nombreRecurso", nombreRecurso);

        context.setVariable("linkConfirmar", frontendUrl + "/invitacion/confirmar?token=" + token);
        return context;
    }

    private Context crearContextoTurno(TurnoEvent evento) {
        Context context = new Context();
        context.setVariable("nombreCliente", evento.getNombreCliente());
        context.setVariable("nombreNegocio", evento.getNombreNegocio());
        context.setVariable("direccionNegocio", evento.getDireccionNegocio());
        context.setVariable("nombreRecurso", evento.getNombreRecurso());
        context.setVariable("nombreServicio", evento.getNombreServicio());
        context.setVariable("fechaHoraTurno", evento.getFechaHoraTurno());
        return context;
    }
}

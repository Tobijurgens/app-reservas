package com.microservice.notificaciones.service;

import jakarta.mail.Part;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailInvitacionTest {
    @Test
    void correoRenderizadoInvitaAConfirmarSinEjecutarDecisionesYEscapaNombres() throws Exception {
        JavaMailSender mail = mock(JavaMailSender.class);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var service = new EmailService(mail, engine);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "emailRemitente", "agendapp@example.com");
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        String token = UUID.randomUUID().toString();

        service.enviarCorreoInvitacion("empleado@example.com", "<script>negocio</script>", "Recurso & agenda", token);

        verify(mail).send(message);
        message.saveChanges(); // El remitente simulado no actualiza los headers MIME como el real.
        String html = contenidoHtml(message);
        assertTrue(html.contains("http://localhost:5173/invitacion/confirmar?token=" + token));
        assertFalse(html.contains("/invitacion/aceptar"));
        assertFalse(html.contains("/invitacion/rechazar"));
        assertTrue(html.contains("7 días"));
        assertTrue(html.contains("Abrirlo no confirma ninguna decisión"));
        assertTrue(html.contains("&lt;script&gt;negocio&lt;/script&gt;"));
        assertFalse(html.contains("<script>negocio</script>"));
        assertTrue(html.contains("Recurso &amp; agenda"));
    }

    private String contenidoHtml(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = contenidoHtml(multipart.getBodyPart(i));
                if (!html.isEmpty()) return html;
            }
        }
        return "";
    }
}

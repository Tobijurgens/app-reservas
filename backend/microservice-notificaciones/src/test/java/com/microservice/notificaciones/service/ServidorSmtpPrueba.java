package com.microservice.notificaciones.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Servidor SMTP de prueba local: no reenvía correos ni registra cuerpos o destinatarios. */
final class ServidorSmtpPrueba implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CountDownLatch cerrar = new CountDownLatch(1);
    private volatile boolean cerrado;
    private volatile boolean timeout;
    private volatile int respuestaDestinatario = 250;
    private final AtomicInteger aceptados = new AtomicInteger();

    ServidorSmtpPrueba() throws IOException {
        this(0);
    }

    ServidorSmtpPrueba(int puerto) throws IOException {
        server = new ServerSocket(puerto, 10, InetAddress.getByName("127.0.0.1"));
        executor.submit(() -> {
            while (!cerrado) {
                try { Socket socket = server.accept(); executor.submit(() -> atender(socket)); }
                catch (IOException ex) { if (!cerrado) throw new IllegalStateException("Falló el SMTP de prueba"); }
            }
        });
    }

    int puerto() { return server.getLocalPort(); }
    int aceptados() { return aceptados.get(); }
    void responderDestinatario(int codigo) { respuestaDestinatario = codigo; }
    void simularTimeout(boolean valor) { timeout = valor; }

    private void atender(Socket socket) {
        try (socket;
             var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(3000);
            if (timeout) { cerrar.await(5, TimeUnit.SECONDS); return; }
            responder(salida, "220 smtp-prueba ESMTP");
            String linea;
            while ((linea = entrada.readLine()) != null) {
                String comando = linea.toUpperCase(java.util.Locale.ROOT);
                if (comando.startsWith("EHLO") || comando.startsWith("HELO")) responder(salida, "250 smtp-prueba");
                else if (comando.startsWith("MAIL FROM")) responder(salida, "250 OK");
                else if (comando.startsWith("RCPT TO")) responder(salida, respuestaDestinatario + " respuesta de prueba");
                else if (comando.equals("DATA")) {
                    responder(salida, "354 fin con punto");
                    while ((linea = entrada.readLine()) != null && !linea.equals(".")) { /* No almacenar contenido. */ }
                    if (linea != null) { aceptados.incrementAndGet(); responder(salida, "250 aceptado"); }
                } else if (comando.equals("QUIT")) { responder(salida, "221 fin"); return; }
                else responder(salida, "250 OK");
            }
        } catch (IOException ex) { /* El cliente puede cerrar por timeout o rechazo esperado. */ }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }

    private void responder(BufferedWriter salida, String linea) throws IOException {
        salida.write(linea + "\r\n"); salida.flush();
    }

    @Override public void close() throws IOException {
        cerrado = true; cerrar.countDown(); server.close(); executor.shutdownNow();
    }
}

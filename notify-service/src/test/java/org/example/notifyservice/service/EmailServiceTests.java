package org.example.notifyservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTests {

    @Test
    void sendsOrderConfirmationToRecipient() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "shop@example.com");

        emailService.sendOrderCreatedEmail("customer@example.com");

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertEquals("shop@example.com", message.getValue().getFrom());
        assertEquals("customer@example.com", message.getValue().getTo()[0]);
        assertEquals("Your order has been created successfully.", message.getValue().getText());
    }

    @Test
    void rejectsBlankRecipient() {
        EmailService emailService = new EmailService(mock(JavaMailSender.class), "");

        assertThrows(IllegalArgumentException.class, () -> emailService.sendOrderCreatedEmail(" "));
    }
}

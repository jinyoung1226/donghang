package com.ebiz.wsb.domain.mail.application;

import com.ebiz.wsb.domain.mail.dto.CheckCodeDTO;
import com.ebiz.wsb.domain.mail.exception.InvalidMailException;
import com.ebiz.wsb.domain.mail.exception.WrongAuthenticationCodeException;
import com.ebiz.wsb.domain.mail.repository.AuthCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {

    @Value("${spring.mail.username}")
    private String FROM_MAIL;
    private static final String MAIL_SEND_EXCEPTION_MESSAGE = "메일 전송 중 오류";

    private final JavaMailSender javaMailSender;
    private final AuthCodeRepository authCodeRepository;

    public void sendAuthenticationCode(String authMailSubject, @NotNull String email) {
        String toEmail = email;
        String randomCode = generateRandomCode();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "utf-8");
            messageHelper.setFrom(FROM_MAIL);
            messageHelper.setTo(toEmail);
            messageHelper.setSubject(authMailSubject);
            messageHelper.setText(randomCode);

            javaMailSender.send(message);
            authCodeRepository.save(email, randomCode);
        } catch (MailSendException e) {
            throw new InvalidMailException(MAIL_SEND_EXCEPTION_MESSAGE);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        int n = random.nextInt(1000000);
        return String.format("%06d", n);
    }

    public void checkAuthCode(@Valid CheckCodeDTO checkCodeDTO) {
        String storedCode = authCodeRepository.findCodeByEmailAndRandomCode(checkCodeDTO);
        if(storedCode == null || !storedCode.equals(checkCodeDTO.getCode())) {
            throw new WrongAuthenticationCodeException(MAIL_SEND_EXCEPTION_MESSAGE);
        }
        authCodeRepository.delete(storedCode);
    }
}

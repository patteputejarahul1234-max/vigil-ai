package com.vigilai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async so EmailService doesn't block the request thread on
 * slow SMTP calls — registration/password-reset responses return
 * immediately while the email sends in the background.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

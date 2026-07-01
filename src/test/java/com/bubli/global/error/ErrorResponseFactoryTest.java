package com.bubli.global.error;

import com.bubli.global.locale.SupportedLocale;
import com.bubli.global.security.AuthUser;
import com.bubli.user.service.UserLocalePublicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseFactoryTest {

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final UserLocalePublicService userLocalePublicService = new UserLocalePublicService() {
        @Override
        public String resolveLocaleCode(AuthUser authUser, String acceptLanguageHeader) {
            return resolveLocaleCode(acceptLanguageHeader);
        }

        @Override
        public String resolveLocaleCode(UUID userId, String acceptLanguageHeader) {
            return resolveLocaleCode(acceptLanguageHeader);
        }

        @Override
        public String resolveLocaleCode(String acceptLanguageHeader) {
            if (acceptLanguageHeader != null && acceptLanguageHeader.startsWith("en")) {
                return "en-US";
            }
            if (acceptLanguageHeader != null && acceptLanguageHeader.startsWith("ja")) {
                return "ja-JP";
            }
            return SupportedLocale.DEFAULT.code();
        }
    };
    private final ErrorResponseFactory factory = new ErrorResponseFactory(messageSource, userLocalePublicService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesErrorMessageFromAcceptLanguage() {
        messageSource.addMessage(ErrorCode.AUTH_401_001.getMessageKey(), Locale.US, "Authentication is required.");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US,en;q=0.9");

        ErrorResponse response = factory.of(ErrorCode.AUTH_401_001, "trace-1", request);

        assertThat(response.getCode()).isEqualTo("AUTH_401_001");
        assertThat(response.getMessageKey()).isEqualTo("error.AUTH_401_001");
        assertThat(response.getMessage()).isEqualTo("Authentication is required.");
    }

    @Test
    void resolvesValidationFieldMessagesFromRequestLocale() {
        Locale japanese = Locale.forLanguageTag("ja-JP");
        messageSource.addMessage(ErrorCode.COMMON_400_002.getMessageKey(), japanese, "Japanese bad request.");
        messageSource.addMessage("NotBlank", japanese, "Japanese blank.");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ja-JP,ja;q=0.9");
        FieldError fieldError = new FieldError(
                "request",
                "name",
                null,
                false,
                new String[]{"NotBlank"},
                null,
                "must not be blank"
        );

        ErrorResponse response = factory.of(ErrorCode.COMMON_400_002, "trace-2", request, List.of(fieldError));

        assertThat(response.getMessage()).isEqualTo("Japanese bad request.");
        assertThat(response.getFields()).singleElement()
                .satisfies(error -> assertThat(error.getReason()).isEqualTo("Japanese blank."));
    }

    @Test
    void authenticatedUserLocaleWinsOverAcceptLanguage() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage(ErrorCode.AUTH_403_001.getMessageKey(), Locale.JAPAN, "Japanese forbidden.");
        source.addMessage(ErrorCode.AUTH_403_001.getMessageKey(), Locale.US, "English forbidden.");
        ErrorResponseFactory localeAwareFactory = new ErrorResponseFactory(source, new UserLocalePublicService() {
            @Override
            public String resolveLocaleCode(AuthUser authUser, String acceptLanguageHeader) {
                return authUser == null ? resolveLocaleCode(acceptLanguageHeader) : "ja-JP";
            }

            @Override
            public String resolveLocaleCode(UUID userId, String acceptLanguageHeader) {
                return userId == null ? resolveLocaleCode(acceptLanguageHeader) : "ja-JP";
            }

            @Override
            public String resolveLocaleCode(String acceptLanguageHeader) {
                return "en-US";
            }
        });
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(UUID.randomUUID()),
                null,
                List.of()
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US,en;q=0.9");

        ErrorResponse response = localeAwareFactory.of(ErrorCode.AUTH_403_001, "trace-3", request);

        assertThat(response.getMessage()).isEqualTo("Japanese forbidden.");
    }
}

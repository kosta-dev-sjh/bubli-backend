package com.bubli.global.error;

import com.bubli.global.locale.SupportedLocale;
import com.bubli.global.locale.LocaleResolverPort;
import com.bubli.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ErrorResponseFactory {

    private final MessageSource messageSource;
    private final LocaleResolverPort localeResolverPort;

    public ErrorResponse of(ErrorCode errorCode, String traceId, HttpServletRequest request) {
        return ErrorResponse.of(errorCode, resolveMessage(errorCode, request), traceId);
    }

    public ErrorResponse of(
            ErrorCode errorCode,
            String traceId,
            HttpServletRequest request,
            List<FieldError> fieldErrors
    ) {
        Locale locale = resolveLocale(request);
        List<ErrorResponse.FieldError> localizedFieldErrors = fieldErrors.stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getField(),
                        resolveValidationMessage(fieldError, locale)
                ))
                .toList();
        return ErrorResponse.of(errorCode, resolveMessage(errorCode, locale), traceId, localizedFieldErrors);
    }

    private String resolveMessage(ErrorCode errorCode, HttpServletRequest request) {
        return resolveMessage(errorCode, resolveLocale(request));
    }

    private String resolveMessage(ErrorCode errorCode, Locale locale) {
        return messageSource.getMessage(
                errorCode.getMessageKey(),
                null,
                errorCode.getDefaultMessage(),
                locale
        );
    }

    private String resolveValidationMessage(FieldError fieldError, Locale locale) {
        try {
            return messageSource.getMessage(fieldError, locale);
        } catch (NoSuchMessageException e) {
            return fieldError.getDefaultMessage();
        }
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request == null ? null : request.getHeader("Accept-Language");
        AuthUser authUser = currentAuthUser();
        String localeCode;
        try {
            localeCode = localeResolverPort.resolveLocaleCode(authUser, acceptLanguage);
        } catch (RuntimeException e) {
            localeCode = localeResolverPort.resolveLocaleCode(acceptLanguage);
        }
        return SupportedLocale.resolve(localeCode).toJavaLocale();
    }

    private AuthUser currentAuthUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return null;
        }
        return authUser;
    }
}

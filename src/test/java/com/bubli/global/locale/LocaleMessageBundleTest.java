package com.bubli.global.locale;

import com.bubli.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class LocaleMessageBundleTest {

    private static final List<String> AGENT_MESSAGE_KEYS = List.of(
            "agent.job.succeeded.event",
            "agent.job.failed.event",
            "agent.job.succeeded.notification.title",
            "agent.job.failed.notification.title",
            "agent.job.notification.body"
    );

    @Test
    void allSupportedLocalesDefineEveryErrorCodeMessageKey() {
        for (SupportedLocale locale : SupportedLocale.values()) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale.toJavaLocale());

            for (ErrorCode errorCode : ErrorCode.values()) {
                assertThat(bundle.containsKey(errorCode.getMessageKey()))
                        .as("%s must define %s", locale.code(), errorCode.getMessageKey())
                        .isTrue();
            }
        }
    }

    @Test
    void allSupportedLocalesDefineAgentNotificationMessageKeys() {
        for (SupportedLocale locale : SupportedLocale.values()) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale.toJavaLocale());

            for (String key : AGENT_MESSAGE_KEYS) {
                assertThat(bundle.containsKey(key))
                        .as("%s must define %s", locale.code(), key)
                        .isTrue();
            }
        }
    }

    @Test
    void bundlesResolveDistinctLocalizedMessages() {
        String korean = ResourceBundle.getBundle("messages", Locale.KOREAN)
                .getString(ErrorCode.AUTH_401_001.getMessageKey());
        String english = ResourceBundle.getBundle("messages", Locale.US)
                .getString(ErrorCode.AUTH_401_001.getMessageKey());
        String japanese = ResourceBundle.getBundle("messages", Locale.JAPAN)
                .getString(ErrorCode.AUTH_401_001.getMessageKey());

        assertThat(List.of(korean, english, japanese)).doesNotHaveDuplicates();
    }
}

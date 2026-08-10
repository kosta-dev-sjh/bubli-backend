package com.bubli.resource.service;

public final class ResourceDocumentLanguageDetector {

    private ResourceDocumentLanguageDetector() {
    }

    public static String detect(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        int korean = 0;
        int japanese = 0;
        int latin = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character >= 0xAC00 && character <= 0xD7A3) {
                korean++;
            } else if ((character >= 0x3040 && character <= 0x30FF)) {
                japanese++;
            } else if (Character.isLetter(character) && character <= 0x024F) {
                latin++;
            }
        }
        if (korean == 0 && japanese == 0 && latin == 0) {
            return "unknown";
        }
        if (korean >= japanese && korean >= latin) {
            return "ko";
        }
        if (japanese >= latin) {
            return "ja";
        }
        return "en";
    }
}

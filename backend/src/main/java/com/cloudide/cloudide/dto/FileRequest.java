package com.cloudide.cloudide.dto;

import com.cloudide.cloudide.enums.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FileRequest {

    @NotBlank(message = "Filename is required")
    @Size(max = 150, message = "Filename must not exceed 150 characters")
    private String filename;

    private String content;

    private ProgrammingLanguage language;

    public FileRequest() {
    }

    public FileRequest(String filename, String content, ProgrammingLanguage language) {
        this.filename = filename;
        this.content = content;
        this.language = language;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public void setLanguage(ProgrammingLanguage language) {
        this.language = language;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String filename;
        private String content;
        private ProgrammingLanguage language;

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder language(ProgrammingLanguage language) {
            this.language = language;
            return this;
        }

        public FileRequest build() {
            return new FileRequest(filename, content, language);
        }
    }
}

package com.enigmazer.clef.service.gemini;

import com.enigmazer.clef.dto.gemini.*;
import com.enigmazer.clef.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.url}")
    private String url;

    private final RestClient restClient;

    public String parse(byte[] pdfBytes, String prompt) {

        String base64pdf = Base64.getEncoder().encodeToString(pdfBytes);
        Part pdfPart = new Part(null, new InlineData("application/pdf", base64pdf));
        Part promtPart = new Part(prompt, null);
        Content content = new Content(List.of(pdfPart, promtPart));
        GenerationConfig generationConfig = new GenerationConfig("application/json");
        GeminiRequest geminiRequest = new GeminiRequest(List.of(content), generationConfig);

        String parsedResponse;
        log.debug("Sending PDF to Gemini for parsing [size={} bytes]", pdfBytes.length);
        try {
            GeminiResponse geminiResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (geminiResponse == null) throw new BusinessException("Unable to parse");

            Candidate candidate = geminiResponse.candidates().getFirst();
            if(candidate == null) {
                log.warn("Gemini returned empty candidates list ");
                throw new BusinessException("Could not parse syllabus — try a clearer document");
            }

            parsedResponse =  candidate.content().parts().getFirst().text();
            if(parsedResponse == null) {
                log.warn("Gemini returned blank text in response");
                throw new BusinessException("Parsing returned empty response");
            }

        }catch (RestClientException e){
            log.error("Gemini API call failed [message={}]", e.getMessage());
            throw  new BusinessException("Failed to connect to parsing service");
        }

        log.debug("Gemini response received successfully");
        return parsedResponse;

    }
}

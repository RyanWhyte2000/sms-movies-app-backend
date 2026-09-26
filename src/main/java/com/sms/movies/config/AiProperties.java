package com.sms.movies.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
  private boolean enabled;
  private String apiKey = "";
  private String baseUrl = "https://api.openai.com";
  private String model = "gpt-4.1-mini-2025-04-14";
  private Duration connectTimeout = Duration.ofSeconds(2);
  private Duration readTimeout = Duration.ofSeconds(5);
  private double minimumConfidence = 0.55;
  private int candidateLimit = 80;

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
  public Duration getConnectTimeout() { return connectTimeout; }
  public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
  public Duration getReadTimeout() { return readTimeout; }
  public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
  public double getMinimumConfidence() { return minimumConfidence; }
  public void setMinimumConfidence(double minimumConfidence) { this.minimumConfidence = minimumConfidence; }
  public int getCandidateLimit() { return candidateLimit; }
  public void setCandidateLimit(int candidateLimit) { this.candidateLimit = candidateLimit; }
}

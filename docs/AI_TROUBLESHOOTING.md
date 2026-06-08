# AI Troubleshooting Guide

This document covers common issues when using AI features (Genealogy Agents, Text Import, Voice Input) in Family Tree Editor and provides steps to resolve them.

## General Diagnostics: "Test Connection"

In the application's **AI Settings** menu, there is a **"Test Connection"** button at the bottom. **Always use this button first** to verify your configuration. It will send a minimal ping to your selected AI provider and report success or failure.

---

## 1. API Key and Authentication Errors

### 🔴 HTTP 401 Unauthorized / Invalid API Key
**Symptom:** The connection test fails with `401 Unauthorized` or "Invalid API Key".
**Causes & Solutions:**
- **OpenAI / Anthropic / Google:** You either entered an incorrect API key, or your API key has been revoked. Ensure there are no leading or trailing spaces.
- **Yandex Cloud:** You are using a Service Account API Key but your service account does not have the required roles. Ensure your service account has `ai.languageModels.user` (for YandexGPT) and `ai.speechkit-stt.user` (for SpeechKit). See [YANDEX_AI_SETUP.md](YANDEX_AI_SETUP.md) for details.

### 🔴 HTTP 403 Forbidden / Permission Denied
**Symptom:** Connection test fails with `403 Forbidden` or "Permission to resource denied".
**Causes & Solutions:**
- **Yandex Cloud:** You specified a "Folder ID" but your key does not have access to that folder. Try leaving the Folder ID field as `default`.
- **Google / OpenAI:** Your account might be suspended or restricted by region.

---

## 2. Rate Limits and Quotas

### 🔴 HTTP 429 Too Many Requests / Quota Exceeded
**Symptom:** The AI was working but suddenly stopped, or fails immediately with `429 Too Many Requests`.
**Causes & Solutions:**
- **OpenAI (Insufficient Quota):** You have run out of prepaid credits on your OpenAI platform account. Add funds to your billing account.
- **Rate Limit Hit:** You sent too many requests in a short time. The application has automatic retry with exponential backoff for rate limits, but if it still fails, wait a few minutes before trying again.

---

## 3. Local Model (Ollama) Troubleshooting

### 🔴 Connection Refused / Timeout
**Symptom:** Using Ollama or a Custom provider fails with "Connection Refused" or a Timeout.
**Causes & Solutions:**
1. **Ollama is not running:** Make sure the Ollama application is launched on your computer.
2. **Incorrect Base URL:** If running Ollama on the same machine, the Base URL should be `http://localhost:11434`. If on a different machine, ensure network firewalls allow traffic on port `11434`.
3. **Android Emulator to Localhost:** If you are running the Android app in an emulator, `localhost` points to the emulator itself. To access Ollama on your host Mac/PC from the Android emulator, change the Base URL to `http://10.0.2.2:11434`.

### 🔴 Model Not Found
**Symptom:** `Error: model 'xyz' not found, try pulling it first`.
**Causes & Solutions:**
You selected a model preset (e.g., `qwen2.5:7b`), but you haven't downloaded it via Ollama yet. Open your terminal and run:
```bash
ollama run qwen2.5:7b
```

---

## 4. Agent Execution Failures

### 🔴 Iteration Limit Reached (50 steps)
**Symptom:** The agent runs for a long time and then stops with "Iteration limit reached (50 steps)".
**Causes & Solutions:**
- The agent got stuck in a loop (e.g., repeatedly searching for the same person without success).
- **Solution:** The agent gracefully synthesizes whatever it found up to that point. If the results are poor, try simplifying the tree before running the agent, or clear the search cache in AI Settings.

### 🔴 Parsing / JSON Formatting Errors (Text Import)
**Symptom:** Importing AI Text fails with a JSON parsing error.
**Causes & Solutions:**
The AI model returned malformed JSON or included markdown code blocks instead of raw JSON.
- **Solution:** Use a more capable model (like GPT-4o-mini or Claude 3.5 Sonnet) for Text Import, as smaller local models sometimes fail strict JSON formatting requirements.

---

## 5. Clearing the Cache
If the AI Agent is repeatedly failing because it "remembers" bad search results or cached web pages, go to **AI Settings** and click **"Clear Research Search Cache"**. This deletes cached methodology guides and search engine results.

## See Also
- [Yandex AI Setup Guide](YANDEX_AI_SETUP.md)
- [Voice Input Troubleshooting](VOICE_INPUT.md)

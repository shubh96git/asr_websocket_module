# asr_websocket_module

This project demonstrates a **real-time speech-to-text pipeline** using a microphone, a Spring Boot server acting as a proxy, and an ASR (Automatic Speech Recognition) backend. It highlights modern **web technologies**, including **WebSockets**, **Spring Security with JWT**, **WebSocket authentication**, and **rate limiting using Bucket4j**.

---

## Core Concept

The main flow of the project is:

- **Browser Mic**: Captures live audio from the user's microphone using the Web Audio API. Audio is converted to 16-bit PCM and streamed over WebSocket.
- **Spring Boot Proxy Server**: Acts as a secure intermediary between the browser and the ASR server. Responsibilities include:
  - Validating JWT tokens for both REST and WebSocket connections.
  - Forwarding audio streams to the ASR server.
  - Returning real-time transcription to the browser.
  - Applying rate limiting to prevent abuse.
- **ASR Server**: Receives streaming audio and performs real-time speech-to-text transcription. Can be implemented using any ASR solution such as NVIDIA Riva or other Python-based ASR backends.

---

## Features

### 1. **WebSocket Communication**
- Enables real-time audio streaming from the browser to the Spring Boot server.
- Receives transcription updates from the ASR server in real-time.
- Low latency streaming for responsive feedback.

### 2. **Proxy Server**
- Central server acts as a proxy for the ASR backend.
- Handles all communication securely and can log or preprocess audio if required.

### 3. **Spring Security & JWT**
- Secures REST endpoints with username/password login.
- Generates **JWT tokens** for authenticated users.
- Tokens are verified for WebSocket handshake and API requests.
- Ensures only authorized users can connect and stream audio.

### 4. **WebSocket Security**
- Implements a custom **HandshakeInterceptor** that:
  - Extracts the JWT token from the `Authorization` header.
  - Validates the token using `JwtTokenService`.
  - Rejects any unauthorized or invalid connections.
- Protects the ASR streaming endpoint from untrusted clients.

### 5. **Rate Limiting (Bucket4j)**
- Prevents clients from overloading the server or ASR backend.
- Configurable request limits per IP or user.
- Ensures fair usage and improves system reliability.

### 6. **Multi-Language Support**
- Users can select a language (e.g., English or Hindi) for real-time transcription.
- Supports dynamic language changes mid-session.

---

## Technical Implementation

- **Frontend (Browser)**
  - HTML + JavaScript (Web Audio API, WebSocket)
  - Streams audio in 16-bit PCM format.
  - Displays real-time transcription with final/interim text.
  - Shows microphone and handshake status.

- **Backend (Spring Boot)**
  - Spring WebSocket for real-time audio streaming.
  - Spring Security for authentication and authorization.
  - JWT token generation and validation.
  - Custom WebSocket HandshakeInterceptor for JWT verification.
  - Bucket4j-based rate limiting for security.

- **ASR Backend**
  - Receives audio over WebSocket from the Spring Boot server.
  - Performs real-time transcription.
  - Sends transcription back to the Spring Boot proxy.

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/shubh96git/asr_websocket_module.git
cd asr_websocket_module



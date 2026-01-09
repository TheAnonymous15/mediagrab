# LiveKit Server Setup for Podcast Studio
==========================================

## Quick Start

### Step 1: Install LiveKit Server

**macOS:**
```bash
brew install livekit
```

**Linux:**
```bash
curl -sSL https://get.livekit.io | bash
```

**Docker:**
```bash
docker pull livekit/livekit-server
```

### Step 2: Start LiveKit Server

```bash
cd livekit-server
livekit-server --config livekit.yaml
```

Or with Docker:
```bash
docker run --rm \
  -p 7880:7880 \
  -p 7881:7881 \
  -p 50000-60000:50000-60000/udp \
  -v $(pwd)/livekit.yaml:/livekit.yaml \
  livekit/livekit-server \
  --config /livekit.yaml
```

### Step 3: Start Token Server

```bash
cd livekit-server
pip install -r requirements.txt
python token_server.py
```

### Step 4: Port Forwarding (for public access)

Forward these ports on your router:
- **7880 TCP** - LiveKit signaling
- **7881 TCP** - TCP fallback
- **50000-60000 UDP** - WebRTC media

### Step 5: Configure Android App

In your app, set the token server URL:
```kotlin
val roomManager = LiveKitRoomManager(context)
roomManager.configure(
    config = LiveKitConfig.selfHosted(
        publicIp = "YOUR_PUBLIC_IP",
        port = 7880,
        apiKey = "devkey",
        apiSecret = "secret"
    ),
    tokenServerUrl = "http://YOUR_PUBLIC_IP:8081/token"
)
```

---

## Architecture

```
┌─────────────────┐      ┌─────────────────┐
│  Android App    │◄────►│  LiveKit Server │
│  (LiveKit SDK)  │      │  (Port 7880)    │
└────────┬────────┘      └────────┬────────┘
         │                        │
         │                        │
         ▼                        ▼
┌─────────────────┐      ┌─────────────────┐
│  Token Server   │      │  Other Clients  │
│  (Port 8081)    │      │  (Web/Mobile)   │
└─────────────────┘      └─────────────────┘
```

---

## Ports Summary

| Port | Protocol | Purpose |
|------|----------|---------|
| 7880 | TCP | LiveKit signaling (WebSocket) |
| 7881 | TCP | TCP fallback for media |
| 8081 | TCP | Token server API |
| 50000-60000 | UDP | WebRTC media streams |

---

## Production Recommendations

1. **Use SSL/TLS** - Get certificates from Let's Encrypt
2. **Change API keys** - Use strong, unique keys
3. **Use Redis** - For multi-server deployments
4. **Set up TURN** - For users behind strict firewalls
5. **Monitor** - Use LiveKit's dashboard or Prometheus

---

## Testing Locally

1. Start LiveKit: `livekit-server --dev`
2. Start token server: `python token_server.py`
3. Test with: `curl -X POST http://localhost:8081/token -H "Content-Type: application/json" -d '{"room":"test","participant":"user1","isHost":true}'`

---

## Troubleshooting

**Connection fails:**
- Check firewall allows ports 7880, 7881
- Ensure UDP ports 50000-60000 are forwarded
- Verify public IP is correct

**Token errors:**
- Ensure API key/secret match between server and token server
- Check token server is running

**No audio/video:**
- Check UDP port forwarding
- Try TCP fallback (port 7881)
- Verify TURN server if behind NAT


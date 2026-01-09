"""
LiveKit Token Server for Podcast Studio
========================================

This server generates access tokens for LiveKit rooms.
Run this alongside the LiveKit server.

Requirements:
    pip install livekit-api flask flask-cors

Usage:
    python livekit_token_server.py

Endpoints:
    POST /token - Generate access token
        Body: {"room": "room_name", "participant": "display_name", "isHost": true/false}
        Response: {"token": "jwt_token"}

    GET /health - Health check
"""

import os
from datetime import timedelta
from flask import Flask, request, jsonify
from flask_cors import CORS
from livekit import api

app = Flask(__name__)
CORS(app)  # Enable CORS for mobile app

# ============================================
# CONFIGURATION
# ============================================

# LiveKit server credentials
# For self-hosted: use the keys from your livekit.yaml
# For LiveKit Cloud: get these from your project settings
LIVEKIT_API_KEY = os.getenv("LIVEKIT_API_KEY", "devkey")
LIVEKIT_API_SECRET = os.getenv("LIVEKIT_API_SECRET", "secret")
LIVEKIT_URL = os.getenv("LIVEKIT_URL", "ws://localhost:7880")

# Token settings
TOKEN_TTL = timedelta(hours=6)  # Token valid for 6 hours

print(f"""
╔══════════════════════════════════════════════════════════════╗
║           LiveKit Token Server for Podcast Studio            ║
╠══════════════════════════════════════════════════════════════╣
║  API Key:     {LIVEKIT_API_KEY:<46} ║
║  LiveKit URL: {LIVEKIT_URL:<46} ║
╚══════════════════════════════════════════════════════════════╝
""")

# ============================================
# ROUTES
# ============================================

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "service": "livekit-token-server",
        "livekit_url": LIVEKIT_URL
    })


@app.route('/token', methods=['POST'])
def generate_token():
    """
    Generate a LiveKit access token

    Request body:
    {
        "room": "podcast_ABC123",
        "participant": "John Doe",
        "isHost": true
    }
    """
    try:
        data = request.json or {}

        room_name = data.get('room', '').strip()
        participant_name = data.get('participant', '').strip()
        is_host = data.get('isHost', False)

        if not room_name:
            return jsonify({"error": "Room name is required"}), 400

        if not participant_name:
            return jsonify({"error": "Participant name is required"}), 400

        # Create access token
        token = api.AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET)

        # Set identity (unique identifier for this participant)
        identity = f"{participant_name.lower().replace(' ', '_')}_{os.urandom(4).hex()}"
        token.with_identity(identity)
        token.with_name(participant_name)
        token.with_ttl(TOKEN_TTL)

        # Set permissions based on role
        if is_host:
            # Host has full permissions
            token.with_grants(api.VideoGrants(
                room_join=True,
                room=room_name,
                room_create=True,
                room_admin=True,
                can_publish=True,
                can_publish_data=True,
                can_subscribe=True,
                can_update_own_metadata=True,
            ))
        else:
            # Guest has limited permissions
            token.with_grants(api.VideoGrants(
                room_join=True,
                room=room_name,
                can_publish=True,
                can_publish_data=True,
                can_subscribe=True,
                can_update_own_metadata=True,
            ))

        jwt_token = token.to_jwt()

        print(f"Token generated for '{participant_name}' in room '{room_name}' (host={is_host})")

        return jsonify({
            "token": jwt_token,
            "identity": identity,
            "room": room_name,
            "livekit_url": LIVEKIT_URL
        })

    except Exception as e:
        print(f"Error generating token: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/room/create', methods=['POST'])
def create_room():
    """
    Create a new room (optional - rooms are created automatically on first join)

    Request body:
    {
        "name": "podcast_ABC123",
        "emptyTimeout": 300,
        "maxParticipants": 10
    }
    """
    try:
        data = request.json or {}
        room_name = data.get('name', '').strip()

        if not room_name:
            return jsonify({"error": "Room name is required"}), 400

        # Create room service client
        room_service = api.RoomServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        # Create room
        room = room_service.create_room(
            api.CreateRoomRequest(
                name=room_name,
                empty_timeout=data.get('emptyTimeout', 300),
                max_participants=data.get('maxParticipants', 10)
            )
        )

        print(f"Room created: {room_name}")

        return jsonify({
            "name": room.name,
            "sid": room.sid,
            "created": True
        })

    except Exception as e:
        print(f"Error creating room: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/room/<room_name>/participants', methods=['GET'])
def list_participants(room_name):
    """List participants in a room"""
    try:
        room_service = api.RoomServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        participants = room_service.list_participants(
            api.ListParticipantsRequest(room=room_name)
        )

        return jsonify({
            "room": room_name,
            "participants": [
                {
                    "sid": p.sid,
                    "identity": p.identity,
                    "name": p.name,
                    "state": str(p.state),
                }
                for p in participants.participants
            ]
        })

    except Exception as e:
        print(f"Error listing participants: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/room/<room_name>/kick/<participant_identity>', methods=['POST'])
def kick_participant(room_name, participant_identity):
    """Kick a participant from a room"""
    try:
        room_service = api.RoomServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        room_service.remove_participant(
            api.RoomParticipantIdentity(
                room=room_name,
                identity=participant_identity
            )
        )

        print(f"Kicked {participant_identity} from {room_name}")

        return jsonify({"success": True})

    except Exception as e:
        print(f"Error kicking participant: {e}")
        return jsonify({"error": str(e)}), 500


# ============================================
# EGRESS (Recording/Streaming) - Requires LiveKit Egress Service
# ============================================

@app.route('/room/<room_name>/record/start', methods=['POST'])
def start_recording(room_name):
    """Start recording a room (requires Egress service)"""
    try:
        # Note: Requires LiveKit Egress service to be running
        egress = api.EgressServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        data = request.json or {}
        output_path = data.get('outputPath', f'/recordings/{room_name}.mp4')

        result = egress.start_room_composite_egress(
            api.RoomCompositeEgressRequest(
                room_name=room_name,
                file=api.EncodedFileOutput(
                    file_type=api.EncodedFileType.MP4,
                    filepath=output_path
                )
            )
        )

        return jsonify({
            "egressId": result.egress_id,
            "status": "recording"
        })

    except Exception as e:
        print(f"Error starting recording: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/room/<room_name>/stream/start', methods=['POST'])
def start_streaming(room_name):
    """Start streaming a room to RTMP (requires Egress service)"""
    try:
        egress = api.EgressServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        data = request.json or {}
        rtmp_url = data.get('rtmpUrl')

        if not rtmp_url:
            return jsonify({"error": "RTMP URL is required"}), 400

        result = egress.start_room_composite_egress(
            api.RoomCompositeEgressRequest(
                room_name=room_name,
                stream=api.StreamOutput(
                    protocol=api.StreamProtocol.RTMP,
                    urls=[rtmp_url]
                )
            )
        )

        return jsonify({
            "egressId": result.egress_id,
            "status": "streaming"
        })

    except Exception as e:
        print(f"Error starting stream: {e}")
        return jsonify({"error": str(e)}), 500


@app.route('/egress/<egress_id>/stop', methods=['POST'])
def stop_egress(egress_id):
    """Stop a recording or stream"""
    try:
        egress = api.EgressServiceClient(
            LIVEKIT_URL.replace('ws://', 'http://').replace('wss://', 'https://'),
            LIVEKIT_API_KEY,
            LIVEKIT_API_SECRET
        )

        egress.stop_egress(api.StopEgressRequest(egress_id=egress_id))

        return jsonify({"success": True})

    except Exception as e:
        print(f"Error stopping egress: {e}")
        return jsonify({"error": str(e)}), 500


# ============================================
# MAIN
# ============================================

if __name__ == '__main__':
    port = int(os.getenv("PORT", 8081))
    host = os.getenv("HOST", "0.0.0.0")

    print(f"Starting token server on {host}:{port}")
    print(f"Token endpoint: http://{host}:{port}/token")
    print()

    app.run(host=host, port=port, debug=True)


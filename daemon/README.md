# Artier IDE Daemon Server

Server backend untuk Artier IDE yang menyediakan:
- WebSocket server untuk komunikasi real-time
- Terminal PTY backend (node-pty)
- File system operations
- Process management

## Setup

### Prerequisites
- Node.js 18+ 
- npm atau yarn

### Install Dependencies

```bash
cd daemon
npm install
```

### Run Server

```bash
# Development
npm run dev

# Production
npm start
```

Server akan berjalan di:
- HTTP: `http://localhost:8080`
- WebSocket: `ws://localhost:8080`

## API

### WebSocket Messages

#### Terminal Operations

**Create Terminal Session**
```json
{
  "type": "terminal_create",
  "payload": {
    "workingDirectory": "/home/user"
  }
}
```

**Send Terminal Input**
```json
{
  "type": "terminal_input",
  "payload": {
    "sessionId": "1234567890",
    "data": "ls -la\n"
  }
}
```

**Resize Terminal**
```json
{
  "type": "terminal_resize",
  "payload": {
    "sessionId": "1234567890",
    "cols": 80,
    "rows": 24
  }
}
```

**Close Terminal Session**
```json
{
  "type": "terminal_close",
  "payload": {
    "sessionId": "1234567890"
  }
}
```

#### File Operations

**Read File**
```json
{
  "type": "file_read",
  "payload": {
    "path": "/path/to/file.txt"
  }
}
```

**Write File**
```json
{
  "type": "file_write",
  "payload": {
    "path": "/path/to/file.txt",
    "content": "Hello World"
  }
}
```

**List Directory**
```json
{
  "type": "directory_list",
  "payload": {
    "path": "/path/to/directory"
  }
}
```

**Create File**
```json
{
  "type": "file_create",
  "payload": {
    "path": "/path/to/new/file.txt"
  }
}
```

**Create Directory**
```json
{
  "type": "directory_create",
  "payload": {
    "path": "/path/to/new/directory"
  }
}
```

**Delete File**
```json
{
  "type": "file_delete",
  "payload": {
    "path": "/path/to/file.txt"
  }
}
```

**Rename File**
```json
{
  "type": "file_rename",
  "payload": {
    "oldPath": "/path/to/old/file.txt",
    "newPath": "/path/to/new/file.txt"
  }
}
```

### Responses

**Terminal Output**
```json
{
  "type": "terminal_output",
  "payload": {
    "sessionId": "1234567890",
    "data": "output text"
  }
}
```

**Terminal Exit**
```json
{
  "type": "terminal_exit",
  "payload": {
    "sessionId": "1234567890",
    "exitCode": 0
  }
}
```

**File Content**
```json
{
  "type": "file_content",
  "payload": {
    "path": "/path/to/file.txt",
    "content": "file content"
  }
}
```

**File Saved**
```json
{
  "type": "file_saved",
  "payload": {
    "path": "/path/to/file.txt"
  }
}
```

**Directory Listing**
```json
{
  "type": "directory_listing",
  "payload": {
    "path": "/path/to/directory",
    "items": [
      {
        "name": "file.txt",
        "isDirectory": false,
        "path": "/path/to/directory/file.txt"
      }
    ]
  }
}
```

**Error**
```json
{
  "type": "error",
  "payload": {
    "message": "Error message"
  }
}
```

## Integration with Android App

### WebSocket Client Setup

```kotlin
val webSocket = OkHttpClient().newWebSocket(
    Request.Builder()
        .url("ws://localhost:8080")
        .build(),
    object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val message = Gson().fromJson(text, WebSocketMessage::class.java)
            // Handle message
        }
    }
)
```

### Send Message

```kotlin
val message = mapOf(
    "type" to "terminal_create",
    "payload" to mapOf(
        "workingDirectory" to "/home/user"
    )
)
webSocket.send(Gson().toJson(message))
```

## Development

### File Structure

```
daemon/
├── server.js          # Main server file
├── package.json       # Dependencies
└── README.md          # This file
```

### Adding New Features

1. Add new message type in `handleMessage` function
2. Implement handler function
3. Add response format to this README
4. Update Android WebSocket client

## Testing

### Using wscat

```bash
# Install wscat
npm install -g wscat

# Connect to server
wscat -c ws://localhost:8080

# Send message
> {"type":"terminal_create","payload":{"workingDirectory":"/"}}
```

### Using Postman

1. Create new WebSocket request
2. Connect to `ws://localhost:8080`
3. Send messages in JSON format

## Troubleshooting

### Common Issues

1. **Port already in use**
   - Change port in environment variable: `PORT=8081 npm start`

2. **Permission denied for file operations**
   - Make sure daemon has read/write permissions to target directories

3. **Terminal not working**
   - Check if bash/powershell is available in PATH
   - On Windows, use PowerShell instead of bash

## License

MIT
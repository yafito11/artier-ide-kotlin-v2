import Fastify from 'fastify';
import websocket from '@fastify/websocket';
import cors from '@fastify/cors';
import { WebSocket } from 'ws';

// Managers
import { PtyManager } from './pty/pty-manager';
import { CloudflaredManager } from './tunnel/cloudflared-manager';
import { SshTunnelManager } from './tunnel/ssh-tunnel-manager';
import { DatabaseManager } from './database/db-manager';
import { DbClient } from './database/db-client';
import { LspServerManager } from './lsp/lsp-server-manager';
import { SseManager } from './sse/sse-manager';
import { PkgManager } from './pkg/pkg-manager';
import { SkillManager } from './skills/skill-manager';

// Types
import { WsMessage } from './types';
import * as fs from 'fs';
import * as path from 'path';
import * as net from 'net';

// Initialize managers
const ptyManager = new PtyManager();
const cloudflaredManager = new CloudflaredManager();
const sshTunnelManager = new SshTunnelManager();
const dbManager = new DatabaseManager();
const dbClient = new DbClient();
const lspManager = new LspServerManager();
const sseManager = new SseManager();
const pkgManager = new PkgManager();
const skillManager = new SkillManager();

// Track WS connections per terminal session
const wsConnections: Map<string, WebSocket> = new Map();

// Create Fastify instance
const fastify = Fastify({
  logger: true,
});

// Register plugins
fastify.register(websocket);
fastify.register(cors, {
  origin: (origin, cb) => {
    // Allow no-origin (native app / curl) and loopback only
    if (!origin || /^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/.test(origin)) {
      cb(null, true);
      return;
    }
    cb(new Error('CORS blocked: origin not allowed'), false);
  },
});

// ==================== REST API Routes ====================

// Health check
fastify.get('/health', async () => {
  return {
    status: 'ok',
    version: '1.0.0',
    cloudflaredAvailable: cloudflaredManager.isAvailable(),
    pkgAvailable: pkgManager.isAvailable(),
  };
});

// Terminal endpoints
fastify.get('/api/terminal/sessions', async () => {
  return { sessions: ptyManager.getAllSessions() };
});

// Database connections
fastify.get('/api/db/connections', async () => {
  return { connections: dbClient.getAllConnections() };
});

// LSP servers
fastify.get('/api/lsp/servers', async () => {
  return { servers: lspManager.getRunningServers() };
});

// SSH tunnels
fastify.get('/api/ssh/tunnels', async () => {
  return { tunnels: sshTunnelManager.getAllTunnels() };
});

// Packages
fastify.get('/api/pkg/installed', async () => {
  return { packages: await pkgManager.getInstalledPackages() };
});

fastify.get<{ Params: { query: string } }>('/api/pkg/search/:query', async (req) => {
  return { packages: await pkgManager.searchPackages(req.params.query) };
});

fastify.post<{ Body: { packageName: string } }>('/api/pkg/install', async (req) => {
  const success = await pkgManager.installPackage(req.body.packageName);
  return { success, packageName: req.body.packageName };
});

// SSE agent stream endpoint
fastify.get<{ Params: { sessionId: string }; Querystring: { agent: string; input: string; workdir?: string } }>(
  '/api/agent/stream/:sessionId',
  async (req, reply) => {
    const { sessionId } = req.params;
    const { agent, input, workdir } = req.query;

    SseManager.setHeaders(reply.raw);

    sseManager.addClient(sessionId, reply.raw);

    try {
      sseManager.spawnAgent(sessionId, agent, input, { workingDirectory: workdir });
    } catch (err: any) {
      sseManager.sendEvent(reply.raw, 'error', { message: err.message });
    }

    reply.hijack();
  }
);

fastify.post<{ Body: { sessionId: string } }>('/api/agent/kill', async (req) => {
  const { sessionId } = req.body;
  const killed = sseManager.killSession(sessionId);
  return { killed, sessionId };
});

fastify.get('/api/agent/active', async () => {
  return { sessions: sseManager.getActiveSessions() };
});

// DB proxy endpoints
fastify.post<{ Body: { type: string; host: string; port: number; database: string; username?: string; password?: string; url?: string; authToken?: string; connectionId: string } }>(
  '/api/db/connect',
  async (req) => {
    const { connectionId, type, host, port, database, username, password, url, authToken } = req.body;

    if (type === 'postgres') {
      await dbClient.connectPostgres(connectionId, { host, port, database, username, password });
    } else if (type === 'libsql') {
      await dbClient.connectLibsql(connectionId, { url: url || '', authToken });
    }

    return { connected: true, connectionId };
  }
);

fastify.post<{ Body: { connectionId: string } }>('/api/db/disconnect', async (req) => {
  const { connectionId } = req.body;
  const disconnected = dbClient.disconnect(connectionId);
  return { disconnected, connectionId };
});

fastify.post<{ Body: { connectionId: string; query: string; params?: any[] } }>(
  '/api/db/query',
  async (req) => {
    const { connectionId, query, params } = req.body;
    const result = await dbClient.query(connectionId, query, params);
    return result;
  }
);

fastify.get<{ Params: { connectionId: string } }>('/api/db/tables/:connectionId', async (req) => {
  const { connectionId } = req.params;
  const tables = await dbClient.getTables(connectionId);
  return { tables };
});

fastify.get<{ Params: { connectionId: string; tableName: string } }>(
  '/api/db/schema/:connectionId/:tableName',
  async (req) => {
    const { connectionId, tableName } = req.params;
    const schema = await dbClient.getTableSchema(connectionId, tableName);
    return { schema };
  }
);

// Skills endpoints (agentskills.io SKILL.md)
fastify.get('/api/skills', async () => {
  return { skills: skillManager.list() };
});

fastify.post('/api/skills/scan', async (req) => {
  const body = (req.body || {}) as { projectRoot?: string };
  if (body.projectRoot) skillManager.setProjectRoot(body.projectRoot);
  return { skills: skillManager.scan() };
});

fastify.get<{ Params: { name: string } }>('/api/skills/:name', async (req) => {
  const skill = skillManager.get(req.params.name);
  if (!skill) {
    return { error: 'Skill not found' };
  }
  return { skill };
});

fastify.post<{ Body: { path?: string; url?: string } }>('/api/skills/install', async (req) => {
  const { path: skillPath, url } = req.body || {};
  if (skillPath) {
    const skill = skillManager.installFromPath(skillPath);
    return { installed: true, skill };
  }
  if (url) {
    const skill = await skillManager.installFromUrl(url);
    return { installed: true, skill };
  }
  return { installed: false, error: 'Provide path or url' };
});

fastify.post<{ Body: { name: string; enabled: boolean } }>('/api/skills/enable', async (req) => {
  const { name, enabled } = req.body;
  const skill = skillManager.setEnabled(name, enabled);
  if (!skill) return { ok: false, error: 'Skill not found' };
  return { ok: true, skill };
});

fastify.delete<{ Params: { name: string } }>('/api/skills/:name', async (req) => {
  try {
    const removed = skillManager.uninstall(req.params.name);
    return { deleted: removed, name: req.params.name };
  } catch (e: any) {
    return { deleted: false, error: e.message };
  }
});

fastify.get('/api/skills/context', async () => {
  return { context: skillManager.buildAgentContext(true) };
});

// Settings endpoints
fastify.get<{ Querystring: { key: string; defaultValue?: string } }>('/api/settings', async (req) => {
  const { key, defaultValue } = req.query;
  const value = dbManager.getSetting(key, defaultValue || '');
  return { key, value };
});

fastify.post<{ Body: { key: string; value: string } }>('/api/settings', async (req) => {
  const { key, value } = req.body;
  dbManager.setSetting(key, value);
  return { saved: true, key };
});

// Project endpoints
fastify.get('/api/projects/recent', async () => {
  return { projects: dbManager.getRecentProjects(10) };
});

fastify.post<{ Body: { id?: string; name: string; path: string } }>('/api/projects/save', async (req) => {
  const project = {
    id: req.body.id || `proj_${Date.now()}`,
    name: req.body.name,
    path: req.body.path,
  };
  dbManager.saveProject(project);
  return { saved: true, project };
});

// Chat session endpoints
fastify.get('/api/chat/sessions', async () => {
  return { sessions: dbManager.getAllChatSessions() };
});

fastify.post<{ Body: { id?: string; title?: string; agentName?: string } }>('/api/chat/sessions', async (req) => {
  const session = {
    id: req.body.id || `chat_${Date.now()}`,
    title: req.body.title || 'New Chat',
    agentName: req.body.agentName || 'opencode',
    createdAt: Date.now(),
    updatedAt: Date.now(),
    isActive: true,
  };
  dbManager.createChatSession(session);
  return { session };
});

fastify.get<{ Params: { sessionId: string } }>('/api/chat/messages/:sessionId', async (req) => {
  const { sessionId } = req.params;
  const messages = dbManager.getChatMessages(sessionId);
  return { sessionId, messages };
});

fastify.post<{ Body: { sessionId: string; role: string; content: string; agentName?: string } }>(
  '/api/chat/messages',
  async (req) => {
    const message = {
      id: `msg_${Date.now()}`,
      sessionId: req.body.sessionId,
      role: req.body.role,
      content: req.body.content,
      agentName: req.body.agentName,
      timestamp: Date.now(),
    };
    dbManager.createChatMessage(message);
    return { message };
  }
);

fastify.delete<{ Params: { sessionId: string } }>('/api/chat/sessions/:sessionId', async (req) => {
  const { sessionId } = req.params;
  dbManager.deleteChatSession(sessionId);
  return { deleted: true, sessionId };
});

// ==================== WebSocket Handler ====================

fastify.register(async function (fastify) {
  fastify.get('/ws', { websocket: true }, (socket: WebSocket) => {
    console.log('[Daemon] WebSocket client connected');

    socket.on('message', (message: Buffer) => {
      try {
        const data: WsMessage = JSON.parse(message.toString());
        handleWsMessage(socket, data);
      } catch (error: any) {
        console.error('[Daemon] Error parsing message:', error.message);
        socket.send(JSON.stringify({
          type: 'error',
          payload: { message: 'Invalid message format' }
        }));
      }
    });

    socket.on('close', () => {
      console.log('[Daemon] WebSocket client disconnected');
      // Clean up terminal connections owned by this socket
      for (const [sessionId, conn] of wsConnections) {
        if (conn === socket) {
          wsConnections.delete(sessionId);
        }
      }
    });

    // Send ready signal
    wsSend(socket, 'daemon_ready', {
      version: '1.0.0',
      features: ['terminal', 'files', 'tunnel', 'router', 'agent', 'lsp', 'database', 'ssh', 'pkg', 'sse', 'skills'],
      cloudflaredAvailable: cloudflaredManager.isAvailable(),
      pkgAvailable: pkgManager.isAvailable(),
      skillCount: skillManager.list().length,
    });
  });
});

// ==================== WebSocket Message Handler ====================

function handleWsMessage(ws: WebSocket, data: WsMessage): void {
  const { type, payload } = data;

  switch (type) {
    // Terminal
    case 'terminal_create': handleTerminalCreate(ws, payload); break;
    case 'terminal_input': handleTerminalInput(payload); break;
    case 'terminal_resize': handleTerminalResize(payload); break;
    case 'terminal_close': handleTerminalClose(payload); break;
    case 'terminal_list': handleTerminalList(ws); break;

    // Files
    case 'file_read': handleFileRead(ws, payload); break;
    case 'file_write': handleFileWrite(ws, payload); break;
    case 'directory_list': handleDirectoryList(ws, payload); break;
    case 'file_create': handleFileCreate(ws, payload); break;
    case 'directory_create': handleDirectoryCreate(ws, payload); break;
    case 'file_delete': handleFileDelete(ws, payload); break;
    case 'file_rename': handleFileRename(ws, payload); break;

    // Tunnel
    case 'tunnel_create': handleTunnelCreate(ws, payload); break;
    case 'tunnel_close': handleTunnelClose(ws, payload); break;
    case 'tunnel_status': handleTunnelStatus(ws, payload); break;
    case 'tunnel_download': handleTunnelDownload(ws, payload); break;

    // SSH Tunnel
    case 'ssh_tunnel_create': handleSshTunnelCreate(ws, payload); break;
    case 'ssh_tunnel_close': handleSshTunnelClose(ws, payload); break;
    case 'ssh_tunnel_list': handleSshTunnelList(ws); break;

    // Port detection
    case 'port_detect': handlePortDetect(ws, payload); break;

    // LSP
    case 'lsp_initialize': handleLspInitialize(ws, payload); break;
    case 'lsp_completion': handleLspCompletion(ws, payload); break;
    case 'lsp_hover': handleLspHover(ws, payload); break;
    case 'lsp_document_symbols': handleLspDocumentSymbols(ws, payload); break;
    case 'lsp_format': handleLspFormat(ws, payload); break;
    case 'lsp_did_open': handleLspDidOpen(payload); break;
    case 'lsp_did_change': handleLspDidChange(payload); break;
    case 'lsp_did_save': handleLspDidSave(payload); break;
    case 'lsp_did_close': handleLspDidClose(payload); break;
    case 'lsp_stop': handleLspStop(ws, payload); break;

    // Database
    case 'db_connect': handleDbConnect(ws, payload); break;
    case 'db_disconnect': handleDbDisconnect(ws, payload); break;
    case 'db_query': handleDbQuery(ws, payload); break;
    case 'db_tables': handleDbTables(ws, payload); break;
    case 'db_schema': handleDbSchema(ws, payload); break;

    // Chat
    case 'db_chat_create_session': handleDbChatCreateSession(ws, payload); break;
    case 'db_chat_get_sessions': handleDbChatGetSessions(ws); break;
    case 'db_chat_add_message': handleDbChatAddMessage(ws, payload); break;
    case 'db_chat_get_messages': handleDbChatGetMessages(ws, payload); break;
    case 'db_chat_delete_session': handleDbChatDeleteSession(ws, payload); break;

    // Settings
    case 'settings_get': handleSettingsGet(ws, payload); break;
    case 'settings_set': handleSettingsSet(ws, payload); break;

    // Projects
    case 'project_save': handleProjectSave(ws, payload); break;
    case 'project_recent': handleProjectRecent(ws); break;

    // Pkg
    case 'pkg_search': handlePkgSearch(ws, payload); break;
    case 'pkg_install': handlePkgInstall(ws, payload); break;
    case 'pkg_installed': handlePkgInstalled(ws); break;

    // Skills
    case 'skill_list': handleSkillList(ws); break;
    case 'skill_scan': handleSkillScan(ws, payload); break;
    case 'skill_get': handleSkillGet(ws, payload); break;
    case 'skill_install': handleSkillInstall(ws, payload); break;
    case 'skill_uninstall': handleSkillUninstall(ws, payload); break;
    case 'skill_set_enabled': handleSkillSetEnabled(ws, payload); break;
    case 'skill_context': handleSkillContext(ws); break;

    default:
      console.log('[Daemon] Unknown message type:', type);
      wsSend(ws, 'error', { message: `Unknown message type: ${type}` });
  }
}

// ==================== Terminal Handlers ====================

function handleTerminalCreate(ws: WebSocket, payload: any): void {
  const sessionId = payload.sessionId || `term_${Date.now()}`;
  const { workingDirectory = '/', cols = 80, rows = 24 } = payload;

  try {
    const session = ptyManager.createSession(sessionId, { workingDirectory, cols, rows });
    wsConnections.set(sessionId, ws);

    if (session.isFallback) {
      session.process.stdout.on('data', (data: Buffer) => {
        wsSend(ws, 'terminal_output', { sessionId, data: data.toString() });
      });
      session.process.stderr.on('data', (data: Buffer) => {
        wsSend(ws, 'terminal_output', { sessionId, data: data.toString() });
      });
      session.process.on('exit', (code: number) => {
        wsSend(ws, 'terminal_exit', { sessionId, exitCode: code });
        wsConnections.delete(sessionId);
      });
    } else {
      session.process.onData((data: string) => {
        wsSend(ws, 'terminal_output', { sessionId, data });
      });
      session.process.onExit(({ exitCode }: { exitCode: number }) => {
        wsSend(ws, 'terminal_exit', { sessionId, exitCode });
        wsConnections.delete(sessionId);
      });
    }

    dbManager.saveTerminalSession({
      id: sessionId,
      workingDirectory,
      shell: session.shell,
      cols,
      rows,
    });

    wsSend(ws, 'terminal_created', { sessionId, shell: session.shell });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `Failed to create terminal: ${error.message}` });
  }
}

function handleTerminalInput(payload: any): void {
  ptyManager.write(payload.sessionId, payload.data);
}

function handleTerminalResize(payload: any): void {
  ptyManager.resize(payload.sessionId, payload.cols, payload.rows);
}

function handleTerminalClose(payload: any): void {
  ptyManager.kill(payload.sessionId);
  dbManager.deleteTerminalSession(payload.sessionId);
  wsConnections.delete(payload.sessionId);
}

function handleTerminalList(ws: WebSocket): void {
  wsSend(ws, 'terminal_list', { sessions: ptyManager.getAllSessions() });
}

// ==================== File Handlers ====================

function handleFileRead(ws: WebSocket, payload: any): void {
  fs.readFile(payload.path, 'utf8', (err, data) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to read file: ${err.message}` }); return; }
    wsSend(ws, 'file_content', { path: payload.path, content: data });
  });
}

function handleFileWrite(ws: WebSocket, payload: any): void {
  fs.writeFile(payload.path, payload.content, 'utf8', (err) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to write file: ${err.message}` }); return; }
    wsSend(ws, 'file_saved', { path: payload.path });
  });
}

function handleDirectoryList(ws: WebSocket, payload: any): void {
  fs.readdir(payload.path, { withFileTypes: true }, (err, entries) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to list directory: ${err.message}` }); return; }
    const items = entries.map(e => ({ name: e.name, isDirectory: e.isDirectory(), path: path.join(payload.path, e.name) }));
    wsSend(ws, 'directory_listing', { path: payload.path, items });
  });
}

function handleFileCreate(ws: WebSocket, payload: any): void {
  fs.writeFile(payload.path, '', 'utf8', (err) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to create file: ${err.message}` }); return; }
    wsSend(ws, 'file_created', { path: payload.path });
  });
}

function handleDirectoryCreate(ws: WebSocket, payload: any): void {
  fs.mkdir(payload.path, { recursive: true }, (err) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to create directory: ${err.message}` }); return; }
    wsSend(ws, 'directory_created', { path: payload.path });
  });
}

function handleFileDelete(ws: WebSocket, payload: any): void {
  fs.unlink(payload.path, (err) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to delete file: ${err.message}` }); return; }
    wsSend(ws, 'file_deleted', { path: payload.path });
  });
}

function handleFileRename(ws: WebSocket, payload: any): void {
  fs.rename(payload.oldPath, payload.newPath, (err) => {
    if (err) { wsSend(ws, 'error', { message: `Failed to rename file: ${err.message}` }); return; }
    wsSend(ws, 'file_renamed', { oldPath: payload.oldPath, newPath: payload.newPath });
  });
}

// ==================== Tunnel Handlers ====================

function handleTunnelCreate(ws: WebSocket, payload: any): void {
  const { port, service = 'http' } = payload;
  const tunnelId = `tunnel_${Date.now()}`;

  if (!cloudflaredManager.isAvailable()) {
    wsSend(ws, 'error', { message: 'cloudflared not found. Please download it first using tunnel_download.' });
    return;
  }

  try {
    const tunnel = cloudflaredManager.createTunnel(tunnelId, port);

    const checkUrl = setInterval(() => {
      if (tunnel.url) {
        clearInterval(checkUrl);
        wsSend(ws, 'tunnel_created', { tunnelId, url: tunnel.url, port, service });
      }
    }, 100);

    setTimeout(() => { clearInterval(checkUrl); }, 30000);

    tunnel.process.on('exit', (code: number) => {
      clearInterval(checkUrl);
      wsSend(ws, 'tunnel_closed', { tunnelId, exitCode: code });
    });

    wsSend(ws, 'tunnel_pending', { tunnelId, port });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `Failed to create tunnel: ${error.message}` });
  }
}

function handleTunnelClose(ws: WebSocket, payload: any): void {
  const success = cloudflaredManager.closeTunnel(payload.tunnelId);
  wsSend(ws, 'tunnel_closed', { tunnelId: payload.tunnelId, success });
}

function handleTunnelStatus(ws: WebSocket, payload: any): void {
  const tunnel = cloudflaredManager.getTunnel(payload.tunnelId);
  if (tunnel) {
    wsSend(ws, 'tunnel_status', {
      tunnelId: payload.tunnelId, status: tunnel.status, url: tunnel.url,
      port: tunnel.port, uptime: Math.floor((Date.now() - tunnel.createdAt) / 1000),
    });
  } else {
    wsSend(ws, 'error', { message: `Tunnel ${payload.tunnelId} not found` });
  }
}

async function handleTunnelDownload(ws: WebSocket, _payload: any): Promise<void> {
  wsSend(ws, 'tunnel_download_progress', { status: 'starting', message: 'Starting download...' });
  try {
    const binaryPath = await cloudflaredManager.download((progress) => {
      wsSend(ws, 'tunnel_download_progress', { status: 'downloading', ...progress });
    });
    wsSend(ws, 'tunnel_download_complete', { path: binaryPath, message: 'Download complete' });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `Download failed: ${error.message}` });
  }
}

// ==================== SSH Tunnel Handlers ====================

async function handleSshTunnelCreate(ws: WebSocket, payload: any): Promise<void> {
  const tunnelId = `ssh_${Date.now()}`;
  try {
    const tunnel = await sshTunnelManager.createTunnel(tunnelId, payload);
    wsSend(ws, 'ssh_tunnel_created', {
      tunnelId: tunnel.id, localPort: tunnel.localPort,
      remoteHost: tunnel.remoteHost, remotePort: tunnel.remotePort,
      sshHost: tunnel.sshHost, status: tunnel.status,
    });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `SSH tunnel failed: ${error.message}` });
  }
}

function handleSshTunnelClose(ws: WebSocket, payload: any): void {
  const success = sshTunnelManager.closeTunnel(payload.tunnelId);
  wsSend(ws, 'ssh_tunnel_closed', { tunnelId: payload.tunnelId, success });
}

function handleSshTunnelList(ws: WebSocket): void {
  wsSend(ws, 'ssh_tunnel_list', { tunnels: sshTunnelManager.getAllTunnels() });
}

// ==================== Port Detection ====================

function handlePortDetect(ws: WebSocket, _payload: any): void {
  const commonPorts = [
    { port: 3000, service: 'npm' }, { port: 5000, service: 'flask' },
    { port: 8000, service: 'django' }, { port: 8080, service: 'java' },
    { port: 4200, service: 'angular' }, { port: 5173, service: 'vite' },
    { port: 5174, service: 'vite' }, { port: 3001, service: 'next.js' },
    { port: 4000, service: 'graphql' },
  ];

  const checkedPorts: any[] = [];
  let checked = 0;

  commonPorts.forEach((portInfo) => {
    const tester = net.createServer()
      .once('error', () => { checkedPorts.push({ ...portInfo, active: true }); checkDone(); })
      .once('listening', () => { tester.close(); checkedPorts.push({ ...portInfo, active: false }); checkDone(); })
      .listen(portInfo.port, '127.0.0.1');
  });

  function checkDone(): void {
    checked++;
    if (checked === commonPorts.length) {
      wsSend(ws, 'ports_detected', { ports: checkedPorts.filter(p => p.active) });
    }
  }
}

// ==================== LSP Handlers ====================

async function handleLspInitialize(ws: WebSocket, payload: any): Promise<void> {
  const { language, rootUri } = payload;
  try {
    const serverId = `${language}_${rootUri}`;
    const existing = lspManager.getRunningServers().find(s => s.id === serverId);
    if (existing) { wsSend(ws, 'lsp_initialized', { serverId, language }); return; }

    lspManager.startServer(language, rootUri);
    const result = await lspManager.initializeServer(serverId, rootUri);
    if (result) {
      wsSend(ws, 'lsp_initialized', { serverId, language, capabilities: result.capabilities });
    } else {
      wsSend(ws, 'error', { message: `Failed to initialize ${language} language server` });
    }
  } catch (error: any) {
    wsSend(ws, 'error', { message: `LSP initialization error: ${error.message}` });
  }
}

async function handleLspCompletion(ws: WebSocket, payload: any): Promise<void> {
  const serverId = `${payload.language}_${payload.rootUri}`;
  try {
    const result = await lspManager.sendRequest(serverId, 'textDocument/completion', {
      textDocument: { uri: payload.fileUri },
      position: { line: payload.line, character: payload.character },
    });
    wsSend(ws, 'lsp_completion', { fileUri: payload.fileUri, items: result?.items || [] });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `LSP completion error: ${error.message}` });
  }
}

async function handleLspHover(ws: WebSocket, payload: any): Promise<void> {
  const serverId = `${payload.language}_${payload.rootUri}`;
  try {
    const result = await lspManager.sendRequest(serverId, 'textDocument/hover', {
      textDocument: { uri: payload.fileUri },
      position: { line: payload.line, character: payload.character },
    });
    wsSend(ws, 'lsp_hover', { fileUri: payload.fileUri, contents: result?.contents, range: result?.range });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `LSP hover error: ${error.message}` });
  }
}

async function handleLspDocumentSymbols(ws: WebSocket, payload: any): Promise<void> {
  const serverId = `${payload.language}_${payload.rootUri}`;
  try {
    const result = await lspManager.sendRequest(serverId, 'textDocument/documentSymbol', {
      textDocument: { uri: payload.fileUri },
    });
    wsSend(ws, 'lsp_document_symbols', { fileUri: payload.fileUri, symbols: result });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `LSP symbols error: ${error.message}` });
  }
}

async function handleLspFormat(ws: WebSocket, payload: any): Promise<void> {
  const serverId = `${payload.language}_${payload.rootUri}`;
  try {
    const result = await lspManager.sendRequest(serverId, 'textDocument/formatting', {
      textDocument: { uri: payload.fileUri },
      options: { tabSize: 4, insertSpaces: true },
    });
    wsSend(ws, 'lsp_format', { fileUri: payload.fileUri, edits: result?.edits || [] });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `LSP format error: ${error.message}` });
  }
}

function handleLspDidOpen(payload: any): void {
  const serverId = `${payload.language}_${payload.rootUri}`;
  lspManager.sendNotification(serverId, 'textDocument/didOpen', {
    textDocument: { uri: payload.fileUri, languageId: payload.language, version: payload.version || 1, text: payload.content },
  });
}

function handleLspDidChange(payload: any): void {
  const serverId = `${payload.language}_${payload.rootUri}`;
  lspManager.sendNotification(serverId, 'textDocument/didChange', {
    textDocument: { uri: payload.fileUri, version: payload.version },
    contentChanges: [{ text: payload.content }],
  });
}

function handleLspDidSave(payload: any): void {
  const serverId = `${payload.language}_${payload.rootUri}`;
  const params: any = { textDocument: { uri: payload.fileUri } };
  if (payload.content) params.text = payload.content;
  lspManager.sendNotification(serverId, 'textDocument/didSave', params);
}

function handleLspDidClose(payload: any): void {
  const serverId = `${payload.language}_${payload.rootUri}`;
  lspManager.sendNotification(serverId, 'textDocument/didClose', { textDocument: { uri: payload.fileUri } });
}

function handleLspStop(ws: WebSocket, payload: any): void {
  const serverId = `${payload.language}_${payload.rootUri}`;
  const stopped = lspManager.stopServer(serverId);
  wsSend(ws, 'lsp_stopped', { serverId, stopped });
}

// ==================== Database Proxy Handlers ====================

async function handleDbConnect(ws: WebSocket, payload: any): Promise<void> {
  try {
    if (payload.type === 'postgres') {
      await dbClient.connectPostgres(payload.connectionId, {
        host: payload.host, port: payload.port, database: payload.database,
        username: payload.username, password: payload.password,
      });
    } else if (payload.type === 'libsql') {
      await dbClient.connectLibsql(payload.connectionId, { url: payload.url, authToken: payload.authToken });
    }
    wsSend(ws, 'db_connected', { connectionId: payload.connectionId });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `DB connect error: ${error.message}` });
  }
}

function handleDbDisconnect(ws: WebSocket, payload: any): void {
  const disconnected = dbClient.disconnect(payload.connectionId);
  wsSend(ws, 'db_disconnected', { connectionId: payload.connectionId, disconnected });
}

async function handleDbQuery(ws: WebSocket, payload: any): Promise<void> {
  try {
    const result = await dbClient.query(payload.connectionId, payload.query, payload.params);
    wsSend(ws, 'db_query_result', { connectionId: payload.connectionId, ...result });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `DB query error: ${error.message}` });
  }
}

async function handleDbTables(ws: WebSocket, payload: any): Promise<void> {
  try {
    const tables = await dbClient.getTables(payload.connectionId);
    wsSend(ws, 'db_tables', { connectionId: payload.connectionId, tables });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `DB tables error: ${error.message}` });
  }
}

async function handleDbSchema(ws: WebSocket, payload: any): Promise<void> {
  try {
    const schema = await dbClient.getTableSchema(payload.connectionId, payload.tableName);
    wsSend(ws, 'db_schema', { connectionId: payload.connectionId, tableName: payload.tableName, schema });
  } catch (error: any) {
    wsSend(ws, 'error', { message: `DB schema error: ${error.message}` });
  }
}

// ==================== Chat Handlers ====================

function handleDbChatCreateSession(ws: WebSocket, payload: any): void {
  const session = {
    id: payload.id || `chat_${Date.now()}`,
    title: payload.title || 'New Chat',
    agentName: payload.agentName || 'opencode',
    createdAt: Date.now(),
    updatedAt: Date.now(),
    isActive: true,
  };
  dbManager.createChatSession(session);
  wsSend(ws, 'db_chat_session_created', { session });
}

function handleDbChatGetSessions(ws: WebSocket): void {
  wsSend(ws, 'db_chat_sessions', { sessions: dbManager.getAllChatSessions() });
}

function handleDbChatAddMessage(ws: WebSocket, payload: any): void {
  const message = {
    id: payload.id || `msg_${Date.now()}`,
    sessionId: payload.sessionId,
    role: payload.role,
    content: payload.content,
    agentName: payload.agentName,
    timestamp: Date.now(),
  };
  dbManager.createChatMessage(message);
  wsSend(ws, 'db_chat_message_added', { message });
}

function handleDbChatGetMessages(ws: WebSocket, payload: any): void {
  wsSend(ws, 'db_chat_messages', {
    sessionId: payload.sessionId,
    messages: dbManager.getChatMessages(payload.sessionId, payload.limit || 100),
  });
}

function handleDbChatDeleteSession(ws: WebSocket, payload: any): void {
  dbManager.deleteChatSession(payload.sessionId);
  wsSend(ws, 'db_chat_session_deleted', { sessionId: payload.sessionId });
}

// ==================== Settings Handlers ====================

function handleSettingsGet(ws: WebSocket, payload: any): void {
  const value = dbManager.getSetting(payload.key, payload.defaultValue || '');
  wsSend(ws, 'settings_value', { key: payload.key, value });
}

function handleSettingsSet(ws: WebSocket, payload: any): void {
  dbManager.setSetting(payload.key, payload.value);
  wsSend(ws, 'settings_saved', { key: payload.key });
}

// ==================== Project Handlers ====================

function handleProjectSave(ws: WebSocket, payload: any): void {
  const project = { id: payload.id || `proj_${Date.now()}`, name: payload.name, path: payload.path };
  dbManager.saveProject(project);
  wsSend(ws, 'project_saved', { project });
}

function handleProjectRecent(ws: WebSocket): void {
  wsSend(ws, 'project_list', { projects: dbManager.getRecentProjects(10) });
}

// ==================== Pkg Handlers ====================

async function handlePkgSearch(ws: WebSocket, payload: any): Promise<void> {
  const packages = await pkgManager.searchPackages(payload.query);
  wsSend(ws, 'pkg_search_results', { query: payload.query, packages });
}

async function handlePkgInstall(ws: WebSocket, payload: any): Promise<void> {
  wsSend(ws, 'pkg_install_progress', { packageName: payload.packageName, status: 'installing' });
  const success = await pkgManager.installPackage(payload.packageName, (output) => {
    wsSend(ws, 'pkg_install_output', { packageName: payload.packageName, output });
  });
  wsSend(ws, 'pkg_install_complete', { packageName: payload.packageName, success });
}

async function handlePkgInstalled(ws: WebSocket): Promise<void> {
  const packages = await pkgManager.getInstalledPackages();
  wsSend(ws, 'pkg_installed_list', { packages });
}

// ==================== Skill Handlers ====================

function handleSkillList(ws: WebSocket): void {
  wsSend(ws, 'skill_list', { skills: skillManager.list() });
}

function handleSkillScan(ws: WebSocket, payload: any): void {
  if (payload?.projectRoot) skillManager.setProjectRoot(payload.projectRoot);
  wsSend(ws, 'skill_list', { skills: skillManager.scan() });
}

function handleSkillGet(ws: WebSocket, payload: any): void {
  const skill = skillManager.get(payload?.name);
  if (!skill) {
    wsSend(ws, 'error', { message: `Skill not found: ${payload?.name}` });
    return;
  }
  wsSend(ws, 'skill_detail', { skill });
}

async function handleSkillInstall(ws: WebSocket, payload: any): Promise<void> {
  try {
    let skill;
    if (payload?.path) skill = skillManager.installFromPath(payload.path);
    else if (payload?.url) skill = await skillManager.installFromUrl(payload.url);
    else {
      wsSend(ws, 'error', { message: 'Provide path or url for skill install' });
      return;
    }
    wsSend(ws, 'skill_installed', { skill });
    wsSend(ws, 'skill_list', { skills: skillManager.list() });
  } catch (e: any) {
    wsSend(ws, 'error', { message: `Skill install failed: ${e.message}` });
  }
}

function handleSkillUninstall(ws: WebSocket, payload: any): void {
  try {
    const deleted = skillManager.uninstall(payload?.name);
    wsSend(ws, 'skill_uninstalled', { name: payload?.name, deleted });
    wsSend(ws, 'skill_list', { skills: skillManager.list() });
  } catch (e: any) {
    wsSend(ws, 'error', { message: e.message });
  }
}

function handleSkillSetEnabled(ws: WebSocket, payload: any): void {
  const skill = skillManager.setEnabled(payload?.name, !!payload?.enabled);
  if (!skill) {
    wsSend(ws, 'error', { message: `Skill not found: ${payload?.name}` });
    return;
  }
  wsSend(ws, 'skill_updated', { skill });
  wsSend(ws, 'skill_list', { skills: skillManager.list() });
}

function handleSkillContext(ws: WebSocket): void {
  wsSend(ws, 'skill_context', { context: skillManager.buildAgentContext(true) });
}

// ==================== Utility ====================

function wsSend(ws: WebSocket, type: string, payload: any): void {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, payload }));
  }
}

// ==================== Graceful Shutdown ====================

async function shutdown(): Promise<void> {
  console.log('[Daemon] Shutting down...');
  ptyManager.killAll();
  cloudflaredManager.killAll();
  sshTunnelManager.killAll();
  sseManager.killAll();
  lspManager.stopAll();
  dbClient.disconnectAll();
  dbManager.close();
  await fastify.close();
  process.exit(0);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

// ==================== Start Server ====================

async function start(): Promise<void> {
  try {
    await dbManager.init();
    console.log('[Daemon] Database initialized');

    const skills = skillManager.scan();
    console.log(`[Daemon] Skills loaded: ${skills.length}`);

    const port = parseInt(process.env.PORT || '8080', 10);
    const host = process.env.HOST || '127.0.0.1';
    await fastify.listen({ port, host });
    console.log(`[Daemon] Fastify server running on ${host}:${port}`);
    console.log(`[Daemon] WebSocket endpoint: ws://${host}:${port}/ws`);
    console.log(`[Daemon] REST API: http://${host}:${port}/api`);
    console.log(`[Daemon] Cloudflared available: ${cloudflaredManager.isAvailable()}`);
    console.log(`[Daemon] Pkg available: ${pkgManager.isAvailable()}`);
    console.log(`[Daemon] Skills: ${skills.map((s) => s.name).join(', ') || '(none)'}`);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
}

start();

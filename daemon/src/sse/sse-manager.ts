import { spawn, ChildProcess } from 'child_process';
import * as os from 'os';
import * as path from 'path';
import * as fs from 'fs';

export interface AgentStreamSession {
  id: string;
  agentName: string;
  process: ChildProcess;
  createdAt: number;
}

/**
 * SSE Manager - Server-Sent Events for streaming agent output
 * Provides SSE endpoint and manages agent process streams
 */
export class SseManager {
  private sessions: Map<string, AgentStreamSession> = new Map();
  private clients: Map<string, Set<any>> = new Map(); // sessionId -> Set of SSE response objects

  /**
   * Register an SSE client for a session
   */
  addClient(sessionId: string, res: any): void {
    if (!this.clients.has(sessionId)) {
      this.clients.set(sessionId, new Set());
    }
    this.clients.get(sessionId)!.add(res);

    // Send initial connection event
    this.sendEvent(res, 'connected', { sessionId, timestamp: Date.now() });

    // Handle client disconnect
    res.on('close', () => {
      this.clients.get(sessionId)?.delete(res);
      if (this.clients.get(sessionId)?.size === 0) {
        this.clients.delete(sessionId);
      }
    });
  }

  /**
   * Remove an SSE client
   */
  removeClient(sessionId: string, res: any): void {
    this.clients.get(sessionId)?.delete(res);
  }

  /**
   * Send SSE event to a response
   */
  sendEvent(res: any, eventName: string, data: any): void {
    if (res.writableEnded) return;
    res.write(`event: ${eventName}\ndata: ${JSON.stringify(data)}\n\n`);
  }

  /**
   * Broadcast event to all clients of a session
   */
  broadcast(sessionId: string, eventName: string, data: any): void {
    const clients = this.clients.get(sessionId);
    if (!clients) return;

    for (const client of clients) {
      this.sendEvent(client, eventName, data);
    }
  }

  /**
   * Spawn an agent process and stream output via SSE
   */
  spawnAgent(
    sessionId: string,
    agentName: string,
    input: string,
    options: {
      workingDirectory?: string;
      env?: Record<string, string>;
    } = {}
  ): AgentStreamSession {
    const { workingDirectory = os.homedir(), env = {} } = options;

    let command: string;
    let args: string[];

    switch (agentName.toLowerCase()) {
      case 'opencode':
        command = 'opencode';
        args = ['--non-interactive', input];
        break;
      case 'claude':
      case 'claude-code':
        command = 'claude';
        args = ['-p', input];
        break;
      case 'hermes':
        command = 'hermes';
        args = ['--chat', input];
        break;
      default:
        throw new Error(`Unknown agent: ${agentName}`);
    }

    // Point CLI agents at local 9Router (loopback only)
    const routerBase = process.env.ARTIER_ROUTER_URL || 'http://127.0.0.1:20128/v1';
    const agentProcess = spawn(command, args, {
      cwd: workingDirectory,
      env: {
        ...process.env,
        OPENAI_BASE_URL: routerBase,
        OPENAI_API_BASE: routerBase,
        OPENAI_API_BASE_URL: routerBase,
        ANTHROPIC_BASE_URL: routerBase,
        ANTHROPIC_API_BASE: routerBase,
        LLM_BASE_URL: routerBase,
        ARTIER_ROUTER_URL: routerBase,
        ...env,
      },
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    const session: AgentStreamSession = {
      id: sessionId,
      agentName,
      process: agentProcess,
      createdAt: Date.now(),
    };

    this.sessions.set(sessionId, session);

    // Stream stdout
    agentProcess.stdout.on('data', (data: Buffer) => {
      this.broadcast(sessionId, 'output', {
        type: 'stdout',
        data: data.toString(),
        timestamp: Date.now(),
      });
    });

    // Stream stderr
    agentProcess.stderr.on('data', (data: Buffer) => {
      this.broadcast(sessionId, 'output', {
        type: 'stderr',
        data: data.toString(),
        timestamp: Date.now(),
      });
    });

    // Handle exit
    agentProcess.on('exit', (code, signal) => {
      this.broadcast(sessionId, 'exit', {
        exitCode: code,
        signal,
        timestamp: Date.now(),
      });
      this.sessions.delete(sessionId);
    });

    // Handle error
    agentProcess.on('error', (err) => {
      this.broadcast(sessionId, 'error', {
        message: err.message,
        timestamp: Date.now(),
      });
      this.sessions.delete(sessionId);
    });

    console.log(`[SSE] Agent ${agentName} spawned for session ${sessionId}`);
    return session;
  }

  /**
   * Kill an agent session
   */
  killSession(sessionId: string): boolean {
    const session = this.sessions.get(sessionId);
    if (!session) return false;

    try {
      session.process.kill('SIGTERM');
    } catch (e) {
      // Process may already be dead
    }

    this.broadcast(sessionId, 'killed', { sessionId, timestamp: Date.now() });
    this.sessions.delete(sessionId);
    return true;
  }

  /**
   * Get all active sessions
   */
  getActiveSessions(): Omit<AgentStreamSession, 'process'>[] {
    return Array.from(this.sessions.values()).map(s => ({
      id: s.id,
      agentName: s.agentName,
      createdAt: s.createdAt,
    }));
  }

  /**
   * Kill all sessions
   */
  killAll(): void {
    for (const [id] of this.sessions) {
      this.killSession(id);
    }
  }

  /**
   * Format SSE response headers
   */
  static setHeaders(res: any): void {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Cache-Control',
    });
    res.flushHeaders();
  }
}

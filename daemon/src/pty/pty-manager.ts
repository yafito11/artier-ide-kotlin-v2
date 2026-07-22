import * as os from 'os';
import * as path from 'path';
import * as fs from 'fs';
import { TerminalSessionData } from '../types';

export class PtyManager {
  private sessions: Map<string, TerminalSessionData> = new Map();
  private pty: any = null;

  constructor() {
    try {
      this.pty = require('node-pty');
    } catch (e) {
      console.warn('[PTY] node-pty not available, using fallback mode');
    }
  }

  createSession(sessionId: string, options: {
    workingDirectory?: string;
    cols?: number;
    rows?: number;
    env?: Record<string, string>;
  } = {}): TerminalSessionData {
    const {
      workingDirectory = os.homedir(),
      cols = 80,
      rows = 24,
      env = {}
    } = options;

    const shell = this.detectShell();

    if (this.pty) {
      const ptyProcess = this.pty.spawn(shell, [], {
        name: 'xterm-256color',
        cols,
        rows,
        cwd: workingDirectory,
        env: {
          ...process.env,
          ...env,
          TERM: 'xterm-256color',
          COLORTERM: 'truecolor',
          LANG: 'en_US.UTF-8',
        },
      });

      const session: TerminalSessionData = {
        id: sessionId,
        process: ptyProcess,
        shell,
        cols,
        rows,
        isFallback: false,
        createdAt: Date.now(),
        lastActivity: Date.now(),
      };

      this.sessions.set(sessionId, session);
      console.log(`[PTY] Session ${sessionId} created: ${shell} in ${workingDirectory}`);
      return session;
    }

    return this.createFallbackSession(sessionId, options);
  }

  private createFallbackSession(sessionId: string, options: {
    workingDirectory?: string;
    cols?: number;
    rows?: number;
  } = {}): TerminalSessionData {
    const { workingDirectory = os.homedir(), cols = 80, rows = 24 } = options;
    const { spawn } = require('child_process');

    const shell = process.platform === 'win32' ? 'powershell.exe' : 'bash';
    const shellProcess = spawn(shell, [], {
      cwd: workingDirectory,
      env: process.env,
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    const session: TerminalSessionData = {
      id: sessionId,
      process: shellProcess,
      shell,
      cols,
      rows,
      isFallback: true,
      createdAt: Date.now(),
      lastActivity: Date.now(),
    };

    this.sessions.set(sessionId, session);
    console.log(`[PTY] Fallback session ${sessionId} created: ${shell}`);
    return session;
  }

  write(sessionId: string, data: string): boolean {
    const session = this.sessions.get(sessionId);
    if (!session) return false;

    session.lastActivity = Date.now();

    if (session.isFallback) {
      session.process.stdin.write(data);
    } else {
      session.process.write(data);
    }

    return true;
  }

  resize(sessionId: string, cols: number, rows: number): boolean {
    const session = this.sessions.get(sessionId);
    if (!session) return false;

    session.cols = cols;
    session.rows = rows;

    if (!session.isFallback && session.process.resize) {
      session.process.resize(cols, rows);
      console.log(`[PTY] Session ${sessionId} resized to ${cols}x${rows}`);
    }

    return true;
  }

  kill(sessionId: string): boolean {
    const session = this.sessions.get(sessionId);
    if (!session) return false;

    try {
      if (session.isFallback) {
        session.process.kill('SIGTERM');
      } else {
        session.process.kill();
      }
    } catch (e: any) {
      console.error(`[PTY] Error killing session ${sessionId}:`, e.message);
    }

    this.sessions.delete(sessionId);
    console.log(`[PTY] Session ${sessionId} killed`);
    return true;
  }

  getSession(sessionId: string): TerminalSessionData | undefined {
    return this.sessions.get(sessionId);
  }

  getAllSessions(): Omit<TerminalSessionData, 'process'>[] {
    return Array.from(this.sessions.values()).map(s => ({
      id: s.id,
      shell: s.shell,
      cols: s.cols,
      rows: s.rows,
      isFallback: s.isFallback,
      createdAt: s.createdAt,
      lastActivity: s.lastActivity,
    }));
  }

  killAll(): void {
    for (const [id] of this.sessions) {
      this.kill(id);
    }
  }

  cleanupIdle(timeoutMs: number = 30 * 60 * 1000): void {
    const now = Date.now();
    for (const [id, session] of this.sessions) {
      if (now - session.lastActivity > timeoutMs) {
        console.log(`[PTY] Cleaning up idle session ${id}`);
        this.kill(id);
      }
    }
  }

  private detectShell(): string {
    if (process.platform === 'win32') {
      return process.env.COMSPEC || 'powershell.exe';
    }

    const shells = [
      process.env.SHELL,
      '/bin/bash',
      '/bin/sh',
      '/usr/bin/bash',
      '/usr/bin/sh',
    ].filter(Boolean) as string[];

    for (const shell of shells) {
      try {
        if (fs.existsSync(shell)) {
          return shell;
        }
      } catch (e) {
        // ignore
      }
    }

    return '/bin/sh';
  }
}

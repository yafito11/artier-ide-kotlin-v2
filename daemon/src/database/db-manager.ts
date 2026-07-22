import * as path from 'path';
import * as os from 'os';
import * as fs from 'fs';
import { ChatSession, ChatMessage } from '../types';

export class DatabaseManager {
  private dbPath: string;
  private db: any = null;

  constructor() {
    this.dbPath = path.join(os.homedir(), '.artier', 'artier.db');
    this.ensureDirectory();
  }

  private ensureDirectory(): void {
    const dir = path.dirname(this.dbPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  }

  async init(): Promise<boolean> {
    try {
      const Database = require('better-sqlite3');
      this.db = new Database(this.dbPath);
      this.db.pragma('journal_mode = WAL');
      this.db.pragma('foreign_keys = ON');
      this.createTables();
      console.log(`[DB] Initialized at ${this.dbPath}`);
      return true;
    } catch (e: any) {
      console.error('[DB] Failed to initialize:', e.message);
      console.log('[DB] Falling back to in-memory storage');
      this.db = null;
      return false;
    }
  }

  private createTables(): void {
    if (!this.db) return;

    this.db.exec(`
      CREATE TABLE IF NOT EXISTS chat_sessions (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        agent_name TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,
        is_active INTEGER DEFAULT 1
      );

      CREATE TABLE IF NOT EXISTS chat_messages (
        id TEXT PRIMARY KEY,
        session_id TEXT NOT NULL,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        agent_name TEXT,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS terminal_sessions (
        id TEXT PRIMARY KEY,
        working_directory TEXT NOT NULL,
        shell TEXT NOT NULL,
        cols INTEGER DEFAULT 80,
        rows INTEGER DEFAULT 24,
        created_at INTEGER NOT NULL,
        last_activity INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL,
        updated_at INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS projects (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        path TEXT NOT NULL,
        last_opened INTEGER NOT NULL,
        created_at INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS api_keys (
        id TEXT PRIMARY KEY,
        provider TEXT NOT NULL,
        encrypted_key TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );
    `);

    console.log('[DB] Tables created/verified');
  }

  createChatSession(session: ChatSession): ChatSession {
    if (!this.db) return session;

    this.db.prepare(`
      INSERT INTO chat_sessions (id, title, agent_name, created_at, updated_at, is_active)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(session.id, session.title, session.agentName, session.createdAt, session.updatedAt, session.isActive ? 1 : 0);

    return session;
  }

  getChatSession(sessionId: string): ChatSession | null {
    if (!this.db) return null;

    const row = this.db.prepare('SELECT * FROM chat_sessions WHERE id = ?').get(sessionId);
    if (!row) return null;

    return {
      id: row.id,
      title: row.title,
      agentName: row.agent_name,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      isActive: row.is_active === 1,
    };
  }

  getAllChatSessions(): ChatSession[] {
    if (!this.db) return [];

    const rows = this.db.prepare('SELECT * FROM chat_sessions ORDER BY updated_at DESC').all();
    return rows.map((row: any) => ({
      id: row.id,
      title: row.title,
      agentName: row.agent_name,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      isActive: row.is_active === 1,
    }));
  }

  updateChatSession(sessionId: string, updates: Partial<ChatSession>): void {
    if (!this.db) return;

    const fields: string[] = [];
    const values: any[] = [];

    if (updates.title !== undefined) { fields.push('title = ?'); values.push(updates.title); }
    if (updates.agentName !== undefined) { fields.push('agent_name = ?'); values.push(updates.agentName); }
    if (updates.isActive !== undefined) { fields.push('is_active = ?'); values.push(updates.isActive ? 1 : 0); }

    fields.push('updated_at = ?');
    values.push(Date.now());
    values.push(sessionId);

    this.db.prepare(`UPDATE chat_sessions SET ${fields.join(', ')} WHERE id = ?`).run(...values);
  }

  deleteChatSession(sessionId: string): void {
    if (!this.db) return;
    this.db.prepare('DELETE FROM chat_messages WHERE session_id = ?').run(sessionId);
    this.db.prepare('DELETE FROM chat_sessions WHERE id = ?').run(sessionId);
  }

  createChatMessage(message: ChatMessage): ChatMessage {
    if (!this.db) return message;

    this.db.prepare(`
      INSERT INTO chat_messages (id, session_id, role, content, agent_name, timestamp)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(message.id, message.sessionId, message.role, message.content, message.agentName || null, message.timestamp);

    return message;
  }

  getChatMessages(sessionId: string, limit: number = 100): ChatMessage[] {
    if (!this.db) return [];

    const rows = this.db.prepare(
      'SELECT * FROM chat_messages WHERE session_id = ? ORDER BY timestamp ASC LIMIT ?'
    ).all(sessionId, limit);

    return rows.map((row: any) => ({
      id: row.id,
      sessionId: row.session_id,
      role: row.role,
      content: row.content,
      agentName: row.agent_name,
      timestamp: row.timestamp,
    }));
  }

  saveTerminalSession(session: { id: string; workingDirectory: string; shell: string; cols: number; rows: number }): void {
    if (!this.db) return;

    this.db.prepare(`
      INSERT OR REPLACE INTO terminal_sessions (id, working_directory, shell, cols, rows, created_at, last_activity)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(session.id, session.workingDirectory, session.shell, session.cols, session.rows, Date.now(), Date.now());
  }

  getTerminalSessions(): any[] {
    if (!this.db) return [];
    return this.db.prepare('SELECT * FROM terminal_sessions ORDER BY last_activity DESC').all();
  }

  deleteTerminalSession(sessionId: string): void {
    if (!this.db) return;
    this.db.prepare('DELETE FROM terminal_sessions WHERE id = ?').run(sessionId);
  }

  getSetting(key: string, defaultValue: string = ''): string {
    if (!this.db) return defaultValue;
    const row = this.db.prepare('SELECT value FROM settings WHERE key = ?').get(key);
    return row ? row.value : defaultValue;
  }

  setSetting(key: string, value: string): void {
    if (!this.db) return;
    this.db.prepare('INSERT OR REPLACE INTO settings (key, value, updated_at) VALUES (?, ?, ?)').run(key, value, Date.now());
  }

  saveProject(project: { id: string; name: string; path: string }): void {
    if (!this.db) return;
    this.db.prepare('INSERT OR REPLACE INTO projects (id, name, path, last_opened, created_at) VALUES (?, ?, ?, ?, ?)')
      .run(project.id, project.name, project.path, Date.now(), Date.now());
  }

  getRecentProjects(limit: number = 10): any[] {
    if (!this.db) return [];
    return this.db.prepare('SELECT * FROM projects ORDER BY last_opened DESC LIMIT ?').all(limit);
  }

  close(): void {
    if (this.db) {
      this.db.close();
      this.db = null;
      console.log('[DB] Closed');
    }
  }
}

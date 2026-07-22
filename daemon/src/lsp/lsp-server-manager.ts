import { spawn, ChildProcess } from 'child_process';
import * as path from 'path';
import * as fs from 'fs';
import { LspServerData, LspLanguageConfig } from '../types';

export class LspServerManager {
  private servers: Map<string, LspServerData> = new Map();
  private languageConfigs: Record<string, LspLanguageConfig>;

  constructor() {
    this.languageConfigs = {
      typescript: {
        command: 'typescript-language-server',
        args: ['--stdio'],
        languages: ['typescript', 'javascript', 'typescriptreact', 'javascriptreact'],
        extensions: ['.ts', '.tsx', '.js', '.jsx'],
      },
      python: {
        command: 'pylsp',
        args: [],
        languages: ['python'],
        extensions: ['.py'],
      },
      rust: {
        command: 'rust-analyzer',
        args: [],
        languages: ['rust'],
        extensions: ['.rs'],
      },
      go: {
        command: 'gopls',
        args: [],
        languages: ['go'],
        extensions: ['.go'],
      },
      java: {
        command: 'jdtls',
        args: [],
        languages: ['java'],
        extensions: ['.java'],
      },
      json: {
        command: 'vscode-json-language-server',
        args: ['--stdio'],
        languages: ['json'],
        extensions: ['.json'],
      },
      html: {
        command: 'vscode-html-language-server',
        args: ['--stdio'],
        languages: ['html'],
        extensions: ['.html'],
      },
      css: {
        command: 'vscode-css-language-server',
        args: ['--stdio'],
        languages: ['css'],
        extensions: ['.css'],
      },
      yaml: {
        command: 'yaml-language-server',
        args: ['--stdio'],
        languages: ['yaml'],
        extensions: ['.yaml', '.yml'],
      },
      markdown: {
        command: 'marksman',
        args: [],
        languages: ['markdown'],
        extensions: ['.md'],
      },
    };
  }

  detectLanguage(filePath: string): string | null {
    const ext = path.extname(filePath).toLowerCase();
    for (const [language, config] of Object.entries(this.languageConfigs)) {
      if (config.extensions.includes(ext)) {
        return language;
      }
    }
    return null;
  }

  startServer(language: string, rootUri: string): LspServerData {
    const config = this.languageConfigs[language];
    if (!config) throw new Error(`No configuration for language: ${language}`);

    const serverId = `${language}_${rootUri}`;

    if (this.servers.has(serverId)) {
      return this.servers.get(serverId)!;
    }

    const serverProcess = spawn(config.command, config.args, {
      cwd: rootUri,
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, NODE_OPTIONS: '--max-old-space-size=256' },
    });

    const server: LspServerData = {
      id: serverId,
      language,
      rootUri,
      process: serverProcess,
      isInitialized: false,
      capabilities: null,
      pendingRequests: new Map(),
      messageId: 0,
    };

    let buffer = '';
    serverProcess.stdout.on('data', (data: Buffer) => {
      buffer += data.toString();
      while (buffer.includes('Content-Length:')) {
        const headerEnd = buffer.indexOf('\r\n\r\n');
        if (headerEnd === -1) break;

        const header = buffer.substring(0, headerEnd);
        const contentLengthMatch = header.match(/Content-Length:\s*(\d+)/);
        if (!contentLengthMatch) break;

        const contentLength = parseInt(contentLengthMatch[1]);
        const messageStart = headerEnd + 4;

        if (buffer.length < messageStart + contentLength) break;

        const messageBody = buffer.substring(messageStart, messageStart + contentLength);
        buffer = buffer.substring(messageStart + contentLength);

        try {
          const message = JSON.parse(messageBody);
          this.handleMessage(server, message);
        } catch (e: any) {
          console.error(`[LSP] Error parsing message:`, e.message);
        }
      }
    });

    serverProcess.stderr.on('data', (data: Buffer) => {
      console.error(`[LSP][${language}] ${data.toString()}`);
    });

    serverProcess.on('exit', (code) => {
      console.log(`[LSP][${language}] Server exited with code ${code}`);
      this.servers.delete(serverId);
    });

    this.servers.set(serverId, server);
    console.log(`[LSP][${language}] Server started for ${rootUri}`);
    return server;
  }

  private handleMessage(server: LspServerData, message: any): void {
    if (message.id !== undefined && server.pendingRequests.has(message.id)) {
      const pending = server.pendingRequests.get(message.id)!;
      server.pendingRequests.delete(message.id);
      if (message.error) {
        pending.reject(new Error(message.error.message));
      } else {
        pending.resolve(message.result);
      }
    }
  }

  sendRequest(serverId: string, method: string, params: any): Promise<any> {
    return new Promise((resolve, reject) => {
      const server = this.servers.get(serverId);
      if (!server) { reject(new Error('Server not found')); return; }

      const id = ++server.messageId;
      server.pendingRequests.set(id, { resolve, reject });

      const message = JSON.stringify({ jsonrpc: '2.0', id, method, params });
      const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;
      server.process.stdin.write(header + message);

      setTimeout(() => {
        if (server.pendingRequests.has(id)) {
          server.pendingRequests.delete(id);
          reject(new Error('Request timeout'));
        }
      }, 30000);
    });
  }

  sendNotification(serverId: string, method: string, params: any): void {
    const server = this.servers.get(serverId);
    if (!server) return;

    const message = JSON.stringify({ jsonrpc: '2.0', method, params });
    const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;
    server.process.stdin.write(header + message);
  }

  async initializeServer(serverId: string, rootUri: string): Promise<any> {
    const server = this.servers.get(serverId);
    if (!server) return null;

    const params = {
      processId: process.pid,
      rootUri,
      capabilities: {
        textDocument: {
          completion: { completionItem: { snippetSupport: true, commitCharactersSupport: true, documentationFormat: ['markdown', 'plaintext'], deprecatedSupport: true, preselectSupport: true } },
          hover: { contentFormat: ['markdown', 'plaintext'] },
          signatureHelp: { signatureInformation: { documentationFormat: ['markdown', 'plaintext'] } },
          definition: { dynamicRegistration: false },
          references: { dynamicRegistration: false },
          documentSymbol: { hierarchicalDocumentSymbolSupport: true },
          formatting: { dynamicRegistration: false },
        },
      },
      clientInfo: { name: 'artier-ide', version: '1.0.0' },
    };

    try {
      const result = await this.sendRequest(serverId, 'initialize', params);
      server.isInitialized = true;
      server.capabilities = result.capabilities;
      this.sendNotification(serverId, 'initialized', {});
      console.log(`[LSP][${server.language}] Server initialized`);
      return result;
    } catch (e: any) {
      console.error(`[LSP][${server.language}] Initialization failed:`, e.message);
      return null;
    }
  }

  stopServer(serverId: string): boolean {
    const server = this.servers.get(serverId);
    if (!server) return false;

    try { server.process.kill('SIGTERM'); } catch (e) { /* ignore */ }
    this.servers.delete(serverId);
    return true;
  }

  stopAll(): void {
    for (const [id] of this.servers) {
      this.stopServer(id);
    }
  }

  getRunningServers(): Omit<LspServerData, 'process' | 'pendingRequests'>[] {
    return Array.from(this.servers.values()).map(s => ({
      id: s.id,
      language: s.language,
      rootUri: s.rootUri,
      isInitialized: s.isInitialized,
      capabilities: s.capabilities,
      messageId: s.messageId,
    }));
  }
}

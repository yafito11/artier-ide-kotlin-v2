import WebSocket from 'ws';

// ==================== WebSocket Types ====================

export interface WsMessage {
  type: string;
  payload?: any;
}

export interface WsResponse {
  type: string;
  payload: any;
}

// ==================== Terminal Types ====================

export interface TerminalSessionData {
  id: string;
  process: any;
  shell: string;
  cols: number;
  rows: number;
  isFallback: boolean;
  createdAt: number;
  lastActivity: number;
}

export interface TerminalCreatePayload {
  sessionId?: string;
  workingDirectory?: string;
  cols?: number;
  rows?: number;
}

export interface TerminalInputPayload {
  sessionId: string;
  data: string;
}

export interface TerminalResizePayload {
  sessionId: string;
  cols: number;
  rows: number;
}

export interface TerminalClosePayload {
  sessionId: string;
}

// ==================== File Types ====================

export interface FileReadPayload {
  path: string;
}

export interface FileWritePayload {
  path: string;
  content: string;
}

export interface DirectoryListPayload {
  path: string;
}

export interface FileRenamePayload {
  oldPath: string;
  newPath: string;
}

// ==================== Tunnel Types ====================

export interface TunnelCreatePayload {
  port: number;
  service?: string;
}

export interface TunnelClosePayload {
  tunnelId: string;
}

export interface TunnelStatusPayload {
  tunnelId: string;
}

export interface TunnelData {
  id: string;
  process: any;
  port: number;
  url: string | null;
  status: string;
  createdAt: number;
  lastActivity: number;
}

// ==================== SSH Tunnel Types ====================

export interface SshTunnelCreatePayload {
  localPort: number;
  remoteHost: string;
  remotePort: number;
  sshHost: string;
  sshPort?: number;
  sshUser?: string;
  sshKeyPath?: string;
}

export interface SshTunnelClosePayload {
  tunnelId: string;
}

// ==================== LSP Types ====================

export interface LspInitializePayload {
  language: string;
  rootUri: string;
}

export interface LspCompletionPayload {
  language: string;
  rootUri: string;
  fileUri: string;
  line: number;
  character: number;
}

export interface LspHoverPayload {
  language: string;
  rootUri: string;
  fileUri: string;
  line: number;
  character: number;
}

export interface LspDocumentSymbolsPayload {
  language: string;
  rootUri: string;
  fileUri: string;
}

export interface LspFormatPayload {
  language: string;
  rootUri: string;
  fileUri: string;
}

export interface LspDidOpenPayload {
  language: string;
  rootUri: string;
  fileUri: string;
  content: string;
  version?: number;
}

export interface LspDidChangePayload {
  language: string;
  rootUri: string;
  fileUri: string;
  content: string;
  version: number;
}

export interface LspDidSavePayload {
  language: string;
  rootUri: string;
  fileUri: string;
  content?: string;
}

export interface LspDidClosePayload {
  language: string;
  rootUri: string;
  fileUri: string;
}

export interface LspStopPayload {
  language: string;
  rootUri: string;
}

// ==================== Database Types ====================

export interface ChatSession {
  id: string;
  title: string;
  agentName: string;
  createdAt: number;
  updatedAt: number;
  isActive: boolean;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  agentName?: string;
  timestamp: number;
}

export interface DbChatCreateSessionPayload {
  id?: string;
  title?: string;
  agentName?: string;
}

export interface DbChatAddMessagePayload {
  id?: string;
  sessionId: string;
  role: string;
  content: string;
  agentName?: string;
}

export interface DbChatGetMessagesPayload {
  sessionId: string;
  limit?: number;
}

export interface DbChatDeleteSessionPayload {
  sessionId: string;
}

// ==================== DB Client Types ====================

export interface DbProxyQueryPayload {
  connectionId: string;
  query: string;
  params?: any[];
}

export interface DbProxyConnectPayload {
  type: 'postgres' | 'libsql';
  host: string;
  port: number;
  database: string;
  username?: string;
  password?: string;
  url?: string;
  authToken?: string;
}

export interface DbProxyDisconnectPayload {
  connectionId: string;
}

// ==================== Settings Types ====================

export interface SettingsGetPayload {
  key: string;
  defaultValue?: string;
}

export interface SettingsSetPayload {
  key: string;
  value: string;
}

// ==================== Project Types ====================

export interface ProjectSavePayload {
  id?: string;
  name: string;
  path: string;
}

// ==================== SSE Types ====================

export interface AgentStreamPayload {
  agentName: string;
  input: string;
  sessionId?: string;
}

// ==================== Pkg Types ====================

export interface PkgInstallPayload {
  packageName: string;
}

export interface PkgSearchPayload {
  query: string;
}

// ==================== Port Detection ====================

export interface DetectedPort {
  port: number;
  service: string;
  active: boolean;
}

// ==================== Completion Types ====================

export interface CompletionItem {
  label: string;
  kind?: number;
  detail?: string;
  documentation?: string;
  insertText?: string;
}

export interface Diagnostic {
  range: LspRange;
  severity: number;
  message: string;
  source?: string;
}

export interface LspRange {
  startLine: number;
  startCharacter: number;
  endLine: number;
  endCharacter: number;
}

export interface DocumentSymbol {
  name: string;
  kind: number;
  range: LspRange;
  children?: DocumentSymbol[];
}

export interface TextEdit {
  range: LspRange;
  newText: string;
}

// ==================== LSP Server Types ====================

export interface LspServerData {
  id: string;
  language: string;
  rootUri: string;
  process: any;
  isInitialized: boolean;
  capabilities: any;
  pendingRequests: Map<number, { resolve: Function; reject: Function }>;
  messageId: number;
}

export interface LspLanguageConfig {
  command: string;
  args: string[];
  languages: string[];
  extensions: string[];
}

// ==================== Manager Types ====================

export interface ManagerOptions {
  workingDirectory?: string;
  cols?: number;
  rows?: number;
  env?: Record<string, string>;
}

export interface PkgPackage {
  name: string;
  version: string;
  description: string;
  installed: boolean;
}

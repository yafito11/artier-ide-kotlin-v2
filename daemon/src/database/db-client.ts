import { EventEmitter } from 'events';

export interface DbConnection {
  id: string;
  type: 'postgres' | 'libsql';
  client: any;
  host: string;
  port: number;
  database: string;
  createdAt: number;
}

export interface QueryResult {
  rows: any[];
  rowCount: number;
  fields?: any[];
  duration: number;
}

export class DbClient extends EventEmitter {
  private connections: Map<string, DbConnection> = new Map();

  async connectPostgres(
    connectionId: string,
    config: { host: string; port: number; database: string; username?: string; password?: string }
  ): Promise<DbConnection> {
    const { Pool } = require('pg');

    const pool = new Pool({
      host: config.host,
      port: config.port,
      database: config.database,
      user: config.username,
      password: config.password,
      max: 5,
      idleTimeoutMillis: 30000,
    });

    // Test connection
    const client = await pool.connect();
    client.release();

    const connection: DbConnection = {
      id: connectionId,
      type: 'postgres',
      client: pool,
      host: config.host,
      port: config.port,
      database: config.database,
      createdAt: Date.now(),
    };

    this.connections.set(connectionId, connection);
    console.log(`[DB Client] PostgreSQL connected: ${config.host}:${config.port}/${config.database}`);
    return connection;
  }

  async connectLibsql(
    connectionId: string,
    config: { url: string; authToken?: string }
  ): Promise<DbConnection> {
    const { createClient } = require('@libsql/client');

    const client = createClient({
      url: config.url,
      authToken: config.authToken,
    });

    // Test connection
    await client.execute('SELECT 1');

    const connection: DbConnection = {
      id: connectionId,
      type: 'libsql',
      client,
      host: config.url,
      port: 0,
      database: config.url,
      createdAt: Date.now(),
    };

    this.connections.set(connectionId, connection);
    console.log(`[DB Client] libSQL connected: ${config.url}`);
    return connection;
  }

  async query(connectionId: string, sql: string, params: any[] = []): Promise<QueryResult> {
    const connection = this.connections.get(connectionId);
    if (!connection) {
      throw new Error(`Connection ${connectionId} not found`);
    }

    const startTime = Date.now();

    try {
      let result: any;

      if (connection.type === 'postgres') {
        result = await connection.client.query(sql, params);
        return {
          rows: result.rows,
          rowCount: result.rowCount,
          fields: result.fields,
          duration: Date.now() - startTime,
        };
      } else {
        // libsql
        result = await connection.client.execute({ sql, args: params });
        return {
          rows: result.rows,
          rowCount: result.rows.length,
          duration: Date.now() - startTime,
        };
      }
    } catch (error: any) {
      this.emit('error', { connectionId, error: error.message });
      throw error;
    }
  }

  async getTables(connectionId: string): Promise<string[]> {
    const connection = this.connections.get(connectionId);
    if (!connection) throw new Error(`Connection ${connectionId} not found`);

    if (connection.type === 'postgres') {
      const result = await this.query(connectionId,
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
      );
      return result.rows.map((r: any) => r.table_name);
    } else {
      const result = await this.query(connectionId,
        "SELECT name FROM sqlite_master WHERE type='table'"
      );
      return result.rows.map((r: any) => r.name);
    }
  }

  async getTableSchema(connectionId: string, tableName: string): Promise<any[]> {
    const connection = this.connections.get(connectionId);
    if (!connection) throw new Error(`Connection ${connectionId} not found`);

    if (connection.type === 'postgres') {
      const result = await this.query(connectionId,
        `SELECT column_name, data_type, is_nullable, column_default
         FROM information_schema.columns
         WHERE table_name = $1 AND table_schema = 'public'
         ORDER BY ordinal_position`,
        [tableName]
      );
      return result.rows;
    } else {
      const result = await this.query(connectionId, `PRAGMA table_info(${tableName})`);
      return result.rows;
    }
  }

  disconnect(connectionId: string): boolean {
    const connection = this.connections.get(connectionId);
    if (!connection) return false;

    try {
      if (connection.type === 'postgres') {
        connection.client.end();
      }
      // libsql client doesn't need explicit close
    } catch (e) {
      // ignore
    }

    this.connections.delete(connectionId);
    console.log(`[DB Client] Disconnected: ${connectionId}`);
    return true;
  }

  disconnectAll(): void {
    for (const [id] of this.connections) {
      this.disconnect(id);
    }
  }

  getConnection(connectionId: string): DbConnection | undefined {
    return this.connections.get(connectionId);
  }

  getAllConnections(): Omit<DbConnection, 'client'>[] {
    return Array.from(this.connections.values()).map(c => ({
      id: c.id,
      type: c.type,
      host: c.host,
      port: c.port,
      database: c.database,
      createdAt: c.createdAt,
    }));
  }
}

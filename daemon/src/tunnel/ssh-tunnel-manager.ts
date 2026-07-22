import { spawn, ChildProcess } from 'child_process';
import * as net from 'net';

export interface SshTunnelData {
  id: string;
  process: ChildProcess | null;
  localPort: number;
  remoteHost: string;
  remotePort: number;
  sshHost: string;
  sshPort: number;
  sshUser: string;
  sshKeyPath?: string;
  status: string;
  createdAt: number;
}

export class SshTunnelManager {
  private tunnels: Map<string, SshTunnelData> = new Map();

  async createTunnel(
    tunnelId: string,
    options: {
      localPort: number;
      remoteHost: string;
      remotePort: number;
      sshHost: string;
      sshPort?: number;
      sshUser?: string;
      sshKeyPath?: string;
    }
  ): Promise<SshTunnelData> {
    const {
      localPort,
      remoteHost,
      remotePort,
      sshHost,
      sshPort = 22,
      sshUser = process.env.USER || 'root',
      sshKeyPath,
    } = options;

    // Check if local port is available
    const isAvailable = await this.isPortAvailable(localPort);
    if (!isAvailable) {
      throw new Error(`Local port ${localPort} is already in use`);
    }

    // Build SSH command
    const args = [
      '-N', // No remote command
      '-L', `${localPort}:${remoteHost}:${remotePort}`,
      '-p', sshPort.toString(),
    ];

    if (sshKeyPath) {
      args.push('-i', sshKeyPath);
    }

    args.push(`${sshUser}@${sshHost}`);

    const sshProcess = spawn('ssh', args, {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    const tunnel: SshTunnelData = {
      id: tunnelId,
      process: sshProcess,
      localPort,
      remoteHost,
      remotePort,
      sshHost,
      sshPort,
      sshUser,
      sshKeyPath,
      status: 'connecting',
      createdAt: Date.now(),
    };

    this.tunnels.set(tunnelId, tunnel);

    // Handle output
    sshProcess.stdout?.on('data', (data: Buffer) => {
      console.log(`[SSH][${tunnelId}] ${data.toString()}`);
    });

    sshProcess.stderr?.on('data', (data: Buffer) => {
      const output = data.toString();
      console.error(`[SSH][${tunnelId}] ${output}`);

      // Check for connection established
      if (output.includes('Warning: Permanently added') || output.includes('established')) {
        tunnel.status = 'active';
      }
    });

    sshProcess.on('exit', (code) => {
      console.log(`[SSH][${tunnelId}] Process exited with code ${code}`);
      tunnel.status = 'closed';
      this.tunnels.delete(tunnelId);
    });

    sshProcess.on('error', (err) => {
      console.error(`[SSH][${tunnelId}] Error: ${err.message}`);
      tunnel.status = 'error';
    });

    // Wait briefly and check if connection was established
    return new Promise((resolve) => {
      setTimeout(() => {
        // If still connecting, assume it's working (SSH -N doesn't output on success)
        if (tunnel.status === 'connecting') {
          tunnel.status = 'active';
        }
        resolve(tunnel);
      }, 1500);
    });
  }

  closeTunnel(tunnelId: string): boolean {
    const tunnel = this.tunnels.get(tunnelId);
    if (!tunnel) return false;

    if (tunnel.process) {
      try {
        tunnel.process.kill('SIGTERM');
      } catch (e) {
        // Process may already be dead
      }
    }

    this.tunnels.delete(tunnelId);
    console.log(`[SSH][${tunnelId}] Tunnel closed`);
    return true;
  }

  getTunnel(tunnelId: string): SshTunnelData | undefined {
    return this.tunnels.get(tunnelId);
  }

  getAllTunnels(): Omit<SshTunnelData, 'process'>[] {
    return Array.from(this.tunnels.values()).map(t => ({
      id: t.id,
      localPort: t.localPort,
      remoteHost: t.remoteHost,
      remotePort: t.remotePort,
      sshHost: t.sshHost,
      sshPort: t.sshPort,
      sshUser: t.sshUser,
      sshKeyPath: t.sshKeyPath,
      status: t.status,
      createdAt: t.createdAt,
    }));
  }

  killAll(): void {
    for (const [id] of this.tunnels) {
      this.closeTunnel(id);
    }
  }

  private isPortAvailable(port: number): Promise<boolean> {
    return new Promise((resolve) => {
      const server = net.createServer();
      server.once('error', () => resolve(false));
      server.once('listening', () => {
        server.close();
        resolve(true);
      });
      server.listen(port, '127.0.0.1');
    });
  }
}

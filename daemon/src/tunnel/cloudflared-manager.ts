import * as os from 'os';
import * as fs from 'fs';
import * as path from 'path';
import { spawn, execSync } from 'child_process';
import https from 'https';
import { TunnelData } from '../types';

export class CloudflaredManager {
  private tunnels: Map<string, TunnelData> = new Map();
  private binaryPath: string | null = null;
  private downloadDir: string;

  constructor() {
    this.downloadDir = path.join(os.homedir(), '.artier', 'binaries');
  }

  getBinaryPath(): string | null {
    if (this.binaryPath && fs.existsSync(this.binaryPath)) {
      return this.binaryPath;
    }

    const possiblePaths = [
      path.join(this.downloadDir, this.getBinaryName()),
      '/usr/local/bin/cloudflared',
      '/usr/bin/cloudflared',
      process.platform === 'win32' ? 'C:\\Program Files\\cloudflared\\cloudflared.exe' : null,
    ].filter(Boolean) as string[];

    for (const p of possiblePaths) {
      if (fs.existsSync(p)) {
        this.binaryPath = p;
        return p;
      }
    }

    return null;
  }

  private getBinaryName(): string {
    return process.platform === 'win32' ? 'cloudflared.exe' : 'cloudflared';
  }

  private getDownloadUrl(): string {
    const platform = process.platform;
    const arch = process.arch;

    let osName: string;
    let archName: string;

    if (platform === 'linux') {
      osName = 'linux';
      archName = arch === 'arm64' ? 'arm64' : 'amd64';
    } else if (platform === 'darwin') {
      osName = 'macos';
      archName = arch === 'arm64' ? 'arm64' : 'amd64';
    } else if (platform === 'win32') {
      osName = 'windows';
      archName = 'amd64';
    } else {
      throw new Error(`Unsupported platform: ${platform}`);
    }

    return `https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-${osName}-${archName}`;
  }

  async download(progressCallback?: (progress: { downloaded: number; total: number; percent: number }) => void): Promise<string> {
    const binaryName = this.getBinaryName();
    const destPath = path.join(this.downloadDir, binaryName);

    if (!fs.existsSync(this.downloadDir)) {
      fs.mkdirSync(this.downloadDir, { recursive: true });
    }

    if (fs.existsSync(destPath)) {
      console.log(`[Cloudflared] Binary already exists at ${destPath}`);
      this.binaryPath = destPath;
      return destPath;
    }

    const url = this.getDownloadUrl();
    console.log(`[Cloudflared] Downloading from ${url}`);

    return new Promise((resolve, reject) => {
      const downloadFile = (downloadUrl: string) => {
        https.get(downloadUrl, (response) => {
          if (response.statusCode === 302 || response.statusCode === 301 || response.statusCode === 307) {
            const location = response.headers.location;
            if (!location) {
              reject(new Error('Redirect without location'));
              return;
            }
            downloadFile(location);
            return;
          }

          if (response.statusCode !== 200) {
            reject(new Error(`HTTP ${response.statusCode}`));
            return;
          }

          const file = fs.createWriteStream(destPath);
          const totalSize = parseInt(response.headers['content-length'] || '0', 10);
          let downloadedSize = 0;

          response.on('data', (chunk) => {
            downloadedSize += chunk.length;
            if (progressCallback && totalSize > 0) {
              progressCallback({
                downloaded: downloadedSize,
                total: totalSize,
                percent: Math.round((downloadedSize / totalSize) * 100),
              });
            }
          });

          response.pipe(file);

          file.on('finish', () => {
            file.close();
            if (process.platform !== 'win32') {
              fs.chmodSync(destPath, 0o755);
            }
            this.binaryPath = destPath;
            console.log(`[Cloudflared] Downloaded to ${destPath}`);
            resolve(destPath);
          });

          file.on('error', (err) => {
            fs.unlink(destPath, () => {});
            reject(err);
          });
        }).on('error', (err) => {
          fs.unlink(destPath, () => {});
          reject(err);
        });
      };

      downloadFile(url);
    });
  }

  isAvailable(): boolean {
    const binaryPath = this.getBinaryPath();
    if (!binaryPath) return false;

    try {
      execSync(`"${binaryPath}" --version`, { stdio: 'pipe', timeout: 5000 });
      return true;
    } catch (e) {
      return false;
    }
  }

  getVersion(): string | null {
    const binaryPath = this.getBinaryPath();
    if (!binaryPath) return null;

    try {
      const output = execSync(`"${binaryPath}" --version`, { stdio: 'pipe', timeout: 5000 });
      return output.toString().trim();
    } catch (e) {
      return null;
    }
  }

  createTunnel(tunnelId: string, port: number, options: { noTlsVerify?: boolean; protocol?: string } = {}): TunnelData {
    const binaryPath = this.getBinaryPath();
    if (!binaryPath) {
      throw new Error('cloudflared not found. Please install or download it first.');
    }

    const args = ['tunnel', '--url', `http://localhost:${port}`];
    if (options.noTlsVerify) args.push('--no-tls-verify');
    if (options.protocol) args.push('--protocol', options.protocol);

    const tunnelProcess = spawn(binaryPath, args, { stdio: ['pipe', 'pipe', 'pipe'] });

    const tunnel: TunnelData = {
      id: tunnelId,
      process: tunnelProcess,
      port,
      url: null,
      status: 'connecting',
      createdAt: Date.now(),
      lastActivity: Date.now(),
    };

    this.tunnels.set(tunnelId, tunnel);

    tunnelProcess.stdout.on('data', (data: Buffer) => {
      const output = data.toString();
      const urlMatch = output.match(/https:\/\/[a-zA-Z0-9-]+\.trycloudflare\.com/);
      if (urlMatch && !tunnel.url) {
        tunnel.url = urlMatch[0];
        tunnel.status = 'active';
        console.log(`[Cloudflared] Tunnel ${tunnelId} active: ${tunnel.url}`);
      }
    });

    tunnelProcess.stderr.on('data', (data: Buffer) => {
      console.error(`[Cloudflared] Error: ${data.toString()}`);
    });

    tunnelProcess.on('exit', (code) => {
      console.log(`[Cloudflared] Process exited with code ${code}`);
      tunnel.status = 'closed';
      this.tunnels.delete(tunnelId);
    });

    return tunnel;
  }

  closeTunnel(tunnelId: string): boolean {
    const tunnel = this.tunnels.get(tunnelId);
    if (!tunnel) return false;

    try {
      tunnel.process.kill('SIGTERM');
    } catch (e: any) {
      console.error(`[Cloudflared] Error killing tunnel ${tunnelId}:`, e.message);
    }

    this.tunnels.delete(tunnelId);
    return true;
  }

  getTunnel(tunnelId: string): TunnelData | undefined {
    return this.tunnels.get(tunnelId);
  }

  getAllTunnels(): TunnelData[] {
    return Array.from(this.tunnels.values());
  }

  killAll(): void {
    for (const [id] of this.tunnels) {
      this.closeTunnel(id);
    }
  }
}

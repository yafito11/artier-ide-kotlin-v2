import { spawn, execSync } from 'child_process';
import * as os from 'os';
import * as fs from 'fs';
import * as path from 'path';
import { PkgPackage } from '../types';

/**
 * PkgManager - Package manager integration for proot/Termux-style packages
 * Handles installing runtime languages and tools on-demand
 */
export class PkgManager {
  private pkgPath: string;
  private packagesDir: string;

  constructor() {
    this.packagesDir = path.join(os.homedir(), '.artier', 'packages');
    // Try to find pkg binary
    this.pkgPath = this.findPkgBinary();
  }

  private findPkgBinary(): string {
    const possiblePaths = [
      'pkg',
      '/usr/bin/pkg',
      '/data/data/com.termux/files/usr/bin/pkg',
      path.join(os.homedir(), '.artier', 'bin', 'pkg'),
    ];

    for (const p of possiblePaths) {
      try {
        if (fs.existsSync(p)) return p;
      } catch (e) { /* ignore */ }
    }

    return 'pkg';
  }

  /**
   * Check if pkg is available
   */
  isAvailable(): boolean {
    try {
      execSync(`${this.pkgPath} --version`, { stdio: 'pipe', timeout: 5000 });
      return true;
    } catch (e) {
      return false;
    }
  }

  /**
   * Search for packages
   */
  async searchPackages(query: string): Promise<PkgPackage[]> {
    return new Promise((resolve, reject) => {
      const proc = spawn(this.pkgPath, ['search', query], {
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      let stdout = '';
      let stderr = '';

      proc.stdout.on('data', (data: Buffer) => { stdout += data.toString(); });
      proc.stderr.on('data', (data: Buffer) => { stderr += data.toString(); });

      proc.on('exit', (code) => {
        if (code !== 0) {
          // Fallback: return common packages
          resolve(this.getCommonPackages().filter(p =>
            p.name.toLowerCase().includes(query.toLowerCase()) ||
            p.description.toLowerCase().includes(query.toLowerCase())
          ));
          return;
        }

        const packages = this.parseSearchOutput(stdout);
        resolve(packages);
      });

      proc.on('error', () => {
        // Fallback to common packages
        resolve(this.getCommonPackages().filter(p =>
          p.name.toLowerCase().includes(query.toLowerCase()) ||
          p.description.toLowerCase().includes(query.toLowerCase())
        ));
      });

      setTimeout(() => { proc.kill(); resolve([]); }, 10000);
    });
  }

  /**
   * Install a package
   */
  async installPackage(packageName: string, onProgress?: (output: string) => void): Promise<boolean> {
    return new Promise((resolve) => {
      const proc = spawn(this.pkgPath, ['install', '-y', packageName], {
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      proc.stdout.on('data', (data: Buffer) => {
        const output = data.toString();
        console.log(`[Pkg] ${output}`);
        onProgress?.(output);
      });

      proc.stderr.on('data', (data: Buffer) => {
        const output = data.toString();
        console.log(`[Pkg] ${output}`);
        onProgress?.(output);
      });

      proc.on('exit', (code) => {
        resolve(code === 0);
      });

      proc.on('error', () => {
        resolve(false);
      });
    });
  }

  /**
   * Check if a package is installed
   */
  isPackageInstalled(packageName: string): boolean {
    try {
      execSync(`${this.pkgPath} list-installed ${packageName}`, { stdio: 'pipe', timeout: 5000 });
      return true;
    } catch (e) {
      return false;
    }
  }

  /**
   * Get installed packages
   */
  async getInstalledPackages(): Promise<PkgPackage[]> {
    return new Promise((resolve) => {
      const proc = spawn(this.pkgPath, ['list-installed'], {
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      let stdout = '';
      proc.stdout.on('data', (data: Buffer) => { stdout += data.toString(); });

      proc.on('exit', () => {
        const packages = this.parseSearchOutput(stdout).map(p => ({ ...p, installed: true }));
        resolve(packages);
      });

      proc.on('error', () => { resolve([]); });
      setTimeout(() => { proc.kill(); resolve([]); }, 5000);
    });
  }

  /**
   * Update package lists
   */
  async updateRepo(onProgress?: (output: string) => void): Promise<boolean> {
    return new Promise((resolve) => {
      const proc = spawn(this.pkgPath, ['update'], {
        stdio: ['pipe', 'pipe', 'pipe'],
      });

      proc.stdout.on('data', (data: Buffer) => {
        onProgress?.(data.toString());
      });

      proc.stderr.on('data', (data: Buffer) => {
        onProgress?.(data.toString());
      });

      proc.on('exit', (code) => { resolve(code === 0); });
      proc.on('error', () => { resolve(false); });
      setTimeout(() => { proc.kill(); resolve(false); }, 60000);
    });
  }

  /**
   * Get common packages available
   */
  getCommonPackages(): PkgPackage[] {
    return [
      { name: 'nodejs', version: '', description: 'Node.js runtime', installed: this.isPackageInstalled('nodejs') },
      { name: 'npm', version: '', description: 'Node.js package manager', installed: this.isPackageInstalled('npm') },
      { name: 'python', version: '', description: 'Python 3 runtime', installed: this.isPackageInstalled('python') },
      { name: 'python-pip', version: '', description: 'Python package manager', installed: this.isPackageInstalled('python-pip') },
      { name: 'golang', version: '', description: 'Go programming language', installed: this.isPackageInstalled('golang') },
      { name: 'rust', version: '', description: 'Rust programming language', installed: this.isPackageInstalled('rust') },
      { name: 'openjdk-17', version: '', description: 'OpenJDK 17', installed: this.isPackageInstalled('openjdk-17') },
      { name: 'ruby', version: '', description: 'Ruby runtime', installed: this.isPackageInstalled('ruby') },
      { name: 'php', version: '', description: 'PHP runtime', installed: this.isPackageInstalled('php') },
      { name: 'git', version: '', description: 'Git version control', installed: this.isPackageInstalled('git') },
      { name: 'curl', version: '', description: 'URL transfer tool', installed: this.isPackageInstalled('curl') },
      { name: 'wget', version: '', description: 'Network retriever', installed: this.isPackageInstalled('wget') },
      { name: 'openssh', version: '', description: 'SSH client/server', installed: this.isPackageInstalled('openssh') },
      { name: 'cloudflared', version: '', description: 'Cloudflare tunnel client', installed: this.isPackageInstalled('cloudflared') },
      { name: 'sqlite', version: '', description: 'SQLite database', installed: this.isPackageInstalled('sqlite') },
      { name: 'postgresql', version: '', description: 'PostgreSQL database', installed: this.isPackageInstalled('postgresql') },
      { name: 'cmake', version: '', description: 'Build system', installed: this.isPackageInstalled('cmake') },
      { name: 'make', version: '', description: 'Build automation', installed: this.isPackageInstalled('make') },
      { name: 'binutils', version: '', description: 'Binary utilities', installed: this.isPackageInstalled('binutils') },
      { name: 'clang', version: '', description: 'C/C++ compiler', installed: this.isPackageInstalled('clang') },
    ];
  }

  private parseSearchOutput(output: string): PkgPackage[] {
    const lines = output.split('\n').filter(l => l.trim());
    const packages: PkgPackage[] = [];

    for (const line of lines) {
      // Try to parse package name and version
      const match = line.match(/^(\S+)\s+(\S+)/);
      if (match) {
        packages.push({
          name: match[1],
          version: match[2] || '',
          description: line.substring(match[0].length).trim(),
          installed: false,
        });
      }
    }

    return packages;
  }
}

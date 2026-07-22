import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import * as https from 'https';
import * as http from 'http';
import { URL } from 'url';

export interface SkillFrontmatter {
  name: string;
  description: string;
  license?: string;
  compatibility?: string;
  metadata?: Record<string, string>;
  allowedTools?: string;
}

export interface SkillInfo {
  name: string;
  description: string;
  license?: string;
  compatibility?: string;
  metadata: Record<string, string>;
  allowedTools?: string;
  path: string;
  source: 'bundled' | 'user' | 'agents' | 'project';
  enabled: boolean;
  hasScripts: boolean;
  hasReferences: boolean;
  hasAssets: boolean;
  bodyPreview: string;
}

export interface SkillDetail extends SkillInfo {
  body: string;
  files: string[];
}

/**
 * Skill Manager — agentskills.io compatible SKILL.md loader
 */
export class SkillManager {
  private skills = new Map<string, SkillDetail>();
  private enabled = new Set<string>();
  private projectRoot: string | null = null;
  private enabledStorePath: string;

  constructor() {
    this.enabledStorePath = path.join(os.homedir(), '.artier', 'enabled-skills.json');
    this.loadEnabledState();
  }

  setProjectRoot(root: string | null): void {
    this.projectRoot = root;
  }

  private loadEnabledState(): void {
    try {
      if (fs.existsSync(this.enabledStorePath)) {
        const raw = JSON.parse(fs.readFileSync(this.enabledStorePath, 'utf-8'));
        if (Array.isArray(raw)) {
          raw.forEach((n: string) => this.enabled.add(n));
        }
      }
    } catch {
      // ignore
    }
  }

  private saveEnabledState(): void {
    try {
      const dir = path.dirname(this.enabledStorePath);
      if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(this.enabledStorePath, JSON.stringify([...this.enabled], null, 2));
    } catch (e: any) {
      console.error('[Skills] Failed to save enabled state:', e.message);
    }
  }

  getDiscoveryRoots(): { root: string; source: SkillInfo['source'] }[] {
    const roots: { root: string; source: SkillInfo['source'] }[] = [];

    // Bundled with daemon
    const bundled = path.join(__dirname, '..', '..', 'skills');
    roots.push({ root: bundled, source: 'bundled' });

    // User global Artier
    roots.push({ root: path.join(os.homedir(), '.artier', 'skills'), source: 'user' });

    // agentskills convention
    roots.push({ root: path.join(os.homedir(), '.agents', 'skills'), source: 'agents' });

    // Project-local
    if (this.projectRoot) {
      roots.push({ root: path.join(this.projectRoot, '.artier', 'skills'), source: 'project' });
      roots.push({ root: path.join(this.projectRoot, '.agents', 'skills'), source: 'project' });
    }

    return roots;
  }

  scan(): SkillInfo[] {
    this.skills.clear();

    for (const { root, source } of this.getDiscoveryRoots()) {
      if (!fs.existsSync(root)) continue;
      let entries: string[] = [];
      try {
        entries = fs.readdirSync(root);
      } catch {
        continue;
      }

      for (const entry of entries) {
        const skillDir = path.join(root, entry);
        let stat: fs.Stats;
        try {
          stat = fs.statSync(skillDir);
        } catch {
          continue;
        }
        if (!stat.isDirectory()) continue;

        const skillMd = path.join(skillDir, 'SKILL.md');
        if (!fs.existsSync(skillMd)) continue;

        try {
          const detail = this.parseSkillFile(skillMd, skillDir, source);
          // Prefer project > user > agents > bundled on name collision
          const existing = this.skills.get(detail.name);
          if (!existing || this.sourcePriority(source) >= this.sourcePriority(existing.source)) {
            this.skills.set(detail.name, detail);
          }
        } catch (e: any) {
          console.warn(`[Skills] Skip ${skillDir}: ${e.message}`);
        }
      }
    }

    console.log(`[Skills] Scanned ${this.skills.size} skill(s)`);
    return this.list();
  }

  private sourcePriority(source: SkillInfo['source']): number {
    switch (source) {
      case 'project': return 4;
      case 'user': return 3;
      case 'agents': return 2;
      case 'bundled': return 1;
      default: return 0;
    }
  }

  parseSkillFile(skillMdPath: string, skillDir: string, source: SkillInfo['source']): SkillDetail {
    const raw = fs.readFileSync(skillMdPath, 'utf-8');
    const { frontmatter, body } = this.parseFrontmatter(raw);

    if (!frontmatter.name || !frontmatter.description) {
      throw new Error('SKILL.md requires name and description in frontmatter');
    }

    this.validateName(frontmatter.name);

    const hasScripts = fs.existsSync(path.join(skillDir, 'scripts'));
    const hasReferences = fs.existsSync(path.join(skillDir, 'references'));
    const hasAssets = fs.existsSync(path.join(skillDir, 'assets'));

    const files: string[] = [];
    this.walkFiles(skillDir, skillDir, files);

    const enabled = this.enabled.has(frontmatter.name);
    const bodyPreview = body.trim().slice(0, 200).replace(/\s+/g, ' ');

    return {
      name: frontmatter.name,
      description: frontmatter.description,
      license: frontmatter.license,
      compatibility: frontmatter.compatibility,
      metadata: frontmatter.metadata || {},
      allowedTools: frontmatter.allowedTools,
      path: skillDir,
      source,
      enabled,
      hasScripts,
      hasReferences,
      hasAssets,
      bodyPreview,
      body,
      files,
    };
  }

  parseFrontmatter(raw: string): { frontmatter: SkillFrontmatter; body: string } {
    const trimmed = raw.replace(/^\uFEFF/, '');
    if (!trimmed.startsWith('---')) {
      throw new Error('SKILL.md must start with YAML frontmatter (---)');
    }

    const end = trimmed.indexOf('\n---', 3);
    if (end === -1) {
      throw new Error('Unclosed YAML frontmatter');
    }

    const yamlBlock = trimmed.slice(3, end).trim();
    const body = trimmed.slice(end + 4).replace(/^\r?\n/, '');

    const fm = this.parseSimpleYaml(yamlBlock);
    const name = String(fm.name || '').trim();
    const description = String(fm.description || '').trim();

    if (!name || !description) {
      throw new Error('Frontmatter must include name and description');
    }
    if (description.length > 1024) {
      throw new Error('description must be <= 1024 characters');
    }

    return {
      frontmatter: {
        name,
        description,
        license: fm.license ? String(fm.license) : undefined,
        compatibility: fm.compatibility ? String(fm.compatibility) : undefined,
        metadata: typeof fm.metadata === 'object' && fm.metadata ? fm.metadata as Record<string, string> : {},
        allowedTools: fm['allowed-tools'] ? String(fm['allowed-tools']) : undefined,
      },
      body,
    };
  }

  /**
   * Minimal YAML parser for flat + one-level nested maps (enough for SKILL.md frontmatter).
   */
  private parseSimpleYaml(yaml: string): Record<string, any> {
    const result: Record<string, any> = {};
    const lines = yaml.split(/\r?\n/);
    let currentKey: string | null = null;
    let nested: Record<string, string> | null = null;

    for (const line of lines) {
      if (!line.trim() || line.trim().startsWith('#')) continue;

      const nestedMatch = line.match(/^\s{2,}([A-Za-z0-9_.-]+):\s*(.*)$/);
      if (nested && nestedMatch) {
        nested[nestedMatch[1]] = this.unquote(nestedMatch[2].trim());
        continue;
      }

      const match = line.match(/^([A-Za-z0-9_.-]+):\s*(.*)$/);
      if (!match) continue;

      const key = match[1];
      const value = match[2].trim();

      if (value === '' || value === '|' || value === '>') {
        nested = {};
        result[key] = nested;
        currentKey = key;
      } else {
        nested = null;
        currentKey = null;
        result[key] = this.unquote(value);
      }
    }

    return result;
  }

  private unquote(v: string): string {
    if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
      return v.slice(1, -1);
    }
    return v;
  }

  validateName(name: string): void {
    if (name.length < 1 || name.length > 64) {
      throw new Error('name must be 1-64 characters');
    }
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name)) {
      throw new Error('name must be lowercase alphanumeric with single hyphens');
    }
  }

  private walkFiles(root: string, dir: string, out: string[]): void {
    let entries: string[] = [];
    try {
      entries = fs.readdirSync(dir);
    } catch {
      return;
    }
    for (const e of entries) {
      const full = path.join(dir, e);
      let st: fs.Stats;
      try {
        st = fs.statSync(full);
      } catch {
        continue;
      }
      if (st.isDirectory()) {
        this.walkFiles(root, full, out);
      } else {
        out.push(path.relative(root, full).replace(/\\/g, '/'));
      }
    }
  }

  list(): SkillInfo[] {
    return [...this.skills.values()]
      .map((s) => this.toInfo(s))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  get(name: string): SkillDetail | null {
    return this.skills.get(name) || null;
  }

  setEnabled(name: string, enabled: boolean): SkillInfo | null {
    const skill = this.skills.get(name);
    if (!skill) return null;
    if (enabled) this.enabled.add(name);
    else this.enabled.delete(name);
    skill.enabled = enabled;
    this.saveEnabledState();
    return this.toInfo(skill);
  }

  getEnabledSkills(): SkillDetail[] {
    return [...this.skills.values()].filter((s) => s.enabled);
  }

  /**
   * Progressive disclosure context for agents:
   * - Always include name+description for all skills
   * - Include full body only for enabled skills
   */
  buildAgentContext(includeBodies: boolean = true): string {
    const all = this.list();
    if (all.length === 0) return '';

    const lines: string[] = ['# Available Skills', ''];
    for (const s of all) {
      lines.push(`- **${s.name}**${s.enabled ? ' [enabled]' : ''}: ${s.description}`);
    }

    if (includeBodies) {
      const enabled = this.getEnabledSkills();
      if (enabled.length > 0) {
        lines.push('', '# Active Skill Instructions', '');
        for (const s of enabled) {
          lines.push(`## Skill: ${s.name}`, '');
          lines.push(s.body.trim(), '');
        }
      }
    }

    return lines.join('\n');
  }

  installFromPath(sourcePath: string): SkillDetail {
    const resolved = path.resolve(sourcePath);
    if (!fs.existsSync(resolved)) {
      throw new Error(`Path not found: ${resolved}`);
    }

    let skillDir = resolved;
    let skillMd = path.join(skillDir, 'SKILL.md');

    if (fs.statSync(resolved).isFile()) {
      if (path.basename(resolved) !== 'SKILL.md') {
        throw new Error('File must be named SKILL.md');
      }
      skillDir = path.dirname(resolved);
      skillMd = resolved;
    }

    if (!fs.existsSync(skillMd)) {
      throw new Error('SKILL.md not found in directory');
    }

    const parsed = this.parseSkillFile(skillMd, skillDir, 'user');
    const targetRoot = path.join(os.homedir(), '.artier', 'skills');
    const targetDir = path.join(targetRoot, parsed.name);

    if (!fs.existsSync(targetRoot)) fs.mkdirSync(targetRoot, { recursive: true });
    this.copyDir(skillDir, targetDir);

    const installed = this.parseSkillFile(path.join(targetDir, 'SKILL.md'), targetDir, 'user');
    this.skills.set(installed.name, installed);
    return installed;
  }

  async installFromUrl(url: string): Promise<SkillDetail> {
    const content = await this.fetchText(url);
    const { frontmatter } = this.parseFrontmatter(content);
    this.validateName(frontmatter.name);

    const targetRoot = path.join(os.homedir(), '.artier', 'skills');
    const targetDir = path.join(targetRoot, frontmatter.name);
    if (!fs.existsSync(targetDir)) fs.mkdirSync(targetDir, { recursive: true });

    fs.writeFileSync(path.join(targetDir, 'SKILL.md'), content, 'utf-8');

    // If URL is a directory listing style ending without SKILL.md, still ok for single file
    const installed = this.parseSkillFile(path.join(targetDir, 'SKILL.md'), targetDir, 'user');
    this.skills.set(installed.name, installed);
    return installed;
  }

  uninstall(name: string): boolean {
    const skill = this.skills.get(name);
    if (!skill) return false;
    if (skill.source === 'bundled') {
      throw new Error('Cannot uninstall bundled skills');
    }

    // Only delete if under user skills dir
    const userRoot = path.join(os.homedir(), '.artier', 'skills');
    if (skill.path.startsWith(userRoot)) {
      fs.rmSync(skill.path, { recursive: true, force: true });
    }

    this.skills.delete(name);
    this.enabled.delete(name);
    this.saveEnabledState();
    return true;
  }

  private copyDir(src: string, dest: string): void {
    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
    for (const entry of fs.readdirSync(src)) {
      const s = path.join(src, entry);
      const d = path.join(dest, entry);
      if (fs.statSync(s).isDirectory()) this.copyDir(s, d);
      else fs.copyFileSync(s, d);
    }
  }

  private fetchText(urlStr: string): Promise<string> {
    return new Promise((resolve, reject) => {
      let parsed: URL;
      try {
        parsed = new URL(urlStr);
      } catch {
        reject(new Error('Invalid URL'));
        return;
      }
      if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
        reject(new Error('Only http/https URLs allowed'));
        return;
      }

      const lib = parsed.protocol === 'https:' ? https : http;
      const req = lib.get(urlStr, { timeout: 15000 }, (res) => {
        if (res.statusCode && res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          this.fetchText(res.headers.location).then(resolve).catch(reject);
          return;
        }
        if (res.statusCode !== 200) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }
        const chunks: Buffer[] = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => resolve(Buffer.concat(chunks).toString('utf-8')));
      });
      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Request timeout'));
      });
    });
  }

  private toInfo(s: SkillDetail): SkillInfo {
    return {
      name: s.name,
      description: s.description,
      license: s.license,
      compatibility: s.compatibility,
      metadata: s.metadata,
      allowedTools: s.allowedTools,
      path: s.path,
      source: s.source,
      enabled: s.enabled,
      hasScripts: s.hasScripts,
      hasReferences: s.hasReferences,
      hasAssets: s.hasAssets,
      bodyPreview: s.bodyPreview,
    };
  }
}

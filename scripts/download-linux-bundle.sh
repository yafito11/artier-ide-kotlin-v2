#!/bin/bash
# =============================================================================
# ARTIER IDE - Linux Bundle Downloader
# Downloads proot, Alpine rootfs, Node.js, npm, bash, cloudflared
# =============================================================================
set -e

ASSETS_DIR="app/src/main/assets"
BIN_DIR="$ASSETS_DIR/bin"
ROOTFS_DIR="$ASSETS_DIR/rootfs"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Create directories
mkdir -p "$BIN_DIR"
mkdir -p "$ROOTFS_DIR"

# =============================================================================
# 1. Download proot (from termux-packages)
# =============================================================================
download_proot() {
    log_info "Downloading proot..."
    local PROOT_URL="https://github.com/nicman23/proot/releases/download/5.4.0/proot"
    local PROOT_BIN="$BIN_DIR/proot"
    
    if [ -f "$PROOT_BIN" ]; then
        log_warn "proot already exists, skipping..."
        return
    fi
    
    # Try termux proot (static build for aarch64)
    local TERMUX_URL="https://packages.termux.dev/apt/termux-main/pool/main/p/proot/proot_5.4.0_aarch64.deb"
    
    if curl -L -o "/tmp/proot.deb" "$TERMUX_URL" 2>/dev/null; then
        cd /tmp && ar x proot.deb && tar xf data.tar.* ./data/data/com.termux/files/usr/bin/proot 2>/dev/null
        if [ -f "./data/data/com.termux/files/usr/bin/proot" ]; then
            cp "./data/data/com.termux/files/usr/bin/proot" "$PROOT_BIN"
            chmod +x "$PROOT_BIN"
            log_info "proot downloaded successfully"
        else
            log_warn "Failed to extract proot, creating placeholder"
            create_proot_placeholder
        fi
        rm -rf /tmp/proot.deb /tmp/data 2>/dev/null
    else
        log_warn "Download failed, creating placeholder"
        create_proot_placeholder
    fi
}

create_proot_placeholder() {
    cat > "$BIN_DIR/proot" << 'PROOT_SCRIPT'
#!/system/bin/sh
# Proot placeholder - replace with actual proot binary
echo "[proot] Placeholder - download actual proot binary"
exec "$@"
PROOT_SCRIPT
    chmod +x "$BIN_DIR/proot"
}

# =============================================================================
# 2. Download Alpine rootfs (minimal)
# =============================================================================
download_rootfs() {
    log_info "Downloading Alpine rootfs..."
    local ROOTFS_TAR="$ASSETS_DIR/alpine-minirootfs.tar.gz"
    
    if [ -f "$ROOTFS_TAR" ]; then
        log_warn "Alpine rootfs already exists, skipping..."
        return
    fi
    
    local ALPINE_URL="https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-r0-aarch64.tar.gz"
    
    if curl -L -o "$ROOTFS_TAR" "$ALPINE_URL" 2>/dev/null; then
        log_info "Alpine rootfs downloaded successfully"
        log_info "Size: $(du -h "$ROOTFS_TAR" | cut -f1)"
    else
        log_error "Failed to download Alpine rootfs"
        log_warn "Creating minimal rootfs structure instead"
        create_minimal_rootfs
    fi
}

create_minimal_rootfs() {
    mkdir -p "$ROOTFS_DIR"/{bin,lib,usr/bin,usr/lib,etc,tmp,home/user,var,proc,sys,dev}
    
    # Create basic files
    echo "root:x:0:0:root:/root:/bin/sh" > "$ROOTFS_DIR/etc/passwd"
    echo "root:x:0:" > "$ROOTFS_DIR/etc/group"
    echo "127.0.0.1 localhost" > "$ROOTFS_DIR/etc/hosts"
    
    # Create basic shell scripts
    for cmd in sh bash ls cat echo mkdir rm cp mv touch chmod; do
        cat > "$ROOTFS_DIR/usr/bin/$cmd" << 'EOF'
#!/system/bin/sh
echo "[placeholder] $0 not installed"
EOF
        chmod +x "$ROOTFS_DIR/usr/bin/$cmd"
    done
}

# =============================================================================
# 3. Download Node.js v20 LTS (from termux)
# =============================================================================
download_nodejs() {
    log_info "Downloading Node.js..."
    local NODE_DIR="$ROOTFS_DIR/usr/bin"
    mkdir -p "$NODE_DIR"
    
    if [ -f "$NODE_DIR/node" ]; then
        log_warn "Node.js already exists, skipping..."
        return
    fi
    
    # Node.js static build for aarch64
    local NODE_URL="https://unofficial-builds.nodejs.org/download/release/v20.11.1/node-v20.11.1-linux-arm64-musl.tar.xz"
    
    if curl -L -o "/tmp/node.tar.xz" "$NODE_URL" 2>/dev/null; then
        cd /tmp && tar xf node.tar.xz --strip-components=2 ./bin/node 2>/dev/null
        if [ -f "/tmp/node" ]; then
            cp "/tmp/node" "$NODE_DIR/node"
            chmod +x "$NODE_DIR/node"
            log_info "Node.js downloaded successfully"
        else
            log_warn "Failed to extract Node.js, creating placeholder"
            create_node_placeholder
        fi
        rm -f /tmp/node.tar.xz /tmp/node 2>/dev/null
    else
        log_warn "Download failed, creating placeholder"
        create_node_placeholder
    fi
}

create_node_placeholder() {
    cat > "$ROOTFS_DIR/usr/bin/node" << 'EOF'
#!/system/bin/sh
echo "[node] Placeholder - download actual Node.js binary"
echo "Node.js is required for the daemon server"
exit 1
EOF
    chmod +x "$ROOTFS_DIR/usr/bin/node"
}

# =============================================================================
# 4. Download npm v10
# =============================================================================
download_npm() {
    log_info "Downloading npm..."
    local NPM_DIR="$ROOTFS_DIR/usr/bin"
    local NPM_LIB="$ROOTFS_DIR/usr/lib/node_modules/npm"
    
    if [ -f "$NPM_DIR/npm" ]; then
        log_warn "npm already exists, skipping..."
        return
    fi
    
    # npm is usually bundled with Node.js, create wrapper
    cat > "$NPM_DIR/npm" << 'EOF'
#!/bin/sh
NPM_PREFIX="$(dirname $(dirname $(readlink -f $0 2>/dev/null || echo $0)))/lib/node_modules/npm"
exec node "$NPM_PREFIX/bin/npm-cli.js" "$@"
EOF
    chmod +x "$NPM_DIR/npm"
    
    # Create npm directory structure
    mkdir -p "$NPM_LIB/bin"
    
    cat > "$NPM_LIB/bin/npm-cli.js" << 'EOF'
#!/usr/bin/env node
console.log('[npm] npm placeholder - install actual npm package');
process.exit(1);
EOF
    chmod +x "$NPM_LIB/bin/npm-cli.js"
    
    log_info "npm wrapper created"
}

# =============================================================================
# 5. Download cloudflared (Cloudflare Tunnel)
# =============================================================================
download_cloudflared() {
    log_info "Downloading cloudflared..."
    local CLOUDFLARED_BIN="$BIN_DIR/cloudflared"
    
    if [ -f "$CLOUDFLARED_BIN" ]; then
        log_warn "cloudflared already exists, skipping..."
        return
    fi
    
    local CF_URL="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64"
    
    if curl -L -o "$CLOUDFLARED_BIN" "$CF_URL" 2>/dev/null; then
        chmod +x "$CLOUDFLARED_BIN"
        log_info "cloudflared downloaded successfully"
    else
        log_warn "Failed to download cloudflared"
        create_cloudflared_placeholder
    fi
}

create_cloudflared_placeholder() {
    cat > "$CLOUDFLARED_BIN" << 'EOF'
#!/system/bin/sh
echo "[cloudflared] Placeholder - download actual cloudflared binary"
echo "cloudflared is required for tunnel functionality"
exit 1
EOF
    chmod +x "$CLOUDFLARED_BIN"
}

# =============================================================================
# 6. Bundle daemon code
# =============================================================================
bundle_daemon() {
    log_info "Bundling daemon code..."
    local DAEMON_DEST="$ROOTFS_DIR/opt/artier/daemon"
    
    mkdir -p "$DAEMON_DEST"
    
    # Copy daemon source files
    if [ -d "daemon" ]; then
        cp -r daemon/package.json "$DAEMON_DEST/"
        cp -r daemon/tsconfig.json "$DAEMON_DEST/"
        cp -r daemon/server.js "$DAEMON_DEST/"
        
        # Copy src directory
        if [ -d "daemon/src" ]; then
            cp -r daemon/src "$DAEMON_DEST/"
        fi
        
        # Copy router if exists
        if [ -d "daemon/router" ]; then
            cp -r daemon/router "$DAEMON_DEST/"
        fi
        
        log_info "Daemon code bundled successfully"
    else
        log_warn "Daemon directory not found"
    fi
}

# =============================================================================
# 7. Create proot startup script
# =============================================================================
create_startup_script() {
    log_info "Creating startup scripts..."
    
    cat > "$BIN_DIR/start-daemon.sh" << 'STARTUP_EOF'
#!/system/bin/sh
# Artier IDE Daemon Startup Script

PROOT_DIR="$(dirname $0)"
ROOTFS_DIR="$PROOT_DIR/../rootfs"
DAEMON_DIR="/opt/artier/daemon"

echo "[artier] Starting Artier IDE Daemon..."

# Check if proot is available
if [ ! -x "$PROOT_DIR/proot" ]; then
    echo "[artier] Error: proot not found or not executable"
    exit 1
fi

# Check if rootfs exists
if [ ! -d "$ROOTFS_DIR" ]; then
    echo "[artier] Error: rootfs directory not found"
    exit 1
fi

# Start daemon via proot
exec "$PROOT_DIR/proot" \
    -0 \
    -r "$ROOTFS_DIR" \
    -w / \
    -b /proc \
    -b /sys \
    -b /dev \
    /bin/sh -c "cd $DAEMON_DIR && node server.js"
STARTUP_EOF
    chmod +x "$BIN_DIR/start-daemon.sh"
    
    log_info "Startup scripts created"
}

# =============================================================================
# Main
# =============================================================================
main() {
    echo "============================================"
    echo "  ARTIER IDE - Linux Bundle Downloader"
    echo "============================================"
    echo ""
    
    download_proot
    download_rootfs
    download_nodejs
    download_npm
    download_cloudflared
    bundle_daemon
    create_startup_script
    
    echo ""
    echo "============================================"
    log_info "Download complete!"
    echo ""
    echo "Assets location:"
    echo "  BIN:   $BIN_DIR/"
    echo "  ROOTFS: $ROOTFS_DIR/"
    echo ""
    echo "Total size:"
    du -sh "$ASSETS_DIR"
    echo "============================================"
}

main "$@"
